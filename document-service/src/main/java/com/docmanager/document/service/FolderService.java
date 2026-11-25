package com.docmanager.document.service;

import com.docmanager.document.entity.Folder;
import com.docmanager.document.exception.NotFoundException;
import com.docmanager.document.repository.FolderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class FolderService {

    private final FolderRepository folderRepository;

    public FolderService(FolderRepository folderRepository) {
        this.folderRepository = folderRepository;
    }

    public Folder create(String name, UUID parentId) {
        return folderRepository.save(new Folder(name, findParent(parentId)));
    }

    @Transactional(readOnly = true)
    public Folder findById(UUID id) {
        return folderRepository.findById(id).orElseThrow(() -> new NotFoundException("Ordner nicht gefunden: " + id));
    }

    @Transactional(readOnly = true)
    public List<Folder> findAll() {
        return folderRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Folder> findChildren(UUID parentId) {
        return folderRepository.findByParent(findById(parentId));
    }

    public Folder update(UUID id, String name, UUID parentId) {
        Folder folder = findById(id);
        folder.moveTo(findParent(parentId));
        folder.rename(name);
        return folder;
    }

    public void delete(UUID id) {
        folderRepository.delete(findById(id));
    }

    private Folder findParent(UUID parentId) {
        return parentId != null ? findById(parentId) : null;
    }
}
