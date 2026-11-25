package com.docmanager.document.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record FolderRequest(
        @NotBlank(message = "Ordnername darf nicht leer sein") @Size(max = 255)
        String name,

        UUID parentId) {}
