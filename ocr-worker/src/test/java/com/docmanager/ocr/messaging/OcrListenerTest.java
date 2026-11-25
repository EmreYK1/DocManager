package com.docmanager.ocr.messaging;

import com.docmanager.messaging.OcrRequestMessage;
import com.docmanager.messaging.OcrResultMessage;
import com.docmanager.ocr.exception.OcrException;
import com.docmanager.ocr.exception.StorageException;
import com.docmanager.ocr.service.DocumentStore;
import com.docmanager.ocr.service.OcrService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OcrListenerTest {

    @Mock
    private DocumentStore documentStore;

    @Mock
    private OcrService ocrService;

    @Mock
    private OcrResultPublisher publisher;

    @Mock
    private MessageRecoverer deadLetter;

    @InjectMocks
    private OcrListener listener;

    private final UUID id = UUID.randomUUID();
    private final OcrRequestMessage request = new OcrRequestMessage(id, "key");

    @Test
    @DisplayName("Erfolg wird gemeldet und die temporäre Datei gelöscht")
    void success_publishesText_andDeletesTempFile() throws Exception {
        when(documentStore.loadDocument("key")).thenReturn(new ByteArrayInputStream("%PDF-".getBytes()));
        AtomicReference<Path> tempFile = new AtomicReference<>();
        when(ocrService.extractText(any())).thenAnswer(inv -> {
            tempFile.set(inv.getArgument(0));
            assertThat(tempFile.get()).hasContent("%PDF-");
            return "Hello";
        });

        listener.handleOcrMessage(request);

        verify(publisher).publish(OcrResultMessage.succeeded(id, "Hello"));
        assertThat(Files.exists(tempFile.get())).isFalse();
    }

    @Test
    @DisplayName("Ein OCR-Fehler ist ein Ergebnis und wird nicht wiederholt")
    void ocrError_publishesFailure() throws Exception {
        when(documentStore.loadDocument("key")).thenReturn(new ByteArrayInputStream("%PDF-".getBytes()));
        when(ocrService.extractText(any())).thenThrow(new OcrException("kaputt"));

        listener.handleOcrMessage(request);

        verify(publisher).publish(OcrResultMessage.failed(id, "OCR fehlgeschlagen: kaputt"));
    }

    @Test
    @DisplayName("Speicherfehler werden weitergeworfen, damit der Auftrag wiederholt wird")
    void storageError_isRethrown() {
        when(documentStore.loadDocument("key")).thenThrow(new StorageException("MinIO weg"));

        assertThatThrownBy(() -> listener.handleOcrMessage(request)).isInstanceOf(StorageException.class);

        verifyNoInteractions(ocrService, publisher);
    }

    @Test
    @DisplayName("Nach allen Wiederholungen: Dead-Letter-Queue und Dokument als fehlgeschlagen melden")
    void recoverer_deadLettersAndReportsFailure() {
        var converter = new Jackson2JsonMessageConverter();
        Message message = converter.toMessage(request, new MessageProperties());
        var cause = new StorageException("MinIO weg");

        new FailedOcrRecoverer(deadLetter, converter, publisher).recover(message, cause);

        verify(deadLetter).recover(message, cause);
        verify(publisher).publish(OcrResultMessage.failed(id, "Verarbeitung gescheitert"));
    }
}
