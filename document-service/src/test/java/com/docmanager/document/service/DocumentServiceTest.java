package com.docmanager.document.service;

import com.docmanager.document.entity.Document;
import com.docmanager.document.entity.DocumentStatus;
import com.docmanager.document.exception.NotFoundException;
import com.docmanager.document.repository.DocumentRepository;
import com.docmanager.document.service.impl.DocumentServiceImpl;
import com.docmanager.document.service.DocumentUpdate;
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
    @DisplayName("create() erstellt Dokument mit UPLOADED Status ohne Folder wenn folderId null")
    void create_withoutFolder_createsDocument() {
        when(documentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Document result = documentService.create("neu.pdf", "application/pdf", 512L, null);

        assertThat(result.getFilename()).isEqualTo("neu.pdf");
        assertThat(result.getStatus()).isEqualTo(DocumentStatus.UPLOADED);
        verify(documentRepository).save(any());
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
        DocumentUpdate update = new DocumentUpdate("renamed.pdf", DocumentStatus.OCR_DONE, null);

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
        DocumentUpdate update = new DocumentUpdate("neu.pdf", null, null);

        when(documentRepository.findById(sampleId)).thenReturn(Optional.of(sampleDoc));
        when(documentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Document result = documentService.update(sampleId, update);

        assertThat(result.getFilename()).isEqualTo("neu.pdf");
        assertThat(result.getStatus()).isEqualTo(DocumentStatus.UPLOADED);
    }

    @Test
    @DisplayName("update() ändert nur Status, Filename bleibt gleich")
    void update_onlyStatus_filenameUnchanged() {
        DocumentUpdate update = new DocumentUpdate(null, DocumentStatus.OCR_DONE, null);

        when(documentRepository.findById(sampleId)).thenReturn(Optional.of(sampleDoc));
        when(documentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Document result = documentService.update(sampleId, update);

        assertThat(result.getStatus()).isEqualTo(DocumentStatus.OCR_DONE);
        assertThat(result.getFilename()).isEqualTo("test.pdf");
    }

    @Test
    @DisplayName("update() wirft NotFoundException wenn Dokument nicht existiert")
    void update_throwsNotFoundException_whenDocumentNotFound() {
        UUID unknownId = UUID.randomUUID();
        when(documentRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentService.update(unknownId, new DocumentUpdate(null, null, null)))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Document not found");

        verify(documentRepository, never()).save(any());
    }

    // ─── delete ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("delete() ruft repository.deleteById() mit korrekter ID auf")
    void delete_callsDeleteById() {
        doNothing().when(documentRepository).deleteById(sampleId);

        documentService.delete(sampleId);

        verify(documentRepository, times(1)).deleteById(sampleId);
    }
}
