package com.docmanager.ocr.messaging;

import java.util.UUID;

public record OcrResultMessage(
        UUID documentId,
        String filename,
        String text,
        boolean success,
        String errorMessage
) {
}

