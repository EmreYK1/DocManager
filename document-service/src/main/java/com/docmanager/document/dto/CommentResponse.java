package com.docmanager.document.dto;

import java.time.Instant;
import java.util.UUID;

public record CommentResponse(UUID id, String content, String author, Instant createdAt, UUID documentId) {}
