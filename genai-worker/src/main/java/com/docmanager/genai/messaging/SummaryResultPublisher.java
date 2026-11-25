package com.docmanager.genai.messaging;

import com.docmanager.messaging.MessagingTopology;
import com.docmanager.messaging.SummaryResultMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class SummaryResultPublisher {

    private static final long CONFIRM_TIMEOUT_MS = 5000;

    private final RabbitTemplate rabbitTemplate;

    public SummaryResultPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /** Wirft, wenn RabbitMQ den Empfang nicht bestätigt. Dann wird der Auftrag wiederholt. */
    public void publish(SummaryResultMessage result) {
        rabbitTemplate.invoke(operations -> {
            operations.convertAndSend(MessagingTopology.EXCHANGE, MessagingTopology.SUMMARY_RESULT_KEY, result);
            operations.waitForConfirmsOrDie(CONFIRM_TIMEOUT_MS);
            return null;
        });
    }
}
