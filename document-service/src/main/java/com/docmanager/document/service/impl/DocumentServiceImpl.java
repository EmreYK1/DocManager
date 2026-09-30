package com.docmanager.document.service.impl;

import com.docmanager.document.entity.Document;
import com.docmanager.document.repository.DocumentRepository;
import com.docmanager.document.service.DocumentService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class DocumentServiceImpl implements DocumentService {

    private final DocumentRepository documentRepository;

    public DocumentServiceImpl(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    @Override
    public Document save(Document document) {
        return documentRepository.save(document);
    }

    @Override
    public Document findById(UUID id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found: " + id));
    }

    @Override
    public List<Document> findAll() {
        return documentRepository.findAll();
    }

    @Override
    public Document update(UUID id, Document updated) {
        Document existing = findById(id);
        existing.setFilename(updated.getFilename());
        existing.setStatus(updated.getStatus());
        existing.setFolder(updated.getFolder());
        return documentRepository.save(existing);
    }

    @Override
    public void delete(UUID id) {
        documentRepository.deleteById(id);
    }
}
