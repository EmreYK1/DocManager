package com.docmanager.document.service;

import com.docmanager.document.entity.Document;
import com.docmanager.document.entity.DocumentStatus;
import com.docmanager.document.repository.DocumentRepository;
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

    @InjectMocks
    private DocumentService documentService;

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

    // ─── save ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("save() ruft repository.save() auf und gibt das Dokument zurück")
    void save_delegatesToRepository() {
        when(documentRepository.save(sampleDoc)).thenReturn(sampleDoc);

        Document result = documentService.save(sampleDoc);

        assertThat(result).isEqualTo(sampleDoc);
        verify(documentRepository, times(1)).save(sampleDoc);
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
    @DisplayName("findById() wirft RuntimeException wenn nicht gefunden")
    void findById_throwsRuntimeException_whenNotFound() {
        UUID unknownId = UUID.randomUUID();
        when(documentRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentService.findById(unknownId))
                .isInstanceOf(RuntimeException.class)
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
        Document updated = new Document("renamed.pdf", "application/pdf", 1024L, null);
        updated.setStatus(DocumentStatus.OCR_DONE);

        when(documentRepository.findById(sampleId)).thenReturn(Optional.of(sampleDoc));
        when(documentRepository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

        Document result = documentService.update(sampleId, updated);

        assertThat(result.getFilename()).isEqualTo("renamed.pdf");
        assertThat(result.getStatus()).isEqualTo(DocumentStatus.OCR_DONE);
        verify(documentRepository).save(sampleDoc);
    }

    @Test
    @DisplayName("update() wirft RuntimeException wenn Dokument nicht existiert")
    void update_throwsException_whenDocumentNotFound() {
        UUID unknownId = UUID.randomUUID();
        when(documentRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentService.update(unknownId, sampleDoc))
                .isInstanceOf(RuntimeException.class)
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
