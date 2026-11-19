package com.docmanager.document.service;

import com.docmanager.document.entity.Document;
import com.docmanager.document.entity.DocumentStatus;
import com.docmanager.document.entity.Folder;
import com.docmanager.document.exception.InvalidFileException;
import com.docmanager.document.exception.NotFoundException;
import com.docmanager.document.exception.SearchException;
import com.docmanager.document.messaging.OcrRequestMessage;
import com.docmanager.document.repository.DocumentRepository;
import com.docmanager.document.service.impl.DocumentServiceImpl;
import com.docmanager.document.service.DocumentUpdate;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
    private RabbitTemplate rabbitTemplate;

    @Mock
    private StorageService storageService;

    @Mock
    private SearchIndexService searchIndexService;

    @InjectMocks
    private DocumentServiceImpl documentService;

    private Document sampleDoc;
    private UUID sampleId;

    @BeforeEach
    void setUp() {
        sampleId = UUID.randomUUID();
        sampleDoc = new Document("test.pdf", "application/pdf", 1024L, null);
        // id setzen per Reflection (wird normalerweise von JPA gesetzt)
        try {
            var idField = Document.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(sampleDoc, sampleId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ─── create ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("create() erstellt Dokument mit OCR_PENDING Status ohne Folder wenn folderId null und sendet Nachricht")
    void create_withoutFolder_createsDocument() {
        when(documentRepository.save(any())).thenAnswer(inv -> {
            Document d = inv.getArgument(0);
            d.setId(sampleId);
            return d;
        });

        Document result = documentService.create("neu.pdf", "application/pdf", 512L, null);

        assertThat(result.getFilename()).isEqualTo("neu.pdf");
        assertThat(result.getStatus()).isEqualTo(DocumentStatus.OCR_PENDING);
        verify(documentRepository).save(any());
        verify(rabbitTemplate).convertAndSend(any(), any(), eq(new OcrRequestMessage(sampleId, null, "neu.pdf")));
    }

    // ─── upload ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("upload() speichert zuerst in MinIO, dann in DB, dann sendet Nachricht")
    void upload_validPdf_createsDocument() {
        byte[] content = "%PDF-1.4 Hello World".getBytes();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.pdf",
                "application/pdf",
                content
        );

        when(storageService.store(file)).thenReturn("test-object-key");
        when(documentRepository.save(any())).thenAnswer(inv -> {
            Document d = inv.getArgument(0);
            d.setId(sampleId);
            return d;
        });

        Document result = documentService.upload(file, null);

        assertThat(result.getFilename()).isEqualTo("test.pdf");
        assertThat(result.getContentType()).isEqualTo("application/pdf");
        assertThat(result.getSizeBytes()).isEqualTo(content.length);
        assertThat(result.getStatus()).isEqualTo(DocumentStatus.OCR_PENDING);
        assertThat(result.getObjectKey()).isEqualTo("test-object-key");

        var inOrder = inOrder(storageService, documentRepository, rabbitTemplate);
        inOrder.verify(storageService).store(file);
        inOrder.verify(documentRepository).save(any());
        inOrder.verify(rabbitTemplate).convertAndSend(any(), any(), eq(new OcrRequestMessage(sampleId, "test-object-key", "test.pdf")));
    }

    @Test
    @DisplayName("Schlägt das Speichern in der DB fehl, wird storageService.delete aufgerufen")
    void upload_whenDbSaveFails_storageDeleteIsCalled() {
        byte[] content = "%PDF-1.4 Hello World".getBytes();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.pdf",
                "application/pdf",
                content
        );

        when(storageService.store(file)).thenReturn("test-object-key");
        when(documentRepository.save(any())).thenThrow(new RuntimeException("DB error"));

        assertThatThrownBy(() -> documentService.upload(file, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("DB error");

        verify(storageService).store(file);
        verify(storageService).delete("test-object-key");
        verify(rabbitTemplate, never()).convertAndSend(any(), any(), any(OcrRequestMessage.class));
    }

    @Test
    @DisplayName("upload() wirft InvalidFileException wenn Datei null oder leer ist")
    void upload_emptyOrNullFile_throwsInvalidFileException() {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "empty.pdf",
                "application/pdf",
                new byte[0]
        );

        assertThatThrownBy(() -> documentService.upload(null, null))
                .isInstanceOf(InvalidFileException.class)
                .hasMessage("Die Datei darf nicht leer sein");

        assertThatThrownBy(() -> documentService.upload(emptyFile, null))
                .isInstanceOf(InvalidFileException.class)
                .hasMessage("Die Datei darf nicht leer sein");

        verify(documentRepository, never()).save(any());
    }

    @Test
    @DisplayName("upload() wirft InvalidFileException wenn Content-Type kein PDF ist")
    void upload_nonPdf_throwsInvalidFileException() {
        MockMultipartFile txtFile = new MockMultipartFile(
                "file",
                "README.md",
                "text/markdown",
                "readme content".getBytes()
        );

        assertThatThrownBy(() -> documentService.upload(txtFile, null))
                .isInstanceOf(InvalidFileException.class)
                .hasMessage("Nur PDF-Dateien sind erlaubt");

        verify(documentRepository, never()).save(any());
    }

    // ─── findById ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("findById() gibt Dokument zurück wenn vorhanden")
    void findById_returnsDocument_whenFound() {
        when(documentRepository.findById(sampleId)).thenReturn(Optional.of(sampleDoc));

        Document result = documentService.findById(sampleId);

        assertThat(result).isEqualTo(sampleDoc);
        verify(documentRepository).findById(sampleId);
    }

    @Test
    @DisplayName("findById() wirft NotFoundException wenn nicht gefunden")
    void findById_throwsNotFoundException_whenNotFound() {
        UUID unknownId = UUID.randomUUID();
        when(documentRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentService.findById(unknownId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Document not found");

        verify(documentRepository).findById(unknownId);
    }

    // ─── findAll ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("findAll() gibt alle Dokumente aus dem Repository zurück")
    void findAll_returnsAllDocuments() {
        Document doc2 = new Document("report.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", 2048L, null);
        when(documentRepository.findAll()).thenReturn(List.of(sampleDoc, doc2));

        List<Document> result = documentService.findAll();

        assertThat(result).hasSize(2).contains(sampleDoc, doc2);
        verify(documentRepository).findAll();
    }

    @Test
    @DisplayName("findAll() gibt leere Liste zurück wenn keine Dokumente existieren")
    void findAll_returnsEmptyList_whenNoDocuments() {
        when(documentRepository.findAll()).thenReturn(List.of());

        List<Document> result = documentService.findAll();

        assertThat(result).isEmpty();
    }

    // ─── update ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("update() aktualisiert Filename und Status und speichert")
    void update_updatesFieldsAndSaves() {
        DocumentUpdate update = new DocumentUpdate("renamed.pdf", DocumentStatus.OCR_DONE, null, false);

        when(documentRepository.findById(sampleId)).thenReturn(Optional.of(sampleDoc));
        when(documentRepository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

        Document result = documentService.update(sampleId, update);

        assertThat(result.getFilename()).isEqualTo("renamed.pdf");
        assertThat(result.getStatus()).isEqualTo(DocumentStatus.OCR_DONE);
        verify(documentRepository).save(sampleDoc);
    }

    @Test
    @DisplayName("Nur Filename ändern -> Status bleibt gleich")
    void update_onlyFilename_statusRemainsSame() {
        DocumentUpdate update = new DocumentUpdate("neu.pdf", null, null, false);

        when(documentRepository.findById(sampleId)).thenReturn(Optional.of(sampleDoc));
        when(documentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Document result = documentService.update(sampleId, update);

        assertThat(result.getFilename()).isEqualTo("neu.pdf");
        assertThat(result.getStatus()).isEqualTo(DocumentStatus.OCR_PENDING);
    }

    @Test
    @DisplayName("update() ändert nur Status, Filename bleibt gleich")
    void update_onlyStatus_filenameUnchanged() {
        DocumentUpdate update = new DocumentUpdate(null, DocumentStatus.OCR_DONE, null, false);

        when(documentRepository.findById(sampleId)).thenReturn(Optional.of(sampleDoc));
        when(documentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Document result = documentService.update(sampleId, update);

        assertThat(result.getStatus()).isEqualTo(DocumentStatus.OCR_DONE);
        assertThat(result.getFilename()).isEqualTo("test.pdf");
    }

    @Test
    @DisplayName("update() setzt neuen Folder wenn folderId angegeben")
    void update_withFolderId_assignsFolder() {
        UUID folderId = UUID.randomUUID();
        Folder folder = new Folder("Rechnungen", null);
        DocumentUpdate update = new DocumentUpdate(null, null, folderId, false);

        when(documentRepository.findById(sampleId)).thenReturn(Optional.of(sampleDoc));
        when(folderService.findById(folderId)).thenReturn(folder);
        when(documentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Document result = documentService.update(sampleId, update);

        assertThat(result.getFolder()).isEqualTo(folder);
    }

    @Test
    @DisplayName("update() entfernt Folder wenn clearFolder gesetzt ist")
    void update_withClearFolder_removesFolder() {
        sampleDoc.setFolder(new Folder("Rechnungen", null));
        DocumentUpdate update = new DocumentUpdate(null, null, null, true);

        when(documentRepository.findById(sampleId)).thenReturn(Optional.of(sampleDoc));
        when(documentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Document result = documentService.update(sampleId, update);

        assertThat(result.getFolder()).isNull();
        verify(folderService, never()).findById(any());
    }

    @Test
    @DisplayName("update() wirft NotFoundException wenn Dokument nicht existiert")
    void update_throwsNotFoundException_whenDocumentNotFound() {
        UUID unknownId = UUID.randomUUID();
        when(documentRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentService.update(unknownId, new DocumentUpdate(null, null, null, false)))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Document not found");

        verify(documentRepository, never()).save(any());
    }

    // ─── delete ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("delete() löscht Datei aus Storage und Dokument aus DB wenn objectKey vorhanden")
    void delete_withObjectKey_deletesFromStorageAndDb() {
        sampleDoc.setObjectKey("minio-file-key");
        when(documentRepository.findById(sampleId)).thenReturn(Optional.of(sampleDoc));

        documentService.delete(sampleId);

        verify(storageService).delete("minio-file-key");
        verify(documentRepository, times(1)).deleteById(sampleId);
    }

    @Test
    @DisplayName("delete() löscht nur aus DB wenn kein objectKey vorhanden")
    void delete_withoutObjectKey_onlyDeletesFromDb() {
        sampleDoc.setObjectKey(null);
        when(documentRepository.findById(sampleId)).thenReturn(Optional.of(sampleDoc));

        documentService.delete(sampleId);

        verify(storageService, never()).delete(any());
        verify(documentRepository, times(1)).deleteById(sampleId);
    }

    @Test
    @DisplayName("delete() entfernt den Index-Eintrag")
    void delete_removesIndexEntry() {
        when(documentRepository.findById(sampleId)).thenReturn(Optional.of(sampleDoc));

        documentService.delete(sampleId);

        verify(searchIndexService).delete(sampleId);
    }

    @Test
    @DisplayName("delete() scheitert nicht, wenn das Löschen im Index fehlschlägt")
    void delete_indexFailure_isIgnored() {
        when(documentRepository.findById(sampleId)).thenReturn(Optional.of(sampleDoc));
        doThrow(new SearchException("ES down", new RuntimeException())).when(searchIndexService).delete(sampleId);

        assertThatCode(() -> documentService.delete(sampleId)).doesNotThrowAnyException();

        verify(documentRepository).deleteById(sampleId);
    }

    // ─── search ──────────────────────────────────────────────────────────────

    private Document docWithId(UUID id, String filename) {
        Document d = new Document(filename, "application/pdf", 1L, null);
        d.setId(id);
        return d;
    }

    @Test
    @DisplayName("search() liefert Dokumente in der Relevanz-Reihenfolge des Index")
    void search_keepsRelevanceOrder() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        when(searchIndexService.search("Hello")).thenReturn(List.of(first, second));
        // Datenbank liefert in anderer Reihenfolge
        when(documentRepository.findAllById(List.of(first, second)))
                .thenReturn(List.of(docWithId(second, "b.pdf"), docWithId(first, "a.pdf")));

        List<Document> result = documentService.search("Hello");

        assertThat(result).extracting(Document::getFilename).containsExactly("a.pdf", "b.pdf");
    }

    @Test
    @DisplayName("search() überspringt IDs, die nicht mehr in der DB existieren")
    void search_skipsIdsMissingInDb() {
        UUID existing = UUID.randomUUID();
        UUID deleted = UUID.randomUUID();
        when(searchIndexService.search("Hello")).thenReturn(List.of(deleted, existing));
        when(documentRepository.findAllById(List.of(deleted, existing)))
                .thenReturn(List.of(docWithId(existing, "a.pdf")));

        List<Document> result = documentService.search("Hello");

        assertThat(result).extracting(Document::getId).containsExactly(existing);
    }

    @Test
    @DisplayName("search() ohne Treffer liefert leere Liste und fragt die DB nicht")
    void search_noHits_returnsEmptyList() {
        when(searchIndexService.search("xyz")).thenReturn(List.of());

        assertThat(documentService.search("xyz")).isEmpty();

        verifyNoInteractions(documentRepository);
    }
}
