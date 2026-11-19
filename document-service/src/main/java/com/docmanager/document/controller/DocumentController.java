package com.docmanager.document.controller;

import com.docmanager.document.dto.DocumentRequest;
import com.docmanager.document.dto.DocumentUpdateRequest;
import com.docmanager.document.dto.DocumentResponse;
import com.docmanager.document.exception.InvalidFileException;
import com.docmanager.document.exception.InvalidQueryException;
import com.docmanager.document.mapper.DocumentMapper;
import com.docmanager.document.entity.Document;
import com.docmanager.document.service.DocumentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
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

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentResponse upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folderId", required = false) UUID folderId) {
        if (file.isEmpty()) {
            throw new InvalidFileException("Die Datei darf nicht leer sein");
        }
        if (file.getContentType() == null || !"application/pdf".equalsIgnoreCase(file.getContentType())) {
            throw new InvalidFileException("Nur PDF-Dateien sind erlaubt");
        }
        Document document = documentService.upload(file, folderId);
        return DocumentMapper.toResponse(document);
    }

    @PostMapping
    public DocumentResponse create(@Valid @RequestBody DocumentRequest request) {
        Document document = documentService.create(request.getFilename(), request.getContentType(), request.getSizeBytes(), request.getFolderId());
        return DocumentMapper.toResponse(document);
    }

    @GetMapping("/search")
    public List<DocumentResponse> search(@RequestParam(value = "q", required = false) String q) {
        if (q == null || q.isBlank()) {
            throw new InvalidQueryException("Der Suchbegriff darf nicht leer sein");
        }
        return documentService.search(q.trim()).stream()
                .map(DocumentMapper::toResponse)
                .toList();
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
    public DocumentResponse update(@PathVariable UUID id, @Valid @RequestBody DocumentUpdateRequest request) {
        DocumentUpdate update = new DocumentUpdate(request.getFilename(), request.getStatus(), request.getFolderId(), request.isClearFolder());
        return DocumentMapper.toResponse(documentService.update(id, update));
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        documentService.delete(id);
    }
}
