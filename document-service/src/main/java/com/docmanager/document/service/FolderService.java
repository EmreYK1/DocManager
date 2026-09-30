package com.docmanager.document.service;

import com.docmanager.document.entity.Folder;

import java.util.List;
import java.util.UUID;

public interface FolderService {

    Folder save(Folder folder);

    Folder findById(UUID id);

    List<Folder> findAll();

    Folder update(UUID id, Folder updated);

    void delete(UUID id);

    List<Folder> findByParent(Folder parent);

    List<Folder> findChildren(UUID parentId);
}
