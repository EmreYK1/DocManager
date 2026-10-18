package com.docmanager.document.messaging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Stub-Worker: hört die OCR-Queue ab und loggt nur die Dokument-ID.
 */
@Slf4j
@Component
public class OcrWorker {

    @RabbitListener(queues = "${app.rabbitmq.queue.document-processing}")
    public void handleOcrMessage(String documentId) {
        log.info("OCR-Nachricht empfangen für Dokument-ID: {}", documentId);
    }
}
