package com.docmanager.ocr.service.impl;

import com.docmanager.ocr.exception.StorageException;
import com.docmanager.ocr.service.DocumentStore;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Slf4j
@Service
public class MinioDocumentStore implements DocumentStore {

    private final MinioClient minioClient;
    private final String bucket;

    public MinioDocumentStore(MinioClient minioClient,
                              @Value("${app.minio.bucket}") String bucket) {
        this.minioClient = minioClient;
        this.bucket = bucket;
    }

    @Override
    public InputStream loadDocument(String objectKey) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectKey)
                            .build()
            );
        } catch (Exception e) {
            log.error("Fehler beim Laden des Dokuments aus MinIO: bucket={}, objectKey={}", bucket, objectKey, e);
            throw new StorageException("Dokument konnte nicht aus MinIO geladen werden: " + objectKey, e);
        }
    }
}

