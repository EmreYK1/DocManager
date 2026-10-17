package com.docmanager.document.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.UUID;

@Data
public class FolderRequest {
    @NotBlank(message = "Ordnername darf nicht leer sein")
    @Size(max = 255)
    private String name;

    private UUID parentId;
}
