package com.docmanager.document.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        int status,
        String message,
        Instant timestamp,
        Map<String, String> fields
) {

    public ErrorResponse(int status, String message) {
        this(status, message, Instant.now(), null);
    }

    public ErrorResponse(int status, String message, Map<String, String> fields) {
        this(status, message, Instant.now(), fields);
    }
}
