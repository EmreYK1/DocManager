package com.docmanager.ocr.config;

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

    @Value("${app.rabbitmq.exchange}")
    private String exchange;

    @Value("${app.rabbitmq.routing-key.document}")
    private String documentRoutingKey;

    @Bean
    public Queue documentProcessingQueue() {
        return new Queue(documentProcessingQueue, true);
    }

    @Bean
    public TopicExchange docmanagerExchange() {
        return new TopicExchange(exchange);
    }

    @Bean
    public Binding documentBinding(Queue documentProcessingQueue, TopicExchange docmanagerExchange) {
        return BindingBuilder
                .bind(documentProcessingQueue)
                .to(docmanagerExchange)
                .with(documentRoutingKey);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        converter.setTypePrecedence(Jackson2JavaTypeMapper.TypePrecedence.INFERRED);
        return converter;
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}

