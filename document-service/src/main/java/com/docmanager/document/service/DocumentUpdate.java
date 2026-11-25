package com.docmanager.document.service;

import java.util.UUID;

public record DocumentUpdate(String filename, UUID folderId, boolean clearFolder) {}
