package com.docmanager.document.mapper;

import com.docmanager.document.dto.CommentResponse;
import com.docmanager.document.entity.Comment;

public final class CommentMapper {

    private CommentMapper() {}

    public static CommentResponse toResponse(Comment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getContent(),
                comment.getAuthor(),
                comment.getCreatedAt(),
                comment.getDocument().getId());
    }
}
