package com.docmanager.indexing.messaging;

import com.docmanager.indexing.exception.IndexingException;
import com.docmanager.indexing.service.SearchIndex;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class IndexingListener {

    private final SearchIndex searchIndex;

    public IndexingListener(SearchIndex searchIndex) {
        this.searchIndex = searchIndex;
    }

    @RabbitListener(queues = "${app.rabbitmq.queue.indexing}")
    public void handleOcrResult(OcrResultMessage message) {
        if (message == null || message.documentId() == null) {
            log.warn("Ungültige Indexing-Nachricht empfangen: null");
            return;
        }

        if (!message.success()) {
            log.info("OCR-Ergebnis ohne Erfolg übersprungen: id={}, grund={}",
                    message.documentId(), message.errorMessage());
            return;
        }

        try {
            searchIndex.index(message.documentId(), message.filename(), message.text());
        } catch (IndexingException e) {
            log.error("Indexierung fehlgeschlagen für Dokument-ID: {}", message.documentId(), e);
        }
    }
}
