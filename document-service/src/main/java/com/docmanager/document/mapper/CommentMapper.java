package com.docmanager.document.mapper;

import com.docmanager.document.dto.CommentResponse;
import com.docmanager.document.entity.Comment;

public class CommentMapper {

    public static CommentResponse toResponse(Comment comment) {
        CommentResponse dto = new CommentResponse();
        dto.setId(comment.getId());
        dto.setContent(comment.getContent());
        dto.setAuthor(comment.getAuthor());
        dto.setCreatedAt(comment.getCreatedAt());
        dto.setDocumentId(comment.getDocument().getId());
        return dto;
    }
}
