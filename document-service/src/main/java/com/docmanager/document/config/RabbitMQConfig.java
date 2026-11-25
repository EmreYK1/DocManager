package com.docmanager.document.config;

import com.docmanager.messaging.MessagingConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@Import(MessagingConfig.class)
@EnableScheduling // für den OutboxRelay
public class RabbitMQConfig {

    @Bean
    public MessageRecoverer messageRecoverer(RabbitTemplate rabbitTemplate) {
        return MessagingConfig.deadLetterRecoverer(rabbitTemplate);
    }
}
