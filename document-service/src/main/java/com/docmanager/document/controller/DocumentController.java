package com.docmanager.document.controller;

import com.docmanager.document.dto.DocumentResponse;
import com.docmanager.document.dto.DocumentSummaryResponse;
import com.docmanager.document.dto.DocumentUpdateRequest;
import com.docmanager.document.entity.Document;
import com.docmanager.document.exception.InvalidRequestException;
import com.docmanager.document.mapper.DocumentMapper;
import com.docmanager.document.service.DocumentService;
import com.docmanager.document.service.DocumentUpdate;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
        Document document = documentService.upload(file, folderId);
        return DocumentMapper.toResponse(document);
    }

    @GetMapping("/search")
    public List<DocumentSummaryResponse> search(@RequestParam(value = "q", required = false) String q) {
        if (q == null || q.isBlank()) {
            throw new InvalidRequestException("Der Suchbegriff darf nicht leer sein");
        }
        return documentService.search(q.trim()).stream()
                .map(DocumentMapper::toSummary)
                .toList();
    }

    @GetMapping("/{id}")
    public DocumentResponse getOne(@PathVariable UUID id) {
        return DocumentMapper.toResponse(documentService.findById(id));
    }

    @GetMapping
    public List<DocumentSummaryResponse> getAll() {
        return documentService.findAll().stream().map(DocumentMapper::toSummary).toList();
    }

    @PatchMapping("/{id}")
    public DocumentResponse update(@PathVariable UUID id, @Valid @RequestBody DocumentUpdateRequest request) {
        DocumentUpdate update = new DocumentUpdate(request.filename(), request.folderId(), request.clearFolder());
        return DocumentMapper.toResponse(documentService.update(id, update));
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        documentService.delete(id);
    }
}
