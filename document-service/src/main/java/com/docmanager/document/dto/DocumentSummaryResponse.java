package com.docmanager.document.dto;

import com.docmanager.document.entity.DocumentStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Schlanke Dokument-Darstellung für Listen und Suchtreffer. Enthält bewusst keinen OCR-Text
 * und keine Zusammenfassung: Die Übersicht lädt sich bei laufender Texterkennung alle paar
 * Sekunden neu, und der Text aller Dokumente zusammen kann sehr groß sein.
 * Den vollständigen Text liefert {@link DocumentResponse} über {@code GET /documents/{id}}.
 */
public record DocumentSummaryResponse(
        UUID id,
        String filename,
        String contentType,
        long sizeBytes,
        Instant uploadedAt,
        DocumentStatus status,
        UUID folderId,
        String objectKey) {}
