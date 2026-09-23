package com.docmanager.document.service;

import com.docmanager.document.entity.Folder;
import com.docmanager.document.repository.FolderRepository;
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
    void setUp() throws Exception {
        rootId  = UUID.randomUUID();
        childId = UUID.randomUUID();

        rootFolder  = new Folder("Root", null);
        childFolder = new Folder("Child", rootFolder);

        setId(rootFolder,  rootId);
        setId(childFolder, childId);
    }

    /** Setzt das private id-Feld per Reflection (JPA-Simulation). */
    private void setId(Folder folder, UUID id) throws Exception {
        var idField = Folder.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(folder, id);
    }

    // ─── save ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("save() persistiert einen Ordner und gibt ihn zurück")
    void save_delegatesToRepository() {
        when(folderRepository.save(rootFolder)).thenReturn(rootFolder);

        Folder result = folderService.save(rootFolder);

        assertThat(result).isEqualTo(rootFolder);
        verify(folderRepository, times(1)).save(rootFolder);
    }

    // ─── findById ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("findById() gibt Ordner zurück wenn er existiert")
    void findById_returnsFolder_whenFound() {
        when(folderRepository.findById(rootId)).thenReturn(Optional.of(rootFolder));

        Folder result = folderService.findById(rootId);

        assertThat(result).isEqualTo(rootFolder);
    }

    @Test
    @DisplayName("findById() wirft RuntimeException wenn Ordner nicht gefunden")
    void findById_throwsRuntimeException_whenNotFound() {
        UUID unknownId = UUID.randomUUID();
        when(folderRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> folderService.findById(unknownId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Folder not found");
    }

    // ─── findAll ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("findAll() gibt alle Ordner zurück")
    void findAll_returnsAllFolders() {
        when(folderRepository.findAll()).thenReturn(List.of(rootFolder, childFolder));

        List<Folder> result = folderService.findAll();

        assertThat(result).hasSize(2).containsExactlyInAnyOrder(rootFolder, childFolder);
    }

    // ─── update ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("update() ändert Name und Parent und speichert")
    void update_updatesNameAndParent() {
        Folder anotherParent = new Folder("AnotherParent", null);
        Folder updated = new Folder("Renamed", anotherParent);

        when(folderRepository.findById(childId)).thenReturn(Optional.of(childFolder));
        when(folderRepository.save(any(Folder.class))).thenAnswer(inv -> inv.getArgument(0));

        Folder result = folderService.update(childId, updated);

        assertThat(result.getName()).isEqualTo("Renamed");
        assertThat(result.getParent()).isEqualTo(anotherParent);
        verify(folderRepository).save(childFolder);
    }

    @Test
    @DisplayName("update() wirft RuntimeException wenn Ordner nicht existiert")
    void update_throwsException_whenFolderNotFound() {
        UUID unknownId = UUID.randomUUID();
        when(folderRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> folderService.update(unknownId, rootFolder))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Folder not found");

        verify(folderRepository, never()).save(any());
    }

    // ─── delete ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("delete() ruft repository.deleteById() mit korrekter ID auf")
    void delete_callsDeleteById() {
        doNothing().when(folderRepository).deleteById(rootId);

        folderService.delete(rootId);

        verify(folderRepository, times(1)).deleteById(rootId);
    }

    // ─── findByParent ────────────────────────────────────────────────────────

    @Test
    @DisplayName("findByParent() gibt alle Unterordner des gegebenen Ordners zurück")
    void findByParent_returnsChildren() {
        Folder child2 = new Folder("Child2", rootFolder);
        when(folderRepository.findByParent(rootFolder)).thenReturn(List.of(childFolder, child2));

        List<Folder> result = folderService.findByParent(rootFolder);

        assertThat(result).hasSize(2).containsExactlyInAnyOrder(childFolder, child2);
        verify(folderRepository).findByParent(rootFolder);
    }

    @Test
    @DisplayName("findByParent() gibt leere Liste zurück wenn kein Kind-Ordner existiert")
    void findByParent_returnsEmptyList_whenNoChildren() {
        Folder lonely = new Folder("Lonely", null);
        when(folderRepository.findByParent(lonely)).thenReturn(List.of());

        List<Folder> result = folderService.findByParent(lonely);

        assertThat(result).isEmpty();
    }
}
