package com.docmanager.document.mapper;

import com.docmanager.document.dto.FolderResponse;
import com.docmanager.document.entity.Folder;

public final class FolderMapper {

    private FolderMapper() {}

    public static FolderResponse toResponse(Folder folder) {
        Folder parent = folder.getParent();
        return new FolderResponse(folder.getId(), folder.getName(), parent != null ? parent.getId() : null);
    }
}
