package com.docmanager.document.messaging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.AmqpException;
import com.docmanager.document.exception.MessagingException;
import org.springframework.stereotype.Component;

/**
 * Stub-Worker: hört die OCR-Queue ab und loggt nur die Dokument-ID.
 */
@Slf4j
@Component
public class OcrWorker {

    @RabbitListener(queues = "${app.rabbitmq.queue.document-processing}")
    public void handleOcrMessage(String documentId) {
        try {
            log.info("OCR-Nachricht empfangen für Dokument-ID: {}", documentId);
        } catch (AmqpException e) {
            log.error("Fehler beim Verarbeiten der OCR-Nachricht für Dokument-ID: {}", documentId, e);
            throw new MessagingException("OCR-Nachricht konnte nicht verarbeitet werden", e);
        }
    }
}
