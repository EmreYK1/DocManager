package com.docmanager.document.controller;

import com.docmanager.document.dto.DocumentRequest;
import com.docmanager.document.dto.DocumentResponse;
import com.docmanager.document.entity.Document;
import com.docmanager.document.entity.Folder;
import com.docmanager.document.mapper.DocumentMapper;
import com.docmanager.document.service.DocumentService;
import com.docmanager.document.service.FolderService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/documents")
public class DocumentController {
    private final DocumentService documentService;
    private final FolderService folderService;

    public DocumentController(DocumentService documentService, FolderService folderService) {
        this.documentService = documentService;
        this.folderService = folderService;
    }

    @PostMapping
    public DocumentResponse create(@RequestBody DocumentRequest request) {
        Folder folder = request.getFolderId() != null
                ? folderService.findById(request.getFolderId())
                : null;
        return DocumentMapper.toResponse(documentService.save(
                new Document(request.getFilename(), request.getContentType(), request.getSizeBytes(), folder)));
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

    @PutMapping("/{id}")
    public DocumentResponse update(@PathVariable UUID id, @RequestBody DocumentRequest request) {
        Folder folder = request.getFolderId() != null
                ? folderService.findById(request.getFolderId())
                : null;
        Document updated = new Document(request.getFilename(), request.getContentType(), request.getSizeBytes(), folder);
        if (request.getStatus() != null) {
            updated.setStatus(request.getStatus());
        }
        return DocumentMapper.toResponse(documentService.update(id, updated));
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        documentService.delete(id);
    }
}
