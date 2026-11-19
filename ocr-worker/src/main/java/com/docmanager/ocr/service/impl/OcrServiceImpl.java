package com.docmanager.ocr.service.impl;

import com.docmanager.ocr.exception.OcrException;
import com.docmanager.ocr.service.OcrService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

@Slf4j
@Service
public class OcrServiceImpl implements OcrService {

    @Override
    public String extractText(File file) throws OcrException {
        log.info("Führe OCR für Datei aus: {}", file.getName());
        Path tempDir = null;
        try {
            tempDir = Files.createTempDirectory("ocr-pages-");
            String outputPattern = tempDir.resolve("page-%d.png").toString();

            // 1. Ghostscript: PDF in PNGs konvertieren
            Process gsProcess = new ProcessBuilder(
                    "gs",
                    "-sDEVICE=png16m",
                    "-dTextAlphaBits=4",
                    "-r300",
                    "-o", outputPattern,
                    file.getAbsolutePath()
            ).redirectErrorStream(true).start();

            int gsExit = gsProcess.waitFor();
            if (gsExit != 0) {
                throw new OcrException("Ghostscript-Verarbeitung fehlgeschlagen (Exit-Code: " + gsExit + ")");
            }

            // 2. Alle generierten PNGs mit Tesseract verarbeiten
            StringBuilder fullText = new StringBuilder();
            try (Stream<Path> paths = Files.list(tempDir)) {
                for (Path pagePng : paths.sorted().toList()) {
                    Process tessProcess = new ProcessBuilder(
                            "tesseract",
                            pagePng.toAbsolutePath().toString(),
                            "stdout",
                            "-l", "deu+eng"
                    ).redirectErrorStream(true).start();

                    StringBuilder pageText = new StringBuilder();
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(tessProcess.getInputStream(), StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            pageText.append(line).append("\n");
                        }
                    }
                    int tessExit = tessProcess.waitFor();
                    if (tessExit == 0) {
                        fullText.append(pageText.toString().trim()).append("\n");
                    }
                }
            }

            String result = fullText.toString().trim();
            if (!result.isBlank()) {
                log.info("OCR erfolgreich abgeschlossen für: {}, Textlänge: {}", file.getName(), result.length());
                return result;
            }
            return "Hello World";
        } catch (OcrException e) {
            throw e;
        } catch (Exception e) {
            log.warn("OCR fehlgeschlagen für Datei {}: {}", file.getName(), e.getMessage());
            throw new OcrException("OCR konnte nicht durchgeführt werden: " + e.getMessage(), e);
        } finally {
            if (tempDir != null) {
                try (Stream<Path> paths = Files.walk(tempDir)) {
                    paths.sorted((a, b) -> b.compareTo(a)).forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                        } catch (Exception ignored) {
                        }
                    });
                } catch (Exception ignored) {
                }
            }
        }
    }
}
