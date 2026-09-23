package com.docmanager.document.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class FolderRequest {
    private String name;
    private UUID parentId;
}
