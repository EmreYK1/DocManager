package com.docmanager.indexing.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${app.rabbitmq.queue.indexing}")
    private String indexingQueue;

    @Value("${app.rabbitmq.exchange}")
    private String exchange;

    @Value("${app.rabbitmq.routing-key.ocr-done}")
    private String ocrDoneRoutingKey;

    @Bean
    public Queue indexingQueue() {
        return new Queue(indexingQueue, true);
    }

    @Bean
    public TopicExchange docmanagerExchange() {
        return new TopicExchange(exchange);
    }

    /**
     * Eigene Queue an document.ocr.done – Kopie der Nachricht, ohne dem document-service etwas wegzunehmen.
     */
    @Bean
    public Binding indexingBinding(Queue indexingQueue, TopicExchange docmanagerExchange) {
        return BindingBuilder
                .bind(indexingQueue)
                .to(docmanagerExchange)
                .with(ocrDoneRoutingKey);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        converter.setTypePrecedence(Jackson2JavaTypeMapper.TypePrecedence.INFERRED);
        return converter;
    }
}
