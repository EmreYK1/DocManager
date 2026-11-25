package com.docmanager.ocr.service;

import java.io.InputStream;

public interface DocumentStore {

    /**
     * Lädt ein Dokument anhand seines Object-Keys als InputStream aus dem Speicher.
     *
     * @param objectKey der eindeutige Schlüssel in MinIO
     * @return InputStream des Dokuments
     */
    InputStream loadDocument(String objectKey);
}
