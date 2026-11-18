package com.docmanager.document.service;

import org.springframework.web.multipart.MultipartFile;

public interface StorageService {

    String store(MultipartFile file);

    void store(String objectKey, MultipartFile file);

    void delete(String objectKey);
}

