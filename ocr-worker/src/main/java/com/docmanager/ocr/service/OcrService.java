package com.docmanager.ocr.service;

import com.docmanager.ocr.exception.OcrException;

import java.nio.file.Path;

public interface OcrService {

    /**
     * Führt OCR auf der übergebenen PDF-Datei aus und liefert den extrahierten Text zurück.
     *
     * @param pdf die zu verarbeitende PDF-Datei
     * @return erkannter Text aller Seiten
     * @throws OcrException bei Fehlern, Exit-Code ungleich 0 oder Timeout
     */
    String extractText(Path pdf) throws OcrException;
}
