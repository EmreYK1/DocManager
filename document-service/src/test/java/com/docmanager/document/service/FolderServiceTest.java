package com.docmanager.document.service;

import com.docmanager.document.entity.Folder;
import com.docmanager.document.exception.InvalidRequestException;
import com.docmanager.document.exception.NotFoundException;
import com.docmanager.document.repository.FolderRepository;
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
class FolderServiceTest {

    @Mock
    private FolderRepository folderRepository;

    @InjectMocks
    private FolderService folderService;

    private Folder rootFolder;
    private Folder childFolder;
    private UUID rootId;
    private UUID childId;

    @BeforeEach
    void setUp() {
        rootId = UUID.randomUUID();
        childId = UUID.randomUUID();

        rootFolder = new Folder("Root", null);
        childFolder = new Folder("Child", rootFolder);

        ReflectionTestUtils.setField(rootFolder, "id", rootId);
        ReflectionTestUtils.setField(childFolder, "id", childId);
    }

    // ─── create ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("create() löst die Parent-ID auf und hängt den Ordner darunter")
    void create_withParent_resolvesParent() {
        when(folderRepository.findById(rootId)).thenReturn(Optional.of(rootFolder));
        when(folderRepository.save(any(Folder.class))).thenAnswer(inv -> inv.getArgument(0));

        Folder result = folderService.create("Child", rootId);

        assertThat(result.getParent()).isEqualTo(rootFolder);
    }

    @Test
    @DisplayName("create() wirft NotFoundException wenn der Parent nicht existiert")
    void create_throwsNotFoundException_whenParentNotFound() {
        UUID unknownId = UUID.randomUUID();
        when(folderRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> folderService.create("Child", unknownId)).isInstanceOf(NotFoundException.class);

        verify(folderRepository, never()).save(any());
    }

    // ─── update ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("update() ändert Name und Parent")
    void update_updatesNameAndParent() {
        UUID anotherParentId = UUID.randomUUID();
        Folder anotherParent = new Folder("AnotherParent", null);

        when(folderRepository.findById(childId)).thenReturn(Optional.of(childFolder));
        when(folderRepository.findById(anotherParentId)).thenReturn(Optional.of(anotherParent));

        Folder result = folderService.update(childId, "Renamed", anotherParentId);

        assertThat(result.getName()).isEqualTo("Renamed");
        assertThat(result.getParent()).isEqualTo(anotherParent);
    }

    @Test
    @DisplayName("update() lehnt den Ordner selbst und einen Unterordner als Elternordner ab")
    void update_rejectsCycle() {
        when(folderRepository.findById(rootId)).thenReturn(Optional.of(rootFolder));
        when(folderRepository.findById(childId)).thenReturn(Optional.of(childFolder));

        assertThatThrownBy(() -> folderService.update(rootId, "Root", rootId))
                .isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> folderService.update(rootId, "Root", childId))
                .isInstanceOf(InvalidRequestException.class);

        assertThat(rootFolder.getParent()).isNull();
        verify(folderRepository, never()).save(any());
    }

    // ─── delete ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("delete() wirft NotFoundException wenn der Ordner nicht existiert")
    void delete_throwsNotFoundException_whenFolderNotFound() {
        UUID unknownId = UUID.randomUUID();
        when(folderRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> folderService.delete(unknownId)).isInstanceOf(NotFoundException.class);
    }
}
