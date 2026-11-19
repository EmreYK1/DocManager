package com.docmanager.indexing.messaging;

import com.docmanager.indexing.exception.IndexingException;
import com.docmanager.indexing.service.SearchIndex;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class IndexingListenerTest {

    @Mock
    private SearchIndex searchIndex;

    private IndexingListener indexingListener;

    private UUID documentId;
    private String filename;
    private String text;

    @BeforeEach
    void setUp() {
        indexingListener = new IndexingListener(searchIndex);
        documentId = UUID.randomUUID();
        filename = "HelloWorld.pdf";
        text = "Hello World";
    }

    @Test
    @DisplayName("Erfolg wird indexiert: success=true ruft SearchIndex mit Dokument-ID auf")
    void handleOcrResult_success_indexesDocument() throws Exception {
        OcrResultMessage message = new OcrResultMessage(documentId, filename, text, true, null);

        indexingListener.handleOcrResult(message);

        verify(searchIndex).index(eq(documentId), eq(filename), eq(text));
    }

    @Test
    @DisplayName("Fehler wird übersprungen: success=false indexiert nicht")
    void handleOcrResult_failure_skipsIndexing() throws Exception {
        OcrResultMessage message = new OcrResultMessage(
                documentId, filename, null, false, "OCR fehlgeschlagen");

        indexingListener.handleOcrResult(message);

        verify(searchIndex, never()).index(any(), any(), any());
        verifyNoInteractions(searchIndex);
    }

    @Test
    @DisplayName("IndexingException wird geloggt und nicht weitergeworfen")
    void handleOcrResult_indexingException_isCaught() throws Exception {
        OcrResultMessage message = new OcrResultMessage(documentId, filename, text, true, null);
        doThrow(new IndexingException("ES down")).when(searchIndex)
                .index(documentId, filename, text);

        indexingListener.handleOcrResult(message);

        verify(searchIndex).index(documentId, filename, text);
    }

    @Test
    @DisplayName("Null-Nachricht wird ignoriert")
    void handleOcrResult_nullMessage_isIgnored() {
        indexingListener.handleOcrResult(null);
        indexingListener.handleOcrResult(new OcrResultMessage(null, filename, text, true, null));

        verifyNoInteractions(searchIndex);
    }
}
