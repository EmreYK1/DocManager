package com.docmanager.ocr.config;

import com.docmanager.messaging.MessagingConfig;
import com.docmanager.ocr.messaging.FailedOcrRecoverer;
import com.docmanager.ocr.messaging.OcrResultPublisher;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(MessagingConfig.class)
public class RabbitMQConfig {

    @Bean
    public MessageRecoverer messageRecoverer(
            RabbitTemplate rabbitTemplate, MessageConverter converter, OcrResultPublisher publisher) {
        return new FailedOcrRecoverer(MessagingConfig.deadLetterRecoverer(rabbitTemplate), converter, publisher);
    }
}
