package com.docmanager.document.dto;

import com.docmanager.document.entity.DocumentStatus;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

/**
 * Schlanke Dokument-Darstellung für Listen und Suchtreffer. Enthält bewusst keinen OCR-Text
 * und keine Zusammenfassung: Die Übersicht lädt sich bei laufender Texterkennung alle paar
 * Sekunden neu, und der Text aller Dokumente zusammen kann sehr groß sein.
 * Den vollständigen Text liefert {@link DocumentResponse} über {@code GET /documents/{id}}.
 */
@Data
public class DocumentSummaryResponse {
    private UUID id;
    private String filename;
    private String contentType;
    private long sizeBytes;
    private Instant uploadedAt;
    private DocumentStatus status;
    private UUID folderId;
    private String objectKey;
}
