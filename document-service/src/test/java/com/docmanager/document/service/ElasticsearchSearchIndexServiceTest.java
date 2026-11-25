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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Stellt sicher, dass der Elasticsearch-Client beim Start keine Verbindung braucht
 * (CI hat kein Elasticsearch) und Verbindungsfehler als SearchException ankommen.
 */
@SpringBootTest(
        classes = {
            ElasticsearchRestClientAutoConfiguration.class,
            ElasticsearchClientAutoConfiguration.class,
            JacksonAutoConfiguration.class,
            ElasticsearchSearchIndexService.class
        },
        properties = "spring.elasticsearch.uris=http://localhost:1")
class ElasticsearchSearchIndexServiceTest {

    @Autowired
    private ElasticsearchSearchIndexService service;

    @Test
    @DisplayName(
            "buildQuery: Fuzzy-Suche plus Teilstring-Suche im Dateinamen, damit 'helloworld' HelloWorld.pdf findet")
    void buildQuery_combinesFuzzyAndFilenameWildcard() {
        String json = ElasticsearchSearchIndexService.buildQuery("helloworld").toString();

        assertThat(json).contains("multi_match").contains("\"fuzziness\":\"AUTO\"");
        assertThat(json).contains("wildcard").contains("*helloworld*").contains("\"case_insensitive\":true");
    }

    @Test
    @DisplayName("buildQuery: jedes Wort bekommt eigene Teilstring-Suche, Sonderzeichen werden maskiert")
    void buildQuery_perTermWildcards_andEscaping() {
        String json =
                ElasticsearchSearchIndexService.buildQuery("hello wor*ld a").toString();

        assertThat(json).contains("*hello*").contains("*wor\\\\*ld*");
        assertThat(json).doesNotContain("\"*a*\"");
    }

    @Test
    @DisplayName("search() bei nicht erreichbarem Elasticsearch → SearchException")
    void search_unreachable_throwsSearchException() {
        assertThatThrownBy(() -> service.search("Hello")).isInstanceOf(SearchException.class);
    }
}
