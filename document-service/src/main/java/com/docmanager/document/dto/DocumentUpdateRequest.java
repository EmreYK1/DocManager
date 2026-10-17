package com.docmanager.document.dto;

import com.docmanager.document.entity.DocumentStatus;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.UUID;

@Data
public class DocumentUpdateRequest {
    // PATCH-Semantik: alle Felder optional, kein @NotBlank
    @Size(max = 255)
    private String filename;

    private DocumentStatus status;
    private UUID folderId;
    private boolean clearFolder;
}
