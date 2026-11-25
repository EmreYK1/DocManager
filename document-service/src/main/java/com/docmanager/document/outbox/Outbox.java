package com.docmanager.document.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Merkt Nachrichten in derselben Datenbank-Transaktion wie die Änderung vor: Entweder wird beides
 * gespeichert oder nichts. Das Senden übernimmt der {@link OutboxRelay}.
 */
@Component
public class Outbox {

    private final OutboxRepository repository;
    private final ObjectMapper objectMapper;

    public Outbox(OutboxRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void add(String routingKey, Object message) {
        try {
            repository.save(new OutboxMessage(routingKey, objectMapper.writeValueAsString(message)));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Nachricht nicht serialisierbar: " + message, e);
        }
    }
}
