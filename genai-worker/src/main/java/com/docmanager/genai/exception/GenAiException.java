package com.docmanager.genai.exception;

public class GenAiException extends RuntimeException {

    public GenAiException(String message) {
        super(message);
    }

    public GenAiException(String message, Throwable cause) {
        super(message, cause);
    }
}
