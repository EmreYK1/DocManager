package com.docmanager.messaging;

import java.util.UUID;

/** document-service → genai-worker: Bitte diesen Text zusammenfassen. */
public record SummaryRequestMessage(UUID documentId, String text) {}
