package com.docmanager.document.dto;

import jakarta.validation.constraints.Size;

import java.util.UUID;

/** PATCH-Semantik: alle Felder optional. Den Status setzt nur die Texterkennung. */
public record DocumentUpdateRequest(@Size(max = 255) String filename, UUID folderId, boolean clearFolder) {}
