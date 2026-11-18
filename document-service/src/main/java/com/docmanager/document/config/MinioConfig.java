package com.docmanager.document.config;

import com.docmanager.document.exception.StorageException;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class MinioConfig {

    @Value("${app.minio.url}")
    private String minioUrl;

    @Value("${app.minio.access-key}")
    private String accessKey;

    @Value("${app.minio.secret-key}")
    private String secretKey;

    @Value("${app.minio.bucket}")
    private String bucket;

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(minioUrl)
                .credentials(accessKey, secretKey)
                .build();
    }

    @Bean
    public ApplicationRunner initializeBucket(MinioClient minioClient) {
        return args -> {
            try {
                boolean exists = minioClient.bucketExists(
                        BucketExistsArgs.builder().bucket(bucket).build());
                if (!exists) {
                    minioClient.makeBucket(
                            MakeBucketArgs.builder().bucket(bucket).build());
                    log.info("MinIO-Bucket '{}' erfolgreich angelegt", bucket);
                } else {
                    log.info("MinIO-Bucket '{}' existiert bereits", bucket);
                }
            } catch (Exception e) {
                log.error("Fehler beim Initialisieren des MinIO-Buckets '{}'", bucket, e);
                throw new StorageException("MinIO-Bucket konnte nicht initialisiert werden", e);
            }
        };
    }
}

