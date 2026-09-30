package com.docmanager.document.service;

import com.docmanager.document.entity.Comment;

import java.util.List;
import java.util.UUID;

public interface CommentService {

    Comment save(UUID documentId, String content, String author);

    Comment findById(UUID id);

    List<Comment> findByDocument(UUID documentId);

    Comment update(UUID id, String newContent);

    void delete(UUID id);
}
