package com.docmanager.document.dto;

import com.docmanager.document.entity.DocumentStatus;
import lombok.Data;
import java.util.UUID;

@Data
public class DocumentUpdateRequest {
    private String filename;
    private DocumentStatus status;
    private UUID folderId;
    private boolean clearFolder;
}
