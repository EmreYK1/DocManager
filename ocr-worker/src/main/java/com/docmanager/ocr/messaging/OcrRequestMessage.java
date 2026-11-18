package com.docmanager.ocr.messaging;

import java.util.UUID;

public record OcrRequestMessage(
        UUID documentId,
        String objectKey,
        String filename
) {
}

