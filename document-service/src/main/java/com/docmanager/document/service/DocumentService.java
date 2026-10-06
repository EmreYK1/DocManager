package com.docmanager.document.service;

import com.docmanager.document.entity.Document;

import java.util.List;
import java.util.UUID;

public interface DocumentService {

    Document findById(UUID id);

    List<Document> findAll();

    Document create(String filename, String contentType, long sizeBytes, UUID folderID);

    Document update(UUID id, DocumentUpdate update);

    void delete(UUID id);
}
