package com.docmanager.document.entity;

public enum DocumentStatus {
    UPLOADED,
    OCR_PENDING,
    OCR_DONE,
    SUMMARY_DONE,
    /** Die Zusammenfassung ist gescheitert, der erkannte Text bleibt nutzbar. */
    SUMMARY_FAILED,
    FAILED
}
