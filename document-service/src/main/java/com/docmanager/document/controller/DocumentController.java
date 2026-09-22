package com.docmanager.document.controller;

import com.docmanager.document.entity.Document;
import com.docmanager.document.service.DocumentService;
import org.springframework.web.bind.annotation.*;

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
    public Document create(@RequestBody Document document){
        return documentService.save(document);
    }

    @GetMapping("/{id}")
    public Document getOne(@PathVariable UUID id){
        return documentService.findById(id);
    }

    @GetMapping
    public List<Document> getAll(){
        return documentService.findAll();
}

    @PutMapping("/{id}")
    public Document update(@PathVariable UUID id, @RequestBody Document document) {
        return documentService.update(id, document);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        documentService.delete(id);
    }
}