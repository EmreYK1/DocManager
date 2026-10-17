package com.docmanager.document.config;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class RabbitMQConfigTest {

    @Value("${app.rabbitmq.queue.document-processing}")
    private String expectedQueueName;

    @Value("${app.rabbitmq.exchange}")
    private String expectedExchangeName;

    @Value("${app.rabbitmq.routing-key.document}")
    private String expectedRoutingKey;

    @Autowired
    private Queue documentProcessingQueue;

    @Autowired
    private TopicExchange docmanagerExchange;

    @Autowired
    private Binding documentBinding;

    @Test
    void queueNameIsReadFromProperties() {
        assertThat(documentProcessingQueue.getName()).isEqualTo(expectedQueueName);
    }

    @Test
    void exchangeNameIsReadFromProperties() {
        assertThat(docmanagerExchange.getName()).isEqualTo(expectedExchangeName);
    }

    @Test
    void bindingRoutingKeyIsReadFromProperties() {
        assertThat(documentBinding.getRoutingKey()).isEqualTo(expectedRoutingKey);
    }
}
