package com.docmanager.document.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CommentRequest {
    @NotBlank(message = "Kommentarinhalt darf nicht leer sein")
    @Size(max = 2000)
    private String content;

    @NotBlank(message = "Autor darf nicht leer sein")
    @Size(max = 100)
    private String author;
}
