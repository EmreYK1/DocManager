package com.docmanager.document.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.MultiMatchQuery;
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

    private final ElasticsearchClient client;
    private final String indexName;

    public ElasticsearchSearchIndexService(ElasticsearchClient client,
                                           @Value("${app.elasticsearch.index:documents}") String indexName) {
        this.client = client;
        this.indexName = indexName;
    }

    @Override
    public List<UUID> search(String query) {
        try {
            SearchResponse<Void> response = client.search(s -> s
                            .index(indexName)
                            .size(MAX_HITS)
                            .query(q -> q.multiMatch(MultiMatchQuery.of(m -> m
                                    .query(query)
                                    .fields("content", "filename")
                                    .fuzziness("AUTO")))),
                    Void.class);
            return response.hits().hits().stream()
                    .map(Hit::id)
                    .map(ElasticsearchSearchIndexService::parseId)
                    .filter(Objects::nonNull)
                    .toList();
        } catch (IOException | RuntimeException e) {
            throw new SearchException("Suche in Elasticsearch fehlgeschlagen", e);
        }
    }

    @Override
    public void delete(UUID documentId) {
        try {
            client.delete(d -> d.index(indexName).id(documentId.toString()));
            log.info("Index-Eintrag gelöscht: id={}", documentId);
        } catch (IOException | RuntimeException e) {
            throw new SearchException("Löschen im Elasticsearch-Index fehlgeschlagen: " + documentId, e);
        }
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
