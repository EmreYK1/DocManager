package com.docmanager.document.service;

import com.docmanager.document.entity.Document;
import com.docmanager.document.entity.DocumentStatus;
import com.docmanager.document.entity.Folder;
import com.docmanager.document.exception.InvalidFileException;
import com.docmanager.document.exception.NotFoundException;
import com.docmanager.document.exception.StorageException;
import com.docmanager.document.outbox.Outbox;
import com.docmanager.document.repository.DocumentRepository;
import com.docmanager.messaging.IndexMessage;
import com.docmanager.messaging.MessagingTopology;
import com.docmanager.messaging.OcrRequestMessage;
import com.docmanager.messaging.OcrResultMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private FolderService folderService;

    @Mock
    private StorageService storageService;

    @Mock
    private SearchIndexService searchIndexService;

    @Mock
    private Outbox outbox;

    @Mock
    private TransactionTemplate transaction;

    @Mock
    private ApplicationEventPublisher events;

    @InjectMocks
    private DocumentService documentService;

    private final UUID id = UUID.randomUUID();
    private Document document;

    @BeforeEach
    void setUp() {
        document = document(id, "test.pdf");
    }

    private static Document document(UUID id, String filename) {
        Document document = new Document(filename, "application/pdf", 1L, null, "key");
        ReflectionTestUtils.setField(document, "id", id);
        return document;
    }

    private static MockMultipartFile file(String contentType, String content) {
        return new MockMultipartFile("file", "test.pdf", contentType, content.getBytes());
    }

    private void found() {
        when(documentRepository.findById(id)).thenReturn(Optional.of(document));
    }

    private void runTransactions() {
        when(transaction.execute(any()))
                .thenAnswer(inv -> inv.<TransactionCallback<?>>getArgument(0).doInTransaction(null));
    }

    @Test
    @DisplayName("upload() speichert Datei und Dokument und merkt den OCR-Auftrag vor")
    void upload_storesFileAndDocumentAndQueuesOcr() {
        MockMultipartFile pdf = file("application/pdf", "%PDF-1.4");
        runTransactions();
        when(storageService.store(pdf)).thenReturn("key");
        when(documentRepository.save(any())).thenReturn(document);

        assertThat(documentService.upload(pdf, null)).isSameAs(document);

        verify(outbox).add(MessagingTopology.OCR_REQUEST_KEY, new OcrRequestMessage(id, "key"));
    }

    @Test
    @DisplayName("upload() löscht die Datei wieder, wenn das Speichern scheitert")
    void upload_whenSaveFails_removesFile() {
        MockMultipartFile pdf = file("application/pdf", "%PDF-1.4");
        runTransactions();
        when(storageService.store(pdf)).thenReturn("key");
        when(documentRepository.save(any())).thenThrow(new IllegalStateException("DB"));

        assertThatThrownBy(() -> documentService.upload(pdf, null)).hasMessage("DB");

        verify(storageService).delete("key");
    }

    @Test
    @DisplayName("upload() lehnt leere Dateien, falsche Typen und umbenannte Textdateien ab")
    void upload_rejectsInvalidFiles() {
        assertThatThrownBy(() -> documentService.upload(file("application/pdf", ""), null))
                .hasMessage("Die Datei darf nicht leer sein");
        for (MockMultipartFile invalid : List.of(file("text/plain", "%PDF-1.4"), file("application/pdf", "Text"))) {
            assertThatThrownBy(() -> documentService.upload(invalid, null))
                    .isInstanceOf(InvalidFileException.class)
                    .hasMessage("Nur PDF-Dateien sind erlaubt");
        }
        verifyNoInteractions(storageService, documentRepository, outbox);
    }

    @Test
    void findById_unknownId_throwsNotFound() {
        assertThatThrownBy(() -> documentService.findById(id)).isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("OCR-Erfolg macht das Dokument durchsuchbar, spätere Nachrichten ändern nichts")
    void applyOcrResult_isIdempotent() {
        found();

        documentService.applyOcrResult(OcrResultMessage.succeeded(id, "Hello"));
        documentService.applyOcrResult(OcrResultMessage.succeeded(id, "anderer Text"));
        documentService.applyOcrResult(OcrResultMessage.failed(id, "zu spät"));

        assertThat(document.getStatus()).isEqualTo(DocumentStatus.OCR_DONE);
        assertThat(document.getOcrText()).isEqualTo("Hello");
        verify(outbox, times(1)).add(MessagingTopology.INDEX_KEY, IndexMessage.upsert(id, "test.pdf", "Hello"));
    }

    @Test
    void applyOcrResult_failure_marksDocumentFailed() {
        found();

        documentService.applyOcrResult(OcrResultMessage.failed(id, "kaputt"));

        assertThat(document.getStatus()).isEqualTo(DocumentStatus.FAILED);
        verifyNoInteractions(outbox);
    }

    @Test
    void applyOcrResult_deletedDocument_isDiscarded() {
        documentService.applyOcrResult(OcrResultMessage.succeeded(id, "Hello"));

        verifyNoInteractions(outbox);
    }

    @Test
    @DisplayName("Umbenennen aktualisiert den Index nur bei durchsuchbaren Dokumenten")
    void update_rename_reindexesOnlySearchableDocuments() {
        found();
        documentService.update(id, new DocumentUpdate("pending.pdf", null, false));
        verifyNoInteractions(outbox);

        document.completeOcr("Hello");
        documentService.update(id, new DocumentUpdate("neu.pdf", null, false));

        verify(outbox).add(MessagingTopology.INDEX_KEY, IndexMessage.upsert(id, "neu.pdf", "Hello"));
    }

    @Test
    void update_assignsAndClearsFolder() {
        found();
        UUID folderId = UUID.randomUUID();
        Folder folder = new Folder("Rechnungen", null);
        when(folderService.findById(folderId)).thenReturn(folder);

        assertThat(documentService
                        .update(id, new DocumentUpdate(null, folderId, false))
                        .getFolder())
                .isSameAs(folder);
        assertThat(documentService
                        .update(id, new DocumentUpdate(null, null, true))
                        .getFolder())
                .isNull();
    }

    @Test
    @DisplayName("delete() entfernt das Dokument und merkt Index- und Dateilöschung vor")
    void delete_removesDocumentAndSchedulesCleanup() {
        found();

        documentService.delete(id);

        verify(documentRepository).delete(document);
        verify(outbox).add(MessagingTopology.INDEX_KEY, IndexMessage.delete(id));
        verify(events).publishEvent(new DocumentService.FileDiscarded("key"));
        verifyNoInteractions(storageService); // erst nach dem Commit
    }

    @Test
    void discardFile_storageFailure_isOnlyLogged() {
        doThrow(new StorageException("weg")).when(storageService).delete("key");

        assertThatCode(() -> documentService.discardFile(new DocumentService.FileDiscarded("key")))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("search() behält die Relevanz-Reihenfolge und überspringt gelöschte Dokumente")
    void search_keepsOrderAndSkipsMissing() {
        UUID second = UUID.randomUUID();
        List<UUID> hits = List.of(id, UUID.randomUUID(), second);
        when(searchIndexService.search("Hello")).thenReturn(hits);
        when(documentRepository.findAllById(hits)).thenReturn(List.of(document(second, "b.pdf"), document));

        assertThat(documentService.search("Hello")).extracting(Document::getId).containsExactly(id, second);
    }
}
