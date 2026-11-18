package com.docmanager.ocr.service;

import com.docmanager.ocr.exception.OcrException;

import java.io.File;

public interface OcrService {

    /**
     * Führt OCR auf der übergebenen Datei aus und liefert den extrahierten Text zurück.
     *
     * @param file die zu verarbeitende PDF- oder Bilddatei
     * @return erkannter Text
     * @throws OcrException bei Fehlern während der OCR-Verarbeitung
     */
    String extractText(File file) throws OcrException;

    default String process(File file) throws OcrException {
        return extractText(file);
    }
}

