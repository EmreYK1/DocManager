package com.docmanager.document.service.impl;

import com.docmanager.document.entity.Document;
import com.docmanager.document.repository.DocumentRepository;
import com.docmanager.document.service.DocumentService;
import com.docmanager.document.service.DocumentUpdate;
import org.springframework.stereotype.Service;
import com.docmanager.document.service.FolderService;
import com.docmanager.document.entity.Folder;
import java.util.List;
import java.util.UUID;

@Service
public class DocumentServiceImpl implements DocumentService {

    private final DocumentRepository documentRepository;
    private final FolderService folderService;
    
    public DocumentServiceImpl(DocumentRepository documentRepository, FolderService folderService) {
        this.documentRepository = documentRepository;
        this.folderService = folderService;
    }

    @Override
    public Document create(String filename, String contentType, long sizeBytes, UUID folderId) {
        Folder folder = folderId != null
                ? folderService.findById(folderId)
                : null;
                Document doc = new Document(filename, contentType, sizeBytes, folder);
                return documentRepository.save(doc);
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
    public Document update(UUID id, DocumentUpdate update) {
        Document existing = findById(id);
        if (update.filename() != null) existing.setFilename(update.filename());
        if (update.status() != null) existing.setStatus(update.status());
        if (update.folderId() != null) existing.setFolder(folderService.findById(update.folderId()));
        return documentRepository.save(existing);
    }

    @Override
    public void delete(UUID id) {
        documentRepository.deleteById(id);
    }
}
