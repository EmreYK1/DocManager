package com.docmanager.document.service;

import com.docmanager.document.exception.StorageException;
import com.docmanager.document.service.impl.MinioStorageService;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MinioStorageServiceTest {

    @Mock
    private MinioClient minioClient;

    private MinioStorageService storageService;
    private final String bucket = "test-bucket";

    @BeforeEach
    void setUp() {
        storageService = new MinioStorageService(minioClient, bucket);
    }

    @Test
    @DisplayName("store() lädt Datei erfolgreich in MinIO hoch und liefert objectKey")
    void store_success_returnsObjectKey() throws Exception {
        byte[] content = "test content".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "sample.pdf", "application/pdf", content);

        String objectKey = storageService.store(file);

        assertThat(objectKey).isNotNull().contains("sample.pdf");
        ArgumentCaptor<PutObjectArgs> captor = ArgumentCaptor.forClass(PutObjectArgs.class);
        verify(minioClient).putObject(captor.capture());
        PutObjectArgs args = captor.getValue();
        assertThat(args.bucket()).isEqualTo(bucket);
        assertThat(args.object()).isEqualTo(objectKey);
        assertThat(args.contentType()).isEqualTo("application/pdf");
    }

    @Test
    @DisplayName("store() wirft StorageException bei MinIO-Fehler")
    void store_failure_throwsStorageException() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "sample.pdf", "application/pdf", "test".getBytes());

        doThrow(new RuntimeException("MinIO connection failed"))
                .when(minioClient)
                .putObject(any(PutObjectArgs.class));

        assertThatThrownBy(() -> storageService.store(file))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("Datei konnte nicht in MinIO gespeichert werden");
    }

    @Test
    @DisplayName("delete() wirft StorageException bei MinIO-Fehler")
    void delete_failure_throwsStorageException() throws Exception {
        doThrow(new RuntimeException("MinIO error")).when(minioClient).removeObject(any(RemoveObjectArgs.class));

        assertThatThrownBy(() -> storageService.delete("key.pdf"))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("Datei konnte nicht aus MinIO gelöscht werden");
    }
}
