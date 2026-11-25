package com.docmanager.document.service;

import com.docmanager.document.entity.Comment;
import com.docmanager.document.entity.Document;
import com.docmanager.document.exception.NotFoundException;
import com.docmanager.document.repository.CommentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
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

    /** Ein Kommentar ist nur über das Dokument erreichbar, zu dem er gehört. */
    @Transactional(readOnly = true)
    public Comment findById(UUID documentId, UUID id) {
        return commentRepository
                .findById(id)
                .filter(comment -> comment.getDocument().getId().equals(documentId))
                .orElseThrow(() -> new NotFoundException("Kommentar nicht gefunden: " + id));
    }

    @Transactional(readOnly = true)
    public List<Comment> findByDocument(UUID documentId) {
        return commentRepository.findByDocument(documentService.findById(documentId));
    }

    public Comment update(UUID documentId, UUID id, String newContent) {
        Comment comment = findById(documentId, id);
        comment.edit(newContent);
        return comment;
    }

    public void delete(UUID documentId, UUID id) {
        commentRepository.delete(findById(documentId, id));
    }
}
