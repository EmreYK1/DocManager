package com.docmanager.genai.messaging;

import com.docmanager.genai.exception.GenAiException;
import com.docmanager.genai.service.SummaryService;
import com.docmanager.messaging.MessagingTopology;
import com.docmanager.messaging.SummaryRequestMessage;
import com.docmanager.messaging.SummaryResultMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Ein Fehler der Gemini-API ist ein Ergebnis und wird als solches gemeldet. Alle anderen Fehler, etwa ein
 * nicht erreichbares RabbitMQ, werden weitergeworfen und wiederholt.
 */
@Component
public class SummaryListener {

    private static final Logger log = LoggerFactory.getLogger(SummaryListener.class);

    private final SummaryService summaryService;
    private final SummaryResultPublisher publisher;

    public SummaryListener(SummaryService summaryService, SummaryResultPublisher publisher) {
        this.summaryService = summaryService;
        this.publisher = publisher;
    }

    @RabbitListener(queues = MessagingTopology.SUMMARY_REQUEST_QUEUE)
    public void handleSummaryRequest(SummaryRequestMessage message) {
        publisher.publish(summarize(message));
    }

    private SummaryResultMessage summarize(SummaryRequestMessage message) {
        if (message.text() == null || message.text().isBlank()) {
            log.warn("Kein Text für die Zusammenfassung: id={}", message.documentId());
            return SummaryResultMessage.failed(message.documentId(), "Kein Text zum Zusammenfassen vorhanden");
        }
        try {
            String summary = summaryService.summarize(message.text());
            log.info("Zusammenfassung erstellt: id={}", message.documentId());
            return SummaryResultMessage.succeeded(message.documentId(), summary);
        } catch (GenAiException e) {
            log.error("Zusammenfassung fehlgeschlagen für Dokument-ID: {}", message.documentId(), e);
            return SummaryResultMessage.failed(
                    message.documentId(), "Zusammenfassung fehlgeschlagen: " + e.getMessage());
        }
    }
}
