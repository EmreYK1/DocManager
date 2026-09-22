package com.docmanager.document.service;

import com.docmanager.document.entity.Folder;
import com.docmanager.document.repository.FolderRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class FolderService {

    private final FolderRepository folderRepository;

    public FolderService(FolderRepository folderRepository) {
        this.folderRepository = folderRepository;
    }

    public Folder save(Folder folder) {
        return folderRepository.save(folder);
    }

    public Folder findById(UUID id) {
        return folderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Folder not found: " + id));
    }

    public List<Folder> findAll() {
        return folderRepository.findAll();
    }

    public Folder update(UUID id, Folder updated) {
        Folder existing = findById(id);
        existing.setName(updated.getName());
        existing.setParent(updated.getParent());
        return folderRepository.save(existing);
    }

    public void delete(UUID id) {
        folderRepository.deleteById(id);
    }

    public List<Folder> findByParent(Folder parent) {
        return folderRepository.findByParent(parent);
    }
}