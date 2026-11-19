package com.docmanager.document.service;

import java.util.List;
import java.util.UUID;

public interface SearchIndexService {

    /**
     * Fuzzy-Volltextsuche auf Inhalt und Dateiname.
     *
     * @return Dokument-IDs der Treffer, nach Relevanz sortiert
     */
    List<UUID> search(String query);

    /**
     * Entfernt den Index-Eintrag eines Dokuments. Nicht vorhandene Einträge sind kein Fehler.
     */
    void delete(UUID documentId);
}
