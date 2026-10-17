package com.docmanager.document.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.util.UUID;

@Data
public class DocumentRequest {
    @NotBlank(message = "Dateiname darf nicht leer sein")
    private String filename;

    @NotBlank(message = "Content-Type darf nicht leer sein")
    private String contentType;

    @Positive(message = "Dateigröße muss größer als 0 sein")
    private long sizeBytes;

    private UUID folderId;
}
