package com.docmanager.indexing.messaging;

import com.docmanager.indexing.service.SearchIndex;
import com.docmanager.messaging.IndexMessage;
import com.docmanager.messaging.MessagingTopology;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/** Fehler werden nicht abgefangen: Sie lösen die Wiederholung aus. Beide Aktionen sind idempotent. */
@Component
public class IndexingListener {

    private final SearchIndex searchIndex;

    public IndexingListener(SearchIndex searchIndex) {
        this.searchIndex = searchIndex;
    }

    @RabbitListener(queues = MessagingTopology.INDEX_QUEUE)
    public void handle(IndexMessage message) {
        if (message.deleted()) {
            searchIndex.delete(message.documentId());
        } else {
            searchIndex.index(message.documentId(), message.filename(), message.content());
        }
    }
}
