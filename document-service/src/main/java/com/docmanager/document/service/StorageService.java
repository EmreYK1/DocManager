package com.docmanager.document.service;

import org.springframework.web.multipart.MultipartFile;

public interface StorageService {

    /** Speichert die Datei und liefert den Schlüssel, unter dem sie abgelegt wurde. */
    String store(MultipartFile file);

    void delete(String objectKey);
}
