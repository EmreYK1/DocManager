package com.docmanager.messaging;

import java.util.UUID;

/** genai-worker → document-service: Ergebnis der Zusammenfassung. */
public record SummaryResultMessage(UUID documentId, String summary, boolean success, String errorMessage) {

    public static SummaryResultMessage succeeded(UUID documentId, String summary) {
        return new SummaryResultMessage(documentId, summary, true, null);
    }

    public static SummaryResultMessage failed(UUID documentId, String errorMessage) {
        return new SummaryResultMessage(documentId, null, false, errorMessage);
    }
}
