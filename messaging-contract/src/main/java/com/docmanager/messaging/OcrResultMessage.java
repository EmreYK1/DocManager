package com.docmanager.messaging;

import java.util.UUID;

/** ocr-worker → document-service: Ergebnis der Texterkennung. */
public record OcrResultMessage(UUID documentId, String text, boolean success, String errorMessage) {

    public static OcrResultMessage succeeded(UUID documentId, String text) {
        return new OcrResultMessage(documentId, text, true, null);
    }

    public static OcrResultMessage failed(UUID documentId, String errorMessage) {
        return new OcrResultMessage(documentId, null, false, errorMessage);
    }
}
