package com.docmanager.document.outbox;

import com.docmanager.messaging.MessagingTopology;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Sendet vorgemerkte Nachrichten in ihrer Reihenfolge. Ist RabbitMQ nicht erreichbar, bleiben sie liegen
 * und werden später gesendet. Dabei kann eine Nachricht doppelt ankommen, deshalb sind alle Empfänger
 * idempotent. Ausgelegt für genau eine Instanz des document-service.
 */
@Slf4j
@Component
public class OutboxRelay {

    private static final long CONFIRM_TIMEOUT_MS = 5000;

    private final OutboxRepository repository;
    private final RabbitTemplate rabbitTemplate;

    public OutboxRelay(OutboxRepository repository, RabbitTemplate rabbitTemplate) {
        this.repository = repository;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Scheduled(fixedDelayString = "${app.outbox.interval-ms:500}")
    public void publishPending() {
        for (OutboxMessage pending : repository.findTop50ByOrderById()) {
            try {
                publish(pending);
            } catch (AmqpException e) {
                log.warn("Nachricht {} wird später erneut gesendet: {}", pending.getId(), e.getMessage());
                return;
            }
            repository.delete(pending);
        }
    }

    // Erst wenn RabbitMQ den Empfang bestätigt hat, darf die Nachricht gelöscht werden
    private void publish(OutboxMessage pending) {
        Message message = MessageBuilder.withBody(pending.getPayload().getBytes(StandardCharsets.UTF_8))
                .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                .build();
        rabbitTemplate.invoke(operations -> {
            operations.send(MessagingTopology.EXCHANGE, pending.getRoutingKey(), message);
            operations.waitForConfirmsOrDie(CONFIRM_TIMEOUT_MS);
            return null;
        });
    }
}
