package com.docmanager.document.mapper;

import com.docmanager.document.dto.FolderResponse;
import com.docmanager.document.entity.Folder;

public class FolderMapper {

    public static FolderResponse toResponse(Folder folder) {
        FolderResponse dto = new FolderResponse();
        dto.setId(folder.getId());
        dto.setName(folder.getName());
        if (folder.getParent() != null) {
            dto.setParentId(folder.getParent().getId());
        }
        return dto;
    }
}
