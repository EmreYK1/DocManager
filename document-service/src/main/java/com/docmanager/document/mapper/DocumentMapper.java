package com.docmanager.document.mapper;

import com.docmanager.document.dto.DocumentResponse;
import com.docmanager.document.dto.DocumentSummaryResponse;
import com.docmanager.document.entity.Document;
import com.docmanager.document.entity.Folder;

import java.util.UUID;

public final class DocumentMapper {

    private DocumentMapper() {}

    public static DocumentResponse toResponse(Document d) {
        return new DocumentResponse(
                d.getId(),
                d.getFilename(),
                d.getContentType(),
                d.getSizeBytes(),
                d.getUploadedAt(),
                d.getStatus(),
                d.getOcrText(),
                d.getSummary(),
                folderId(d),
                d.getObjectKey());
    }

    /** Wie {@link #toResponse}, aber ohne OCR-Text und Zusammenfassung (für Listen und Suche). */
    public static DocumentSummaryResponse toSummary(Document d) {
        return new DocumentSummaryResponse(
                d.getId(),
                d.getFilename(),
                d.getContentType(),
                d.getSizeBytes(),
                d.getUploadedAt(),
                d.getStatus(),
                folderId(d),
                d.getObjectKey());
    }

    private static UUID folderId(Document document) {
        Folder folder = document.getFolder();
        return folder != null ? folder.getId() : null;
    }
}
