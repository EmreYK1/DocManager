package com.docmanager.document.service.impl;

import com.docmanager.document.exception.StorageException;
import com.docmanager.document.service.StorageService;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;

@Slf4j
@Service
public class MinioStorageService implements StorageService {

    private final MinioClient minioClient;
    private final String bucket;

    public MinioStorageService(MinioClient minioClient, @Value("${app.minio.bucket}") String bucket) {
        this.minioClient = minioClient;
        this.bucket = bucket;
    }

    @Override
    public String store(MultipartFile file) {
        String filename = file.getOriginalFilename();
        String objectKey = UUID.randomUUID() + (filename != null && !filename.isBlank() ? "-" + filename : ".pdf");
        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder().bucket(bucket).object(objectKey).stream(inputStream, file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build());
            log.info("Datei erfolgreich in MinIO gespeichert: bucket={}, objectKey={}", bucket, objectKey);
            return objectKey;
        } catch (Exception e) {
            log.error("Fehler beim Speichern der Datei in MinIO: bucket={}, objectKey={}", bucket, objectKey, e);
            throw new StorageException("Datei konnte nicht in MinIO gespeichert werden", e);
        }
    }

    @Override
    public void delete(String objectKey) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder().bucket(bucket).object(objectKey).build());
            log.info("Datei erfolgreich aus MinIO gelöscht: bucket={}, objectKey={}", bucket, objectKey);
        } catch (Exception e) {
            log.error("Fehler beim Löschen der Datei aus MinIO: bucket={}, objectKey={}", bucket, objectKey, e);
            throw new StorageException("Datei konnte nicht aus MinIO gelöscht werden", e);
        }
    }
}
