package com.docmanager.document.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${app.rabbitmq.queue.document-processing}")
    private String documentProcessingQueue;

    @Value("${app.rabbitmq.queue.ocr-result:document.ocr.result}")
    private String ocrResultQueue;

    @Value("${app.rabbitmq.exchange}")
    private String exchange;

    @Value("${app.rabbitmq.routing-key.document}")
    private String documentRoutingKey;

    @Value("${app.rabbitmq.routing-key.ocr-done:document.ocr.done}")
    private String ocrDoneRoutingKey;

    /**
     * Queue für die Verarbeitung von Dokumenten-Ereignissen.
     */
    @Bean
    public Queue documentProcessingQueue() {
        return new Queue(documentProcessingQueue, true);
    }

    /**
     * Queue für OCR-Ergebnisse, die der document-service empfängt.
     */
    @Bean
    public Queue ocrResultQueue() {
        return new Queue(ocrResultQueue, true);
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
     * Bindet die ocrResultQueue an den Exchange mit dem Routing-Key document.ocr.done.
     */
    @Bean
    public Binding ocrResultBinding(Queue ocrResultQueue, TopicExchange docmanagerExchange) {
        return BindingBuilder
                .bind(ocrResultQueue)
                .to(docmanagerExchange)
                .with(ocrDoneRoutingKey);
    }

    /**
     * Verwendet Jackson zur JSON-Serialisierung von AMQP-Nachrichten
     * mit INFERRED TypePrecedence, um nachrichtenspezifische Klassennamen aus fremden Modulen zu mappen.
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        converter.setTypePrecedence(Jackson2JavaTypeMapper.TypePrecedence.INFERRED);
        return converter;
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
