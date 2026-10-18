package com.docmanager.document.service.impl;

import com.docmanager.document.entity.Document;
import com.docmanager.document.exception.NotFoundException;
import com.docmanager.document.repository.DocumentRepository;
import com.docmanager.document.service.DocumentService;
import com.docmanager.document.service.DocumentUpdate;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.AmqpException;
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
    private final String exchange;
    private final String documentRoutingKey;

    public DocumentServiceImpl(DocumentRepository documentRepository,
                               FolderService folderService,
                               RabbitTemplate rabbitTemplate,
                               @Value("${app.rabbitmq.exchange}") String exchange,
                               @Value("${app.rabbitmq.routing-key.document}") String documentRoutingKey) {
        this.documentRepository = documentRepository;
        this.folderService = folderService;
        this.rabbitTemplate = rabbitTemplate;
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
        try {
            rabbitTemplate.convertAndSend(exchange, documentRoutingKey, doc.getId().toString());
            log.info("OCR-Nachricht gesendet für Dokument-ID: {}", doc.getId());
        } catch (AmqpException e) {
            log.error("Fehler beim Senden der OCR-Nachricht für Dokument-ID: {}", doc.getId(), e);
            throw new MessagingException("Nachricht konnte nicht gesendet werden", e);
        }
        return doc;
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
        documentRepository.deleteById(id);
        log.info("Dokument gelöscht: id={}", id);
    }
}
