package com.docmanager.indexing.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import com.docmanager.indexing.exception.IndexingException;
import com.docmanager.indexing.service.SearchIndex;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Service
public class ElasticsearchSearchIndex implements SearchIndex, ApplicationRunner {

    private final ElasticsearchClient client;
    private final String indexName;

    public ElasticsearchSearchIndex(
            ElasticsearchClient client, @Value("${app.elasticsearch.index:documents}") String indexName) {
        this.client = client;
        this.indexName = indexName;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        ensureIndexExists();
    }

    @Override
    public void index(UUID documentId, String filename, String content) {
        try {
            client.index(i ->
                    i.index(indexName).id(documentId.toString()).document(new DocumentDocument(filename, content)));
            log.info("Dokument indexiert: id={}, filename={}", documentId, filename);
        } catch (IOException | RuntimeException e) {
            throw new IndexingException("Indexierung fehlgeschlagen für Dokument " + documentId, e);
        }
    }

    @Override
    public void delete(UUID documentId) {
        try {
            client.delete(d -> d.index(indexName).id(documentId.toString()));
            log.info("Index-Eintrag gelöscht: id={}", documentId);
        } catch (IOException | RuntimeException e) {
            throw new IndexingException("Löschen im Index fehlgeschlagen für Dokument " + documentId, e);
        }
    }

    private void ensureIndexExists() throws Exception {
        boolean exists = client.indices()
                .exists(ExistsRequest.of(e -> e.index(indexName)))
                .value();
        if (exists) {
            log.info("Elasticsearch-Index '{}' existiert bereits", indexName);
            return;
        }

        client.indices().create(c -> c.index(indexName).mappings(m -> m.properties("filename", p -> p.text(t -> t))
                .properties("content", p -> p.text(t -> t))));
        log.info("Elasticsearch-Index '{}' angelegt (filename, content als text)", indexName);
    }

    /**
     * Dokument-Body für den Elasticsearch-Index.
     */
    public record DocumentDocument(String filename, String content) {}
}
