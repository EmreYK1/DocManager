package com.docmanager.document.controller;

import com.docmanager.document.dto.CommentRequest;
import com.docmanager.document.dto.CommentResponse;
import com.docmanager.document.dto.CommentUpdateRequest;
import com.docmanager.document.mapper.CommentMapper;
import com.docmanager.document.service.CommentService;
import jakarta.validation.Valid;
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

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse create(@PathVariable UUID documentId, @Valid @RequestBody CommentRequest request) {
        return CommentMapper.toResponse(commentService.save(documentId, request.content(), request.author()));
    }

    @GetMapping
    public List<CommentResponse> getAllForDocument(@PathVariable UUID documentId) {
        return commentService.findByDocument(documentId).stream()
                .map(CommentMapper::toResponse)
                .toList();
    }

    @GetMapping("/{id}")
    public CommentResponse getOne(@PathVariable UUID documentId, @PathVariable UUID id) {
        return CommentMapper.toResponse(commentService.findById(documentId, id));
    }

    @PutMapping("/{id}")
    public CommentResponse update(
            @PathVariable UUID documentId, @PathVariable UUID id, @Valid @RequestBody CommentUpdateRequest request) {
        return CommentMapper.toResponse(commentService.update(documentId, id, request.content()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID documentId, @PathVariable UUID id) {
        commentService.delete(documentId, id);
    }
}
