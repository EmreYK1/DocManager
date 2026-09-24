package com.docmanager.document.service;

import com.docmanager.document.entity.Comment;
import com.docmanager.document.entity.Document;
import com.docmanager.document.repository.CommentRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final DocumentService documentService;

    public CommentService(CommentRepository commentRepository, DocumentService documentService) {
        this.commentRepository = commentRepository;
        this.documentService = documentService;
    }

    public Comment save(UUID documentId, String content, String author) {
        Document document = documentService.findById(documentId);
        return commentRepository.save(new Comment(content, author, document));
    }

    public Comment findById(UUID id) {
        return commentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Comment not found: " + id));
    }

    public List<Comment> findByDocument(UUID documentId) {
        Document document = documentService.findById(documentId);
        return commentRepository.findByDocument(document);
    }

    public Comment update(UUID id, String newContent) {
        Comment existing = findById(id);
        existing.setContent(newContent);
        return commentRepository.save(existing);
    }

    public void delete(UUID id) {
        commentRepository.deleteById(id);
    }
}
