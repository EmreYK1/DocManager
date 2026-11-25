package com.docmanager.messaging;

import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.docmanager.messaging.MessagingTopology.*;

/**
 * Jeder Dienst deklariert die gesamte Topologie. So geht keine Nachricht verloren, nur weil ihr
 * Empfänger noch nie gestartet war.
 */
@Configuration(proxyBeanMethods = false)
public class MessagingConfig {

    @Bean
    public Declarables topology() {
        TopicExchange exchange = new TopicExchange(EXCHANGE);
        Queue ocrRequests = new Queue(OCR_REQUEST_QUEUE);
        Queue ocrResults = new Queue(OCR_RESULT_QUEUE);
        Queue index = new Queue(INDEX_QUEUE);
        Queue summaryRequests = new Queue(SUMMARY_REQUEST_QUEUE);
        Queue summaryResults = new Queue(SUMMARY_RESULT_QUEUE);
        return new Declarables(
                exchange,
                ocrRequests,
                ocrResults,
                index,
                summaryRequests,
                summaryResults,
                new Queue(DEAD_LETTER_QUEUE),
                BindingBuilder.bind(ocrRequests).to(exchange).with(OCR_REQUEST_KEY),
                BindingBuilder.bind(ocrResults).to(exchange).with(OCR_RESULT_KEY),
                BindingBuilder.bind(index).to(exchange).with(INDEX_KEY),
                BindingBuilder.bind(summaryRequests).to(exchange).with(SUMMARY_REQUEST_KEY),
                BindingBuilder.bind(summaryResults).to(exchange).with(SUMMARY_RESULT_KEY));
    }

    /** Spring Boot nutzt den Converter für das RabbitTemplate und alle Listener. */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /** Verschiebt eine Nachricht nach den Wiederholungen in die Dead-Letter-Queue. */
    public static MessageRecoverer deadLetterRecoverer(RabbitTemplate rabbitTemplate) {
        return new RepublishMessageRecoverer(rabbitTemplate, "", DEAD_LETTER_QUEUE);
    }
}
