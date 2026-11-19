package com.docmanager.document.messaging;

import com.docmanager.document.entity.Document;
import com.docmanager.document.entity.DocumentStatus;
import com.docmanager.document.repository.DocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OcrResultListenerTest {

    @Mock
    private DocumentRepository documentRepository;

    @InjectMocks
    private OcrResultListener ocrResultListener;

    private UUID documentId;
    private Document document;

    @BeforeEach
    void setUp() {
        documentId = UUID.randomUUID();
        document = new Document("test.pdf", "application/pdf", 1024L, null);
        try {
            var idField = Document.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(document, documentId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("Erfolgsfall: Setzt ocrText und Status OCR_DONE und speichert das Dokument")
    void handleOcrResult_success_setsOcrTextAndStatusOcrDone() {
        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));

        OcrResultMessage message = new OcrResultMessage(
                documentId,
                "test.pdf",
                "Erkannter Text: Hello World",
                true,
                null
        );

        ocrResultListener.handleOcrResult(message);

        ArgumentCaptor<Document> captor = ArgumentCaptor.forClass(Document.class);
        verify(documentRepository).save(captor.capture());

        Document saved = captor.getValue();
        assertThat(saved.getOcrText()).isEqualTo("Erkannter Text: Hello World");
        assertThat(saved.getStatus()).isEqualTo(DocumentStatus.OCR_DONE);
    }

    @Test
    @DisplayName("Fehlerfall: Setzt Status auf FAILED und speichert das Dokument")
    void handleOcrResult_failure_setsStatusFailed() {
        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));

        OcrResultMessage message = new OcrResultMessage(
                documentId,
                "test.pdf",
                null,
                false,
                "OCR fehlgeschlagen: Datei beschädigt"
        );

        ocrResultListener.handleOcrResult(message);

        ArgumentCaptor<Document> captor = ArgumentCaptor.forClass(Document.class);
        verify(documentRepository).save(captor.capture());

        Document saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(DocumentStatus.FAILED);
        assertThat(saved.getOcrText()).isNull();
    }

    @Test
    @DisplayName("Unbekannte Dokument-ID: Loggt Warnung und verwirft die Nachricht ohne Speichern")
    void handleOcrResult_unknownDocumentId_logsAndDiscardsWithoutSaving() {
        UUID unknownId = UUID.randomUUID();
        when(documentRepository.findById(unknownId)).thenReturn(Optional.empty());

        OcrResultMessage message = new OcrResultMessage(
                unknownId,
                "deleted.pdf",
                "Ein Text",
                true,
                null
        );

        ocrResultListener.handleOcrResult(message);

        verify(documentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Ungültige Nachricht (null oder ohne ID): wird ignoriert")
    void handleOcrResult_nullMessage_isIgnored() {
        ocrResultListener.handleOcrResult(null);
        ocrResultListener.handleOcrResult(new OcrResultMessage(null, "test.pdf", "text", true, null));

        verify(documentRepository, never()).findById(any());
        verify(documentRepository, never()).save(any());
    }
}

