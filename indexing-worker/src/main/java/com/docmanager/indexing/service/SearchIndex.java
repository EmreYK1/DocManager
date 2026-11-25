package com.docmanager.indexing.service;

import java.util.UUID;

public interface SearchIndex {

    /** Legt den Eintrag an oder ersetzt ihn. */
    void index(UUID documentId, String filename, String content);

    /** Ein nicht vorhandener Eintrag ist kein Fehler. */
    void delete(UUID documentId);
}
