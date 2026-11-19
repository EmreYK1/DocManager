package com.docmanager.document.service;

import com.docmanager.document.exception.SearchException;
import com.docmanager.document.service.impl.ElasticsearchSearchIndexService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchClientAutoConfiguration;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchRestClientAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Stellt sicher, dass der Elasticsearch-Client beim Start keine Verbindung braucht
 * (CI hat kein Elasticsearch) und Verbindungsfehler als SearchException ankommen.
 */
@SpringBootTest(
        classes = {ElasticsearchRestClientAutoConfiguration.class, ElasticsearchClientAutoConfiguration.class,
                JacksonAutoConfiguration.class,
                ElasticsearchSearchIndexService.class},
        properties = "spring.elasticsearch.uris=http://localhost:1")
class ElasticsearchSearchIndexServiceTest {

    @Autowired
    private ElasticsearchSearchIndexService service;

    @Test
    @DisplayName("Kontext startet ohne erreichbares Elasticsearch")
    void contextStartsWithoutElasticsearch() {
        assertThat(service).isNotNull();
    }

    @Test
    @DisplayName("search() bei nicht erreichbarem Elasticsearch → SearchException")
    void search_unreachable_throwsSearchException() {
        assertThatThrownBy(() -> service.search("Hello")).isInstanceOf(SearchException.class);
    }

    @Test
    @DisplayName("delete() bei nicht erreichbarem Elasticsearch → SearchException")
    void delete_unreachable_throwsSearchException() {
        assertThatThrownBy(() -> service.delete(UUID.randomUUID())).isInstanceOf(SearchException.class);
    }
}
