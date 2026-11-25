package com.docmanager.messaging;

import java.util.UUID;

/** document-service → ocr-worker: Bitte den Text dieser Datei erkennen. */
public record OcrRequestMessage(UUID documentId, String objectKey) {}
