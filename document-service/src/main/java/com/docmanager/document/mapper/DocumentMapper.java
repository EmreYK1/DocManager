package com.docmanager.document.mapper;

import com.docmanager.document.dto.DocumentResponse;
import com.docmanager.document.entity.Document;

public class DocumentMapper {

    public static DocumentResponse toResponse(Document document) {
        DocumentResponse dto = new DocumentResponse();
        dto.setId(document.getId());
        dto.setFilename(document.getFilename());
        dto.setContentType(document.getContentType());
        dto.setSizeBytes(document.getSizeBytes());
        dto.setUploadedAt(document.getUploadedAt());
        dto.setStatus(document.getStatus());
        dto.setOcrText(document.getOcrText());
        dto.setSummary(document.getSummary());
        if (document.getFolder() != null) {
            dto.setFolderId(document.getFolder().getId());
        }
        return dto;
    }
}
