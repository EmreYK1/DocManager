package com.docmanager.document.controller;

import com.docmanager.document.dto.CommentRequest;
import com.docmanager.document.dto.CommentResponse;
import com.docmanager.document.mapper.CommentMapper;
import com.docmanager.document.service.CommentService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/documents/{documentId}/comments")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    // POST /documents/{documentId}/comments
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse create(@PathVariable UUID documentId,
                                  @RequestBody CommentRequest request) {
        return CommentMapper.toResponse(
                commentService.save(documentId, request.getContent(), request.getAuthor()));
    }

    // GET /documents/{documentId}/comments
    @GetMapping
    public List<CommentResponse> getAllForDocument(@PathVariable UUID documentId) {
        return commentService.findByDocument(documentId).stream()
                .map(CommentMapper::toResponse)
                .toList();
    }

    // GET /documents/{documentId}/comments/{id}
    @GetMapping("/{id}")
    public CommentResponse getOne(@PathVariable UUID documentId,
                                  @PathVariable UUID id) {
        return CommentMapper.toResponse(commentService.findById(id));
    }

    // PUT /documents/{documentId}/comments/{id}
    @PutMapping("/{id}")
    public CommentResponse update(@PathVariable UUID documentId,
                                  @PathVariable UUID id,
                                  @RequestBody CommentRequest request) {
        return CommentMapper.toResponse(commentService.update(id, request.getContent()));
    }

    // DELETE /documents/{documentId}/comments/{id}
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID documentId,
                       @PathVariable UUID id) {
        commentService.delete(id);
    }
}
