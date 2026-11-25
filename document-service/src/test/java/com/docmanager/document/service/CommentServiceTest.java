package com.docmanager.document.service;

import com.docmanager.document.entity.Comment;
import com.docmanager.document.entity.Document;
import com.docmanager.document.exception.NotFoundException;
import com.docmanager.document.repository.CommentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

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
    void setUp() {
        docId = UUID.randomUUID();
        commentId = UUID.randomUUID();

        sampleDoc = new Document("test.pdf", "application/pdf", 1024L, null, null);
        ReflectionTestUtils.setField(sampleDoc, "id", docId);

        sampleComment = new Comment("Sehr gutes Dokument", "Alice", sampleDoc);
        ReflectionTestUtils.setField(sampleComment, "id", commentId);
    }

    // ─── findById ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("findById() wirft NotFoundException wenn nicht gefunden")
    void findById_throwsNotFoundException_whenNotFound() {
        UUID unknownId = UUID.randomUUID();
        when(commentRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.findById(docId, unknownId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Kommentar nicht gefunden");
    }

    // ─── update ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("update() ändert den Content")
    void update_updatesContentAndSaves() {
        when(commentRepository.findById(commentId)).thenReturn(Optional.of(sampleComment));

        Comment result = commentService.update(docId, commentId, "Geänderter Inhalt");

        assertThat(result.getContent()).isEqualTo("Geänderter Inhalt");
    }

    // ─── delete ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("delete() löscht den Comment des Dokuments")
    void delete_deletesComment() {
        when(commentRepository.findById(commentId)).thenReturn(Optional.of(sampleComment));

        commentService.delete(docId, commentId);

        verify(commentRepository).delete(sampleComment);
    }

    @Test
    @DisplayName("Ein Comment ist über ein fremdes Dokument nicht erreichbar")
    void foreignDocument_throwsNotFoundException() {
        UUID otherDocId = UUID.randomUUID();
        when(commentRepository.findById(commentId)).thenReturn(Optional.of(sampleComment));

        assertThatThrownBy(() -> commentService.findById(otherDocId, commentId)).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> commentService.update(otherDocId, commentId, "Text"))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> commentService.delete(otherDocId, commentId)).isInstanceOf(NotFoundException.class);

        verify(commentRepository, never()).save(any());
        verify(commentRepository, never()).delete(any(Comment.class));
    }
}
