package com.docmanager.document.dto;

import com.docmanager.document.entity.DocumentStatus;
import lombok.Data;
import java.util.UUID;

@Data
public class DocumentRequest {
    private String filename;
    private String contentType;
    private long sizeBytes;
    private UUID folderId;
    private DocumentStatus status;
}
