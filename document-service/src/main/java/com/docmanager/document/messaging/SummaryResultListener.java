package com.docmanager.document.messaging;

import com.docmanager.document.service.DocumentService;
import com.docmanager.messaging.MessagingTopology;
import com.docmanager.messaging.SummaryResultMessage;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/** Nimmt Summary-Ergebnisse an. Fehler werden nicht abgefangen: Sie lösen die Wiederholung aus. */
@Component
public class SummaryResultListener {

    private final DocumentService documentService;

    public SummaryResultListener(DocumentService documentService) {
        this.documentService = documentService;
    }

    @RabbitListener(queues = MessagingTopology.SUMMARY_RESULT_QUEUE)
    public void handleSummaryResult(SummaryResultMessage message) {
        documentService.applySummaryResult(message);
    }
}
