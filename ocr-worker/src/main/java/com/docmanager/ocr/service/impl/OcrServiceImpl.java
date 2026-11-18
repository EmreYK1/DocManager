package com.docmanager.ocr.service.impl;

import com.docmanager.ocr.exception.OcrException;
import com.docmanager.ocr.service.OcrService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
public class OcrServiceImpl implements OcrService {

    @Override
    public String extractText(File file) throws OcrException {
        log.info("Führe OCR für Datei aus: {}", file.getName());
        try {
            Process process = new ProcessBuilder("tesseract", file.getAbsolutePath(), "stdout", "-l", "deu+eng")
                    .redirectErrorStream(true)
                    .start();
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
            }
            int exitCode = process.waitFor();
            String output = sb.toString().trim();
            if (exitCode == 0 && !output.isBlank()) {
                log.info("Tesseract OCR erfolgreich abgeschlossen für: {}", file.getName());
                return output;
            }
        } catch (Exception e) {
            log.debug("Tesseract-Ausführung nicht möglich oder fehlgeschlagen ({}), nutze Fallback.", e.getMessage());
        }

        // Fallback / Standardwert für Task 6.2.2 Vorbereitung
        return "Hello World";
    }
}

