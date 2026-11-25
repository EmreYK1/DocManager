package com.docmanager.document.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommentRequest(
        @NotBlank(message = "Kommentarinhalt darf nicht leer sein")
        @Size(max = 2000, message = "Kommentarinhalt darf maximal 2000 Zeichen lang sein")
        String content,

        @NotBlank(message = "Autor darf nicht leer sein")
        @Size(max = 100, message = "Autor darf maximal 100 Zeichen lang sein")
        String author) {}
