package com.docmanager.document.dto;

import java.util.UUID;

public record FolderResponse(UUID id, String name, UUID parentId) {}
