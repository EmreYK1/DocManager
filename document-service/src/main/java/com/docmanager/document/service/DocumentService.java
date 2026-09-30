package com.docmanager.document.service;

import com.docmanager.document.entity.Document;

import java.util.List;
import java.util.UUID;

public interface DocumentService {

    Document save(Document document);

    Document findById(UUID id);

    List<Document> findAll();

    Document update(UUID id, Document updated);

    void delete(UUID id);
}
