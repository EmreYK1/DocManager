package com.docmanager.document.service;

import com.docmanager.document.entity.Document;
import com.docmanager.document.entity.Folder;
import com.docmanager.document.exception.InvalidFileException;
import com.docmanager.document.exception.NotFoundException;
import com.docmanager.document.exception.StorageException;
import com.docmanager.document.messaging.MessagePublisher;
import com.docmanager.document.repository.DocumentRepository;
import com.docmanager.messaging.IndexMessage;
import com.docmanager.messaging.MessagingTopology;
import com.docmanager.messaging.OcrRequestMessage;
import com.docmanager.messaging.OcrResultMessage;
import com.docmanager.messaging.SummaryRequestMessage;
import com.docmanager.messaging.SummaryResultMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class DocumentService {

    private static final byte[] PDF_SIGNATURE = "%PDF-".getBytes(StandardCharsets.US_ASCII);

    private final DocumentRepository documentRepository;
    private final FolderService folderService;
    private final StorageService storageService;
    private final SearchIndexService searchIndexService;
    private final MessagePublisher messages;
    private final TransactionTemplate transaction;
    private final ApplicationEventPublisher events;

    public DocumentService(
            DocumentRepository documentRepository,
            FolderService folderService,
            StorageService storageService,
            SearchIndexService searchIndexService,
            MessagePublisher messages,
            TransactionTemplate transaction,
            ApplicationEventPublisher events) {
        this.documentRepository = documentRepository;
        this.folderService = folderService;
        this.storageService = storageService;
        this.searchIndexService = searchIndexService;
        this.messages = messages;
        this.transaction = transaction;
        this.events = events;
    }

    /** Die Datei wird außerhalb der Transaktion gespeichert, damit keine DB-Verbindung darauf wartet. */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Document upload(MultipartFile file, UUID folderId) {
        requirePdf(file);
        String originalName = file.getOriginalFilename();
        String filename = originalName == null || originalName.isBlank() ? file.getName() : originalName;

        String objectKey = storageService.store(file);
        try {
            // Der OCR-Auftrag wird erst nach dem Commit gesendet
            return transaction.execute(status -> {
                Folder folder = folderId != null ? folderService.findById(folderId) : null;
                Document document = documentRepository.save(
                        new Document(filename, file.getContentType(), file.getSize(), folder, objectKey));
                messages.send(MessagingTopology.OCR_REQUEST_KEY, new OcrRequestMessage(document.getId(), objectKey));
                log.info("Dokument erstellt: id={}, filename={}", document.getId(), filename);
                return document;
            });
        } catch (RuntimeException e) {
            log.warn("Speichern fehlgeschlagen, lösche Datei {} aus dem Storage", objectKey);
            discardFile(new FileDiscarded(objectKey));
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public Document findById(UUID id) {
        return documentRepository
                .findById(id)
                .orElseThrow(() -> new NotFoundException("Dokument nicht gefunden: " + id));
    }

    @Transactional(readOnly = true)
    public List<Document> findAll() {
        return documentRepository.findAll();
    }

    public Document update(UUID id, DocumentUpdate update) {
        Document document = findById(id);
        boolean renamed = update.filename() != null && !update.filename().equals(document.getFilename());
        if (renamed) {
            document.rename(update.filename());
        }
        if (update.clearFolder()) {
            document.moveTo(null);
        } else if (update.folderId() != null) {
            document.moveTo(folderService.findById(update.folderId()));
        }
        if (renamed && document.isSearchable()) {
            requestIndexing(document);
        }
        return document;
    }

    /** Idempotent: Doppelte oder verspätete Ergebnisse ändern nichts. */
    public void applyOcrResult(OcrResultMessage result) {
        Document document = documentRepository.findById(result.documentId()).orElse(null);
        if (document == null) {
            log.warn("OCR-Ergebnis für unbekanntes oder gelöschtes Dokument verworfen: {}", result.documentId());
            return;
        }
        if (result.success()) {
            if (document.completeOcr(result.text())) {
                requestIndexing(document);
                requestSummary(document);
                log.info("Texterkennung abgeschlossen: id={}", document.getId());
            }
        } else if (document.failOcr()) {
            log.warn("Texterkennung fehlgeschlagen: id={}, grund={}", document.getId(), result.errorMessage());
        }
    }

    /** Idempotent wie {@link #applyOcrResult}. */
    public void applySummaryResult(SummaryResultMessage result) {
        Document document = documentRepository.findById(result.documentId()).orElse(null);
        if (document == null) {
            log.warn("Summary-Ergebnis für unbekanntes oder gelöschtes Dokument verworfen: {}", result.documentId());
            return;
        }
        if (result.success()) {
            if (document.completeSummary(result.summary())) {
                log.info("Zusammenfassung gespeichert: id={}", document.getId());
            }
        } else if (document.failSummary()) {
            log.warn("Zusammenfassung fehlgeschlagen: id={}, grund={}", document.getId(), result.errorMessage());
        }
    }

    public void delete(UUID id) {
        Document document = findById(id);
        documentRepository.delete(document);
        messages.send(MessagingTopology.INDEX_KEY, IndexMessage.delete(id));
        if (document.getObjectKey() != null) {
            events.publishEvent(new FileDiscarded(document.getObjectKey()));
        }
        log.info("Dokument gelöscht: id={}", id);
    }

    /** Läuft nach dem Commit: Eine Datei verschwindet nie, solange ihr Dokument noch existiert. */
    @TransactionalEventListener
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void discardFile(FileDiscarded event) {
        try {
            storageService.delete(event.objectKey());
        } catch (StorageException e) {
            log.error("Verwaiste Datei im Storage: {}", event.objectKey(), e);
        }
    }

    /** Sucht Dokumente per Elasticsearch, nach Relevanz sortiert. */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public List<Document> search(String query) {
        List<UUID> ids = searchIndexService.search(query);
        if (ids.isEmpty()) {
            return List.of();
        }
        Map<UUID, Document> byId = documentRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Document::getId, Function.identity()));
        // Reihenfolge der Relevanz beibehalten, IDs ohne Dokument in der DB überspringen
        return ids.stream().map(byId::get).filter(Objects::nonNull).toList();
    }

    private void requestIndexing(Document document) {
        messages.send(
                MessagingTopology.INDEX_KEY,
                IndexMessage.upsert(document.getId(), document.getFilename(), document.getOcrText()));
    }

    private void requestSummary(Document document) {
        messages.send(
                MessagingTopology.SUMMARY_REQUEST_KEY,
                new SummaryRequestMessage(document.getId(), document.getOcrText()));
    }

    private void requirePdf(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("Die Datei darf nicht leer sein");
        }
        // Den Content-Type bestimmt der Client, deshalb zählt zusätzlich der Dateianfang
        if (!"application/pdf".equalsIgnoreCase(file.getContentType()) || !hasPdfSignature(file)) {
            throw new InvalidFileException("Nur PDF-Dateien sind erlaubt");
        }
    }

    private boolean hasPdfSignature(MultipartFile file) {
        try (InputStream in = file.getInputStream()) {
            return Arrays.equals(in.readNBytes(PDF_SIGNATURE.length), PDF_SIGNATURE);
        } catch (IOException e) {
            throw new StorageException("Datei konnte nicht gelesen werden", e);
        }
    }

    public record FileDiscarded(String objectKey) {}
}
