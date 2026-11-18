package com.docmanager.ocr.messaging;

import com.docmanager.ocr.exception.OcrException;
import com.docmanager.ocr.service.DocumentStore;
import com.docmanager.ocr.service.OcrService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Slf4j
@Component
public class OcrListener {

    private final DocumentStore documentStore;
    private final OcrService ocrService;
    private final RabbitTemplate rabbitTemplate;
    private final String exchange;
    private final String ocrDoneRoutingKey;

    public OcrListener(DocumentStore documentStore,
                       OcrService ocrService,
                       RabbitTemplate rabbitTemplate,
                       @Value("${app.rabbitmq.exchange}") String exchange,
                       @Value("${app.rabbitmq.routing-key.ocr-done:document.ocr.done}") String ocrDoneRoutingKey) {
        this.documentStore = documentStore;
        this.ocrService = ocrService;
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange;
        this.ocrDoneRoutingKey = ocrDoneRoutingKey;
    }

    @RabbitListener(queues = "${app.rabbitmq.queue.document-processing}")
    public void handleOcrMessage(OcrRequestMessage message) {
        if (message == null || message.documentId() == null) {
            log.warn("Ungültige OCR-Nachricht empfangen: null");
            return;
        }

        // Fall 1: Fehlt objectKey (Dokument über den alten Endpoint ohne Datei angelegt)
        if (message.objectKey() == null || message.objectKey().isBlank()) {
            log.warn("Kein objectKey für Dokument-ID: {} vorhanden", message.documentId());
            sendResult(new OcrResultMessage(
                    message.documentId(),
                    message.filename(),
                    null,
                    false,
                    "Kein objectKey vorhanden (Dokument ohne Datei angelegt)"
            ));
            return;
        }

        Path tempFile = null;
        try {
            tempFile = Files.createTempFile("ocr-", ".pdf");

            // Datei aus MinIO über DocumentStore laden
            try (InputStream in = documentStore.loadDocument(message.objectKey())) {
                if (in == null) {
                    throw new IOException("Datei nicht gefunden in DocumentStore: " + message.objectKey());
                }
                Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING);
            }

            long fileSize = Files.size(tempFile);
            log.info("OCR-Nachricht empfangen für Dokument-ID: {}, Dateigröße: {} Bytes", message.documentId(), fileSize);

            // OCR durchführen
            String text = ocrService.extractText(tempFile.toFile());

            // Erfolgreiches Ergebnis senden
            sendResult(new OcrResultMessage(
                    message.documentId(),
                    message.filename(),
                    text,
                    true,
                    null
            ));
            log.info("OCR-Ergebnis gesendet für Dokument-ID: {}, Textlänge: {}", message.documentId(), text != null ? text.length() : 0);

        } catch (OcrException e) {
            log.error("OCR fehlgeschlagen für Dokument-ID: {}", message.documentId(), e);
            sendResult(new OcrResultMessage(
                    message.documentId(),
                    message.filename(),
                    null,
                    false,
                    "OCR fehlgeschlagen: " + e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Fehler bei der Dokumentverarbeitung für Dokument-ID: {}", message.documentId(), e);
            sendResult(new OcrResultMessage(
                    message.documentId(),
                    message.filename(),
                    null,
                    false,
                    "Datei nicht in MinIO gefunden oder Verarbeitungsfehler: " + e.getMessage()
            ));
        } finally {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                    log.debug("Temporäre Datei gelöscht: {}", tempFile);
                } catch (IOException e) {
                    log.warn("Konnte temporäre Datei nicht löschen: {}", tempFile, e);
                }
            }
        }
    }

    private void sendResult(OcrResultMessage result) {
        try {
            rabbitTemplate.convertAndSend(exchange, ocrDoneRoutingKey, result);
            if (!result.success()) {
                log.info("OCR-Ergebnis gesendet für Dokument-ID: {}, success=false, Grund: {}", result.documentId(), result.errorMessage());
            }
        } catch (Exception e) {
            log.error("Fehler beim Senden des OCR-Ergebnisses für Dokument-ID: {}", result.documentId(), e);
        }
    }
}

