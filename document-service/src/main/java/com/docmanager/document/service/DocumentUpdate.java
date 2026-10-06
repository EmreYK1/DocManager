package com.docmanager.document.service;

import java.util.UUID;
import com.docmanager.document.entity.DocumentStatus;

public record DocumentUpdate(
    String filename,
    DocumentStatus status,
    UUID folderId
) {}