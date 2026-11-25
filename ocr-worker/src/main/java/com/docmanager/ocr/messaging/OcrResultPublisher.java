package com.docmanager.ocr.messaging;

import com.docmanager.messaging.MessagingTopology;
import com.docmanager.messaging.OcrResultMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class OcrResultPublisher {

    private static final long CONFIRM_TIMEOUT_MS = 5000;

    private final RabbitTemplate rabbitTemplate;

    public OcrResultPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /** Wirft, wenn RabbitMQ den Empfang nicht bestätigt. Dann wird der Auftrag wiederholt. */
    public void publish(OcrResultMessage result) {
        rabbitTemplate.invoke(operations -> {
            operations.convertAndSend(MessagingTopology.EXCHANGE, MessagingTopology.OCR_RESULT_KEY, result);
            operations.waitForConfirmsOrDie(CONFIRM_TIMEOUT_MS);
            return null;
        });
    }
}
