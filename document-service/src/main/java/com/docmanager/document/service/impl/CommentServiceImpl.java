package com.docmanager.document.service.impl;

import com.docmanager.document.entity.Comment;
import com.docmanager.document.entity.Document;
import com.docmanager.document.exception.NotFoundException;
import com.docmanager.document.repository.CommentRepository;
import com.docmanager.document.service.CommentService;
import com.docmanager.document.service.DocumentService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final DocumentService documentService;

    public CommentServiceImpl(CommentRepository commentRepository, DocumentService documentService) {
        this.commentRepository = commentRepository;
        this.documentService = documentService;
    }

    @Override
    public Comment save(UUID documentId, String content, String author) {
        Document document = documentService.findById(documentId);
        return commentRepository.save(new Comment(content, author, document));
    }

    @Override
    public Comment findById(UUID id) {
        return commentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Comment not found: " + id));
    }

    @Override
    public List<Comment> findByDocument(UUID documentId) {
        Document document = documentService.findById(documentId);
        return commentRepository.findByDocument(document);
    }

    @Override
    public Comment update(UUID id, String newContent) {
        Comment existing = findById(id);
        existing.setContent(newContent);
        return commentRepository.save(existing);
    }

    @Override
    public void delete(UUID id) {
        commentRepository.deleteById(id);
    }
}
