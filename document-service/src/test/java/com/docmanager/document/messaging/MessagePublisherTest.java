package com.docmanager.document.messaging;

import com.docmanager.messaging.MessagingTopology;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessagePublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private MessagePublisher publisher;

    @AfterEach
    void clearTransaction() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    @DisplayName("Ohne Transaktion wird sofort gesendet")
    void withoutTransaction_sendsImmediately() {
        publisher.send(MessagingTopology.INDEX_KEY, "nachricht");

        verify(rabbitTemplate).convertAndSend(MessagingTopology.EXCHANGE, MessagingTopology.INDEX_KEY, "nachricht");
    }

    @Test
    @DisplayName("In einer Transaktion wird erst nach dem Commit gesendet")
    void withinTransaction_sendsAfterCommit() {
        TransactionSynchronizationManager.initSynchronization();

        publisher.send(MessagingTopology.INDEX_KEY, "nachricht");
        verifyNoInteractions(rabbitTemplate);

        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        verify(rabbitTemplate).convertAndSend(MessagingTopology.EXCHANGE, MessagingTopology.INDEX_KEY, "nachricht");
    }

    @Test
    @DisplayName("Ein Sendefehler wird geloggt und nicht weitergeworfen")
    void sendFailure_isOnlyLogged() {
        doThrow(new AmqpException("RabbitMQ weg"))
                .when(rabbitTemplate)
                .convertAndSend(MessagingTopology.EXCHANGE, MessagingTopology.INDEX_KEY, "nachricht");

        assertThatCode(() -> publisher.send(MessagingTopology.INDEX_KEY, "nachricht"))
                .doesNotThrowAnyException();
    }
}
