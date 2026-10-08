package com.docmanager.document.service.impl;

import com.docmanager.document.entity.Folder;
import com.docmanager.document.exception.NotFoundException;
import com.docmanager.document.repository.FolderRepository;
import com.docmanager.document.service.FolderService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class FolderServiceImpl implements FolderService {

    private final FolderRepository folderRepository;

    public FolderServiceImpl(FolderRepository folderRepository) {
        this.folderRepository = folderRepository;
    }

    @Override
    public Folder create(String name, UUID parentId) {
        return folderRepository.save(new Folder(name, findParent(parentId)));
    }

    @Override
    public Folder findById(UUID id) {
        return folderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Folder not found: " + id));
    }

    @Override
    public List<Folder> findAll() {
        return folderRepository.findAll();
    }

    @Override
    public Folder update(UUID id, String name, UUID parentId) {
        Folder existing = findById(id);
        existing.setName(name);
        existing.setParent(findParent(parentId));
        return folderRepository.save(existing);
    }

    @Override
    public void delete(UUID id) {
        folderRepository.deleteById(id);
    }

    @Override
    public List<Folder> findByParent(Folder parent) {
        return folderRepository.findByParent(parent);
    }

    @Override
    public List<Folder> findChildren(UUID parentId) {
        Folder parent = findById(parentId);
        return folderRepository.findByParent(parent);
    }

    private Folder findParent(UUID parentId) {
        return parentId != null ? findById(parentId) : null;
    }
}
