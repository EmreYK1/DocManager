package com.docmanager.ocr.messaging;

import com.docmanager.ocr.exception.OcrException;
import com.docmanager.ocr.exception.StorageException;
import com.docmanager.ocr.service.DocumentStore;
import com.docmanager.ocr.service.OcrService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OcrListenerTest {

    private static final String EXCHANGE = "docmanager.exchange";
    private static final String OCR_DONE_ROUTING_KEY = "document.ocr.done";

    @Mock
    private DocumentStore documentStore;

    @Mock
    private OcrService ocrService;

    @Mock
    private RabbitTemplate rabbitTemplate;

    private OcrListener ocrListener;

    private UUID documentId;
    private String objectKey;
    private String filename;

    @BeforeEach
    void setUp() {
        ocrListener = new OcrListener(
                documentStore,
                ocrService,
                rabbitTemplate,
                EXCHANGE,
                OCR_DONE_ROUTING_KEY
        );
        documentId = UUID.randomUUID();
        objectKey = "test-object-key";
        filename = "HelloWorld.pdf";
    }

    @Test
    @DisplayName("Erfolgreicher Ablauf: Lädt PDF, ruft OcrService auf, sendet success=true und löscht temporäre Datei")
    void handleOcrMessage_success_sendsSuccessTrue_andDeletesTempFile() throws Exception {
        byte[] pdfContent = "%PDF-1.4 Hello World".getBytes(StandardCharsets.UTF_8);
        when(documentStore.loadDocument(objectKey)).thenAnswer(inv -> new ByteArrayInputStream(pdfContent));

        AtomicReference<File> tempFileRef = new AtomicReference<>();
        when(ocrService.extractText(any(File.class))).thenAnswer(inv -> {
            File f = inv.getArgument(0);
            tempFileRef.set(f);
            assertThat(f).exists();
            assertThat(f.length()).isEqualTo(pdfContent.length);
            return "Hello World";
        });

        OcrRequestMessage request = new OcrRequestMessage(documentId, objectKey, filename);
        ocrListener.handleOcrMessage(request);

        ArgumentCaptor<OcrResultMessage> captor = ArgumentCaptor.forClass(OcrResultMessage.class);
        verify(rabbitTemplate).convertAndSend(eq(EXCHANGE), eq(OCR_DONE_ROUTING_KEY), captor.capture());

        OcrResultMessage result = captor.getValue();
        assertThat(result.documentId()).isEqualTo(documentId);
        assertThat(result.filename()).isEqualTo(filename);
        assertThat(result.text()).isEqualTo("Hello World");
        assertThat(result.success()).isTrue();
        assertThat(result.errorMessage()).isNull();

        assertThat(tempFileRef.get()).isNotNull();
        assertThat(tempFileRef.get()).doesNotExist();
    }

    @Test
    @DisplayName("Fehlende Datei in MinIO: sendet success=false und temporäre Datei ist danach gelöscht")
    void handleOcrMessage_missingFile_sendsSuccessFalse_andDeletesTempFile() {
        when(documentStore.loadDocument(objectKey))
                .thenThrow(new StorageException("Datei nicht in MinIO gefunden: " + objectKey));

        OcrRequestMessage request = new OcrRequestMessage(documentId, objectKey, filename);
        ocrListener.handleOcrMessage(request);

        ArgumentCaptor<OcrResultMessage> captor = ArgumentCaptor.forClass(OcrResultMessage.class);
        verify(rabbitTemplate).convertAndSend(eq(EXCHANGE), eq(OCR_DONE_ROUTING_KEY), captor.capture());

        OcrResultMessage result = captor.getValue();
        assertThat(result.documentId()).isEqualTo(documentId);
        assertThat(result.filename()).isEqualTo(filename);
        assertThat(result.text()).isNull();
        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).contains("Datei nicht in MinIO gefunden");

        verifyNoInteractions(ocrService);
    }

    @Test
    @DisplayName("OcrException: sendet success=false und temporäre Datei ist danach gelöscht")
    void handleOcrMessage_ocrException_sendsSuccessFalse_andDeletesTempFile() throws Exception {
        byte[] pdfContent = "%PDF-1.4 Test".getBytes(StandardCharsets.UTF_8);
        when(documentStore.loadDocument(objectKey)).thenAnswer(inv -> new ByteArrayInputStream(pdfContent));

        AtomicReference<File> tempFileRef = new AtomicReference<>();
        when(ocrService.extractText(any(File.class))).thenAnswer(inv -> {
            File f = inv.getArgument(0);
            tempFileRef.set(f);
            assertThat(f).exists();
            throw new OcrException("OCR-Erkennung fehlgeschlagen");
        });

        OcrRequestMessage request = new OcrRequestMessage(documentId, objectKey, filename);
        ocrListener.handleOcrMessage(request);

        ArgumentCaptor<OcrResultMessage> captor = ArgumentCaptor.forClass(OcrResultMessage.class);
        verify(rabbitTemplate).convertAndSend(eq(EXCHANGE), eq(OCR_DONE_ROUTING_KEY), captor.capture());

        OcrResultMessage result = captor.getValue();
        assertThat(result.documentId()).isEqualTo(documentId);
        assertThat(result.filename()).isEqualTo(filename);
        assertThat(result.text()).isNull();
        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).contains("OCR fehlgeschlagen");

        assertThat(tempFileRef.get()).isNotNull();
        assertThat(tempFileRef.get()).doesNotExist();
    }

    @Test
    @DisplayName("Fehlender objectKey: sendet sofort success=false ohne DocumentStore oder OcrService aufzurufen")
    void handleOcrMessage_missingObjectKey_sendsSuccessFalse() {
        OcrRequestMessage request = new OcrRequestMessage(documentId, null, filename);
        ocrListener.handleOcrMessage(request);

        ArgumentCaptor<OcrResultMessage> captor = ArgumentCaptor.forClass(OcrResultMessage.class);
        verify(rabbitTemplate).convertAndSend(eq(EXCHANGE), eq(OCR_DONE_ROUTING_KEY), captor.capture());

        OcrResultMessage result = captor.getValue();
        assertThat(result.documentId()).isEqualTo(documentId);
        assertThat(result.filename()).isEqualTo(filename);
        assertThat(result.text()).isNull();
        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).contains("Kein objectKey vorhanden");

        verifyNoInteractions(documentStore, ocrService);
    }

    @Test
    @DisplayName("Leerer objectKey (Blank): sendet sofort success=false")
    void handleOcrMessage_blankObjectKey_sendsSuccessFalse() {
        OcrRequestMessage request = new OcrRequestMessage(documentId, "   ", filename);
        ocrListener.handleOcrMessage(request);

        ArgumentCaptor<OcrResultMessage> captor = ArgumentCaptor.forClass(OcrResultMessage.class);
        verify(rabbitTemplate).convertAndSend(eq(EXCHANGE), eq(OCR_DONE_ROUTING_KEY), captor.capture());

        OcrResultMessage result = captor.getValue();
        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).contains("Kein objectKey vorhanden");

        verifyNoInteractions(documentStore, ocrService);
    }
}

