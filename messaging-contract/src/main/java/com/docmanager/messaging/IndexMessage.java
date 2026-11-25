package com.docmanager.messaging;

import java.util.UUID;

/**
 * document-service → indexing-worker: Suchindex anpassen. Nur der document-service kennt den aktuellen
 * Stand eines Dokuments. Weil alle Änderungen durch dieselbe Queue laufen, bleibt ihre Reihenfolge erhalten.
 */
public record IndexMessage(UUID documentId, String filename, String content, boolean deleted) {

    public static IndexMessage upsert(UUID documentId, String filename, String content) {
        return new IndexMessage(documentId, filename, content, false);
    }

    public static IndexMessage delete(UUID documentId) {
        return new IndexMessage(documentId, null, null, true);
    }
}
