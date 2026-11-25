package com.docmanager.document.messaging;

import com.docmanager.messaging.MessagingTopology;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Sendet Nachrichten an RabbitMQ. Läuft eine Transaktion, wird erst nach ihrem Commit gesendet, damit
 * kein Empfänger ein Dokument abfragt, das noch nicht gespeichert ist.
 */
@Slf4j
@Component
public class MessagePublisher {

    private final RabbitTemplate rabbitTemplate;

    public MessagePublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void send(String routingKey, Object message) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            publish(routingKey, message);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                publish(routingKey, message);
            }
        });
    }

    // Die Änderung in der Datenbank ist schon gespeichert, ein Sendefehler wird deshalb nur geloggt
    private void publish(String routingKey, Object message) {
        try {
            rabbitTemplate.convertAndSend(MessagingTopology.EXCHANGE, routingKey, message);
        } catch (AmqpException e) {
            log.error("Nachricht konnte nicht gesendet werden: routingKey={}, nachricht={}", routingKey, message, e);
        }
    }
}
