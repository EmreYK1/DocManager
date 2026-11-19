package com.docmanager.document.messaging;

import com.docmanager.document.entity.Document;
import com.docmanager.document.entity.DocumentStatus;
import com.docmanager.document.repository.DocumentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
public class OcrResultListener {

    private final DocumentRepository documentRepository;

    public OcrResultListener(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    @RabbitListener(queues = "${app.rabbitmq.queue.ocr-result:document.ocr.result}")
    public void handleOcrResult(OcrResultMessage message) {
        if (message == null || message.documentId() == null) {
            log.warn("Ungültige OCR-Ergebnis-Nachricht empfangen: null");
            return;
        }

        Optional<Document> docOpt = documentRepository.findById(message.documentId());
        if (docOpt.isEmpty()) {
            log.warn("Dokument mit ID {} nicht gefunden (wurde eventuell gelöscht). OCR-Ergebnis wird verworfen.", message.documentId());
            return;
        }

        Document document = docOpt.get();
        if (message.success()) {
            document.setOcrText(message.text());
            document.setStatus(DocumentStatus.OCR_DONE);
            log.info("OCR-Ergebnis erfolgreich verarbeitet für Dokument-ID: {}, neuer Status: OCR_DONE", document.getId());
        } else {
            document.setStatus(DocumentStatus.FAILED);
            log.warn("OCR fehlgeschlagen für Dokument-ID: {}, Grund: {}, neuer Status: FAILED", document.getId(), message.errorMessage());
        }

        documentRepository.save(document);
    }
}

