package com.docmanager.document.service.impl;

import com.docmanager.document.entity.Document;
import com.docmanager.document.exception.InvalidFileException;
import com.docmanager.document.exception.NotFoundException;
import com.docmanager.document.messaging.OcrRequestMessage;
import com.docmanager.document.repository.DocumentRepository;
import com.docmanager.document.service.DocumentService;
import com.docmanager.document.service.DocumentUpdate;
import com.docmanager.document.service.StorageService;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.AmqpException;
import org.springframework.web.multipart.MultipartFile;
import com.docmanager.document.exception.MessagingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.docmanager.document.service.FolderService;
import com.docmanager.document.entity.Folder;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class DocumentServiceImpl implements DocumentService {

    private final DocumentRepository documentRepository;
    private final FolderService folderService;
    private final RabbitTemplate rabbitTemplate;
    private final StorageService storageService;
    private final String exchange;
    private final String documentRoutingKey;

    public DocumentServiceImpl(DocumentRepository documentRepository,
                               FolderService folderService,
                               RabbitTemplate rabbitTemplate,
                               StorageService storageService,
                               @Value("${app.rabbitmq.exchange}") String exchange,
                               @Value("${app.rabbitmq.routing-key.document}") String documentRoutingKey) {
        this.documentRepository = documentRepository;
        this.folderService = folderService;
        this.rabbitTemplate = rabbitTemplate;
        this.storageService = storageService;
        this.exchange = exchange;
        this.documentRoutingKey = documentRoutingKey;
    }

    @Override
    public Document create(String filename, String contentType, long sizeBytes, UUID folderId) {
        Folder folder = folderId != null
                ? folderService.findById(folderId)
                : null;
        Document doc = documentRepository.save(new Document(filename, contentType, sizeBytes, folder));
        log.info("Dokument erstellt: id={}, filename={}", doc.getId(), doc.getFilename());
        sendOcrMessage(doc);
        return doc;
    }

    @Override
    public Document upload(MultipartFile file, UUID folderId) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("Die Datei darf nicht leer sein");
        }
        if (file.getContentType() == null || !"application/pdf".equalsIgnoreCase(file.getContentType())) {
            throw new InvalidFileException("Nur PDF-Dateien sind erlaubt");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || filename.isBlank()) {
            filename = file.getName();
        }

        // 1. Zuerst in MinIO speichern
        String objectKey = storageService.store(file);

        Document doc;
        try {
            // 2. Dann in der DB speichern
            Folder folder = folderId != null ? folderService.findById(folderId) : null;
            doc = documentRepository.save(new Document(filename, file.getContentType(), file.getSize(), folder, objectKey));
            log.info("Dokument erstellt: id={}, filename={}, objectKey={}", doc.getId(), doc.getFilename(), objectKey);
        } catch (Exception e) {
            // Schlägt die DB fehl, die Datei wieder löschen
            log.warn("DB-Speichern fehlgeschlagen, lösche Datei '{}' aus Storage", objectKey);
            try {
                storageService.delete(objectKey);
            } catch (Exception cleanupEx) {
                log.error("Fehler beim Aufräumen der Datei '{}' aus Storage", objectKey, cleanupEx);
            }
            throw e;
        }

        // 3. Dann die Nachricht senden
        sendOcrMessage(doc);

        return doc;
    }

    private void sendOcrMessage(Document doc) {
        try {
            OcrRequestMessage message = new OcrRequestMessage(doc.getId(), doc.getObjectKey(), doc.getFilename());
            rabbitTemplate.convertAndSend(exchange, documentRoutingKey, message);
            log.info("OCR-Nachricht gesendet für Dokument-ID: {}", doc.getId());
        } catch (AmqpException e) {
            log.error("Fehler beim Senden der OCR-Nachricht für Dokument-ID: {}", doc.getId(), e);
            throw new MessagingException("Nachricht konnte nicht gesendet werden", e);
        }
    }

    @Override
    public Document findById(UUID id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Document not found: " + id));
    }

    @Override
    public List<Document> findAll() {
        return documentRepository.findAll();
    }

    @Override
    public Document update(UUID id, DocumentUpdate update) {
        Document existing = findById(id);
        if (update.filename() != null) existing.setFilename(update.filename());
        if (update.status() != null) existing.setStatus(update.status());
        if (update.clearFolder()) {
            existing.setFolder(null);
        } else if (update.folderId() != null) {
            existing.setFolder(folderService.findById(update.folderId()));
        }
        existing = documentRepository.save(existing);
        log.info("Dokument aktualisiert: id={}, neuer Status={}", id, existing.getStatus());
        return existing;
    }

    @Override
    public void delete(UUID id) {
        Document existing = findById(id);
        if (existing.getObjectKey() != null) {
            storageService.delete(existing.getObjectKey());
        }
        documentRepository.deleteById(id);
        log.info("Dokument gelöscht: id={}", id);
    }
}
