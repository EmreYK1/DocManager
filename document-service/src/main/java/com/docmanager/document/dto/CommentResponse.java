package com.docmanager.document.dto;

import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class CommentResponse {
    private UUID id;
    private String content;
    private String author;
    private Instant createdAt;
    private UUID documentId;
}
