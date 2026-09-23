package com.docmanager.document.service;

import com.docmanager.document.entity.Document;
import com.docmanager.document.repository.DocumentRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class DocumentService {

    private final DocumentRepository documentRepository;

    public DocumentService(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    public Document save(Document document) {
        return documentRepository.save(document);
    }

    public Document findById(UUID id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found: " + id));
    }

    public List<Document> findAll() {
        return documentRepository.findAll();
    }

    public Document update(UUID id, Document updated) {
        Document existing = findById(id);
        existing.setFilename(updated.getFilename());
        existing.setStatus(updated.getStatus());
        existing.setFolder(updated.getFolder());
        return documentRepository.save(existing);
    }

    public void delete(UUID id) {
        documentRepository.deleteById(id);
    }
}