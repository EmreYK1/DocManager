package com.docmanager.genai.config;

import com.docmanager.messaging.MessagingConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(MessagingConfig.class)
public class RabbitMQConfig {

    private static final Logger log = LoggerFactory.getLogger(RabbitMQConfig.class);

    @Bean
    public MessageRecoverer messageRecoverer(RabbitTemplate rabbitTemplate) {
        return MessagingConfig.deadLetterRecoverer(rabbitTemplate);
    }

    /**
     * Spring AMQP verbindet sich erst bei Bedarf. Solange der Worker noch keinen Listener hat, baut er die
     * Verbindung deshalb beim Start selbst auf und deklariert dabei die Topologie.
     */
    @Bean
    public ApplicationRunner rabbitConnectionInitializer(AmqpAdmin amqpAdmin) {
        return args -> {
            amqpAdmin.initialize();
            log.info("Mit RabbitMQ verbunden");
        };
    }
}
