package com.docmanager.document.service;

import com.docmanager.document.entity.Comment;
import com.docmanager.document.entity.Document;
import com.docmanager.document.repository.CommentRepository;
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
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private DocumentService documentService;

    @InjectMocks
    private CommentService commentService;

    private Document sampleDoc;
    private Comment sampleComment;
    private UUID docId;
    private UUID commentId;

    @BeforeEach
    void setUp() throws Exception {
        docId     = UUID.randomUUID();
        commentId = UUID.randomUUID();

        sampleDoc = new Document("test.pdf", "application/pdf", 1024L, null);
        setField(Document.class, sampleDoc, "id", docId);

        sampleComment = new Comment("Sehr gutes Dokument", "Alice", sampleDoc);
        setField(Comment.class, sampleComment, "id", commentId);
    }

    private <T> void setField(Class<T> clazz, T obj, String fieldName, Object value) throws Exception {
        var field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(obj, value);
    }

    // ─── save ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("save() lädt Document per ID und speichert Comment")
    void save_loadsDocumentAndPersistsComment() {
        when(documentService.findById(docId)).thenReturn(sampleDoc);
        when(commentRepository.save(any(Comment.class))).thenReturn(sampleComment);

        Comment result = commentService.save(docId, "Sehr gutes Dokument", "Alice");

        assertThat(result.getContent()).isEqualTo("Sehr gutes Dokument");
        assertThat(result.getAuthor()).isEqualTo("Alice");
        assertThat(result.getDocument()).isEqualTo(sampleDoc);
        verify(documentService).findById(docId);
        verify(commentRepository).save(any(Comment.class));
    }

    @Test
    @DisplayName("save() wirft RuntimeException wenn Document nicht existiert")
    void save_throwsException_whenDocumentNotFound() {
        UUID unknownId = UUID.randomUUID();
        when(documentService.findById(unknownId)).thenThrow(new RuntimeException("Document not found: " + unknownId));

        assertThatThrownBy(() -> commentService.save(unknownId, "Text", "Bob"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Document not found");

        verify(commentRepository, never()).save(any());
    }

    // ─── findById ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("findById() gibt Comment zurück wenn vorhanden")
    void findById_returnsComment_whenFound() {
        when(commentRepository.findById(commentId)).thenReturn(Optional.of(sampleComment));

        Comment result = commentService.findById(commentId);

        assertThat(result).isEqualTo(sampleComment);
    }

    @Test
    @DisplayName("findById() wirft RuntimeException wenn nicht gefunden")
    void findById_throwsRuntimeException_whenNotFound() {
        UUID unknownId = UUID.randomUUID();
        when(commentRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.findById(unknownId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Comment not found");
    }

    // ─── findByDocument ──────────────────────────────────────────────────────

    @Test
    @DisplayName("findByDocument() gibt alle Comments eines Dokuments zurück")
    void findByDocument_returnsComments() {
        Comment c2 = new Comment("Zweiter Kommentar", "Bob", sampleDoc);
        when(documentService.findById(docId)).thenReturn(sampleDoc);
        when(commentRepository.findByDocument(sampleDoc)).thenReturn(List.of(sampleComment, c2));

        List<Comment> result = commentService.findByDocument(docId);

        assertThat(result).hasSize(2).containsExactlyInAnyOrder(sampleComment, c2);
        verify(commentRepository).findByDocument(sampleDoc);
    }

    @Test
    @DisplayName("findByDocument() gibt leere Liste zurück wenn keine Comments existieren")
    void findByDocument_returnsEmptyList_whenNoComments() {
        when(documentService.findById(docId)).thenReturn(sampleDoc);
        when(commentRepository.findByDocument(sampleDoc)).thenReturn(List.of());

        List<Comment> result = commentService.findByDocument(docId);

        assertThat(result).isEmpty();
    }

    // ─── update ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("update() ändert den Content und speichert")
    void update_updatesContentAndSaves() {
        when(commentRepository.findById(commentId)).thenReturn(Optional.of(sampleComment));
        when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> inv.getArgument(0));

        Comment result = commentService.update(commentId, "Geänderter Inhalt");

        assertThat(result.getContent()).isEqualTo("Geänderter Inhalt");
        verify(commentRepository).save(sampleComment);
    }

    @Test
    @DisplayName("update() wirft RuntimeException wenn Comment nicht existiert")
    void update_throwsException_whenNotFound() {
        UUID unknownId = UUID.randomUUID();
        when(commentRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.update(unknownId, "Text"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Comment not found");

        verify(commentRepository, never()).save(any());
    }

    // ─── delete ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("delete() ruft repository.deleteById() mit korrekter ID auf")
    void delete_callsDeleteById() {
        doNothing().when(commentRepository).deleteById(commentId);

        commentService.delete(commentId);

        verify(commentRepository, times(1)).deleteById(commentId);
    }
}
