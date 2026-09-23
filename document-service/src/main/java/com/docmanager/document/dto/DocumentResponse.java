package com.docmanager.document.dto;

import com.docmanager.document.entity.DocumentStatus;
import lombok.Data;
import java.time.Instant;
import java.util.UUID;

@Data
public class DocumentResponse {
    private UUID id;
    private String filename;
    private String contentType;
    private long sizeBytes;
    private Instant uploadedAt;
    private DocumentStatus status;
    private String ocrText;
    private String summary;
    private UUID folderId;
}
