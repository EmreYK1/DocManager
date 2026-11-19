package com.docmanager.indexing.service;

import com.docmanager.indexing.exception.IndexingException;

import java.util.UUID;

public interface SearchIndex {

    void index(UUID documentId, String filename, String content) throws IndexingException;
}
