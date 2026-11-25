package com.docmanager.ocr.messaging;

import com.docmanager.messaging.MessagingTopology;
import com.docmanager.messaging.OcrRequestMessage;
import com.docmanager.messaging.OcrResultMessage;
import com.docmanager.ocr.exception.OcrException;
import com.docmanager.ocr.service.DocumentStore;
import com.docmanager.ocr.service.OcrService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Ein Fehler der Texterkennung (z. B. kaputtes PDF) ist ein Ergebnis und wird als solches gemeldet.
 * Alle anderen Fehler, etwa ein nicht erreichbarer Speicher, werden weitergeworfen und wiederholt.
 */
@Slf4j
@Component
public class OcrListener {

    private final DocumentStore documentStore;
    private final OcrService ocrService;
    private final OcrResultPublisher publisher;

    public OcrListener(DocumentStore documentStore, OcrService ocrService, OcrResultPublisher publisher) {
        this.documentStore = documentStore;
        this.ocrService = ocrService;
        this.publisher = publisher;
    }

    @RabbitListener(queues = MessagingTopology.OCR_REQUEST_QUEUE)
    public void handleOcrMessage(OcrRequestMessage message) throws IOException {
        Path tempFile = Files.createTempFile("ocr-", ".pdf");
        try {
            try (InputStream in = documentStore.loadDocument(message.objectKey())) {
                Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING);
            }
            log.info("Texterkennung für Dokument {}: {} Bytes", message.documentId(), Files.size(tempFile));
            publisher.publish(recognize(message, tempFile));
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    private OcrResultMessage recognize(OcrRequestMessage message, Path pdf) {
        try {
            return OcrResultMessage.succeeded(message.documentId(), ocrService.extractText(pdf));
        } catch (OcrException e) {
            log.error("OCR fehlgeschlagen für Dokument-ID: {}", message.documentId(), e);
            return OcrResultMessage.failed(message.documentId(), "OCR fehlgeschlagen: " + e.getMessage());
        }
    }
}
