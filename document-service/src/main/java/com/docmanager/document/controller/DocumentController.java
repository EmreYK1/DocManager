package com.docmanager.document.controller;

import com.docmanager.document.dto.DocumentRequest;
import com.docmanager.document.dto.DocumentResponse;
import com.docmanager.document.mapper.DocumentMapper;
import com.docmanager.document.entity.Document;
import com.docmanager.document.service.DocumentService;
import org.springframework.web.bind.annotation.*;
import com.docmanager.document.service.DocumentUpdate;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/documents")
public class DocumentController {
    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping
    public DocumentResponse create(@RequestBody DocumentRequest request) {
        Document document = documentService.create(request.getFilename(), request.getContentType(), request.getSizeBytes(), request.getFolderId());
        return DocumentMapper.toResponse(document);
    }

    @GetMapping("/{id}")
    public DocumentResponse getOne(@PathVariable UUID id) {
        return DocumentMapper.toResponse(documentService.findById(id));
    }

    @GetMapping
    public List<DocumentResponse> getAll() {
        return documentService.findAll().stream()
                .map(DocumentMapper::toResponse)
                .toList();
    }

    @PatchMapping("/{id}")
    public DocumentResponse update(@PathVariable UUID id, @RequestBody DocumentRequest request) {
        DocumentUpdate update = new DocumentUpdate(request.getFilename(), request.getStatus(), request.getFolderId(), request.isClearFolder());
        return DocumentMapper.toResponse(documentService.update(id, update));
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        documentService.delete(id);
    }
}
