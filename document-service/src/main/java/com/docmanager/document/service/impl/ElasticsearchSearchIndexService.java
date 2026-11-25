package com.docmanager.document.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MultiMatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.docmanager.document.exception.SearchException;
import com.docmanager.document.service.SearchIndexService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
public class ElasticsearchSearchIndexService implements SearchIndexService {

    private static final int MAX_HITS = 100;
    private static final int MIN_WILDCARD_TERM_LENGTH = 2;

    private final ElasticsearchClient client;
    private final String indexName;

    public ElasticsearchSearchIndexService(
            ElasticsearchClient client, @Value("${app.elasticsearch.index:documents}") String indexName) {
        this.client = client;
        this.indexName = indexName;
    }

    @Override
    public List<UUID> search(String query) {
        try {
            SearchResponse<Void> response =
                    client.search(s -> s.index(indexName).size(MAX_HITS).query(buildQuery(query)), Void.class);
            return response.hits().hits().stream()
                    .map(Hit::id)
                    .map(ElasticsearchSearchIndexService::parseId)
                    .filter(Objects::nonNull)
                    .toList();
        } catch (IOException | RuntimeException e) {
            throw new SearchException("Suche in Elasticsearch fehlgeschlagen", e);
        }
    }

    /**
     * Fuzzy-Suche auf Inhalt und Dateiname (findet auch Tippfehler) plus Teilstring-Suche im
     * Dateinamen. Letztere ist nötig, weil Elasticsearch „HelloWorld.pdf“ als EIN Wort
     * („helloworld.pdf“) indexiert und „helloworld“ sonst zu weit davon entfernt wäre.
     */
    public static Query buildQuery(String query) {
        BoolQuery.Builder bool = new BoolQuery.Builder()
                .minimumShouldMatch("1")
                .should(s -> s.multiMatch(MultiMatchQuery.of(
                        m -> m.query(query).fields("content", "filename").fuzziness("AUTO"))));
        for (String term : query.trim().split("\\s+")) {
            if (term.length() >= MIN_WILDCARD_TERM_LENGTH) {
                String pattern = "*" + escapeWildcard(term) + "*";
                bool.should(
                        s -> s.wildcard(w -> w.field("filename").value(pattern).caseInsensitive(true)));
            }
        }
        return Query.of(q -> q.bool(bool.build()));
    }

    private static String escapeWildcard(String term) {
        return term.replace("\\", "\\\\").replace("*", "\\*").replace("?", "\\?");
    }

    private static UUID parseId(String id) {
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            log.warn("Ungültige ID im Index übersprungen: {}", id);
            return null;
        }
    }
}
