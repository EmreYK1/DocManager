package com.docmanager.document.messaging;

import com.docmanager.document.service.DocumentService;
import com.docmanager.messaging.MessagingTopology;
import com.docmanager.messaging.OcrResultMessage;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/** Nimmt OCR-Ergebnisse an. Fehler werden nicht abgefangen: Sie lösen die Wiederholung aus. */
@Component
public class OcrResultListener {

    private final DocumentService documentService;

    public OcrResultListener(DocumentService documentService) {
        this.documentService = documentService;
    }

    @RabbitListener(queues = MessagingTopology.OCR_RESULT_QUEUE)
    public void handleOcrResult(OcrResultMessage message) {
        documentService.applyOcrResult(message);
    }
}
