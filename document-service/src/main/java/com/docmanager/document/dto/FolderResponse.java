package com.docmanager.document.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class FolderResponse {
    private UUID id;
    private String name;
    private UUID parentId;
}
