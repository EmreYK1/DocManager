package com.docmanager.document.service;

import java.util.List;
import java.util.UUID;

/** Lesender Zugriff auf den Suchindex. Geschrieben wird er nur vom indexing-worker. */
public interface SearchIndexService {

    /**
     * Fuzzy-Volltextsuche auf Inhalt und Dateiname.
     *
     * @return Dokument-IDs der Treffer, nach Relevanz sortiert
     */
    List<UUID> search(String query);
}
