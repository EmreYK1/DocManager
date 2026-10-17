package com.docmanager.document.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${app.rabbitmq.queue.document-processing}")
    private String documentProcessingQueue;

    @Value("${app.rabbitmq.exchange}")
    private String exchange;

    @Value("${app.rabbitmq.routing-key.document}")
    private String documentRoutingKey;

    /**
     * Queue für die Verarbeitung von Dokumenten-Ereignissen.
     */
    @Bean
    public Queue documentProcessingQueue() {
        return new Queue(documentProcessingQueue, true);
    }

    /**
     * Topic-Exchange, über den alle Dokument-Events geroutet werden.
     */
    @Bean
    public TopicExchange docmanagerExchange() {
        return new TopicExchange(exchange);
    }

    /**
     * Bindet die Queue an den Exchange mit dem konfigurierten Routing-Key.
     */
    @Bean
    public Binding documentBinding(Queue documentProcessingQueue, TopicExchange docmanagerExchange) {
        return BindingBuilder
                .bind(documentProcessingQueue)
                .to(docmanagerExchange)
                .with(documentRoutingKey);
    }

    /**
     * Verwendet Jackson zur JSON-Serialisierung von AMQP-Nachrichten.
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * RabbitTemplate mit JSON-MessageConverter, bereit zur Injektion.
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}
