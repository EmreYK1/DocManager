package com.docmanager.document.messaging;

import java.util.UUID;

public record OcrRequestMessage(
        UUID documentId,
        String objectKey,
        String filename
) {
}

