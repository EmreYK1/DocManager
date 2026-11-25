package com.docmanager.document.dto;

import com.docmanager.document.entity.DocumentStatus;

import java.time.Instant;
import java.util.UUID;

public record DocumentResponse(
        UUID id,
        String filename,
        String contentType,
        long sizeBytes,
        Instant uploadedAt,
        DocumentStatus status,
        String ocrText,
        String summary,
        UUID folderId,
        String objectKey) {}
