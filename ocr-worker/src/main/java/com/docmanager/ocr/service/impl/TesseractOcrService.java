package com.docmanager.ocr.service.impl;

import com.docmanager.ocr.exception.OcrException;
import com.docmanager.ocr.service.OcrService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * OCR: Ghostscript wandelt das PDF in PNG-Seiten, Tesseract erkennt den Text je Seite.
 */
@Slf4j
@Service
public class TesseractOcrService implements OcrService {

    private final ProcessRunner processRunner;
    private final String language;
    private final Duration timeout;
    private final String ghostscriptCommand;
    private final String tesseractCommand;

    public TesseractOcrService(ProcessRunner processRunner,
                               @Value("${app.ocr.language:deu+eng}") String language,
                               @Value("${app.ocr.timeout-seconds:120}") long timeoutSeconds,
                               @Value("${app.ocr.ghostscript-command:gs}") String ghostscriptCommand,
                               @Value("${app.ocr.tesseract-command:tesseract}") String tesseractCommand) {
        this.processRunner = processRunner;
        this.language = language;
        this.timeout = Duration.ofSeconds(timeoutSeconds);
        this.ghostscriptCommand = ghostscriptCommand;
        this.tesseractCommand = tesseractCommand;
    }

    @Override
    public String extractText(Path pdf) throws OcrException {
        Path tempDir = null;
        try {
            tempDir = Files.createTempDirectory("ocr-pages-");

            runChecked("Ghostscript", List.of(
                    ghostscriptCommand, "-dNOPAUSE", "-dBATCH", "-dSAFER",
                    "-sDEVICE=png16m", "-r300",
                    "-sOutputFile=" + tempDir.resolve("page-%03d.png"),
                    pdf.toAbsolutePath().toString()));

            List<Path> pages;
            try (Stream<Path> files = Files.list(tempDir)) {
                pages = files.filter(p -> p.getFileName().toString().endsWith(".png")).sorted().toList();
            }
            if (pages.isEmpty()) {
                throw new OcrException("Ghostscript hat keine Seiten erzeugt");
            }

            List<String> texts = new ArrayList<>();
            for (Path page : pages) {
                ProcessRunner.Result result = runChecked("Tesseract", List.of(
                        tesseractCommand, page.toAbsolutePath().toString(), "stdout", "-l", language));
                texts.add(result.stdout().trim());
            }
            String text = String.join("\n", texts).trim();
            log.info("OCR abgeschlossen: {} Seite(n), {} Zeichen", pages.size(), text.length());
            return text;
        } catch (IOException e) {
            throw new OcrException("OCR konnte nicht durchgeführt werden: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new OcrException("OCR wurde unterbrochen", e);
        } finally {
            deleteRecursively(tempDir);
        }
    }

    private ProcessRunner.Result runChecked(String tool, List<String> command)
            throws IOException, InterruptedException, OcrException {
        ProcessRunner.Result result = processRunner.run(command, timeout);
        if (result.timedOut()) {
            throw new OcrException(tool + " hat das Timeout von " + timeout.toSeconds() + " s überschritten");
        }
        if (result.exitCode() != 0) {
            throw new OcrException(tool + " fehlgeschlagen (Exit-Code " + result.exitCode() + "): "
                    + result.stderr().trim());
        }
        return result;
    }

    private void deleteRecursively(Path dir) {
        if (dir == null) {
            return;
        }
        try (Stream<Path> paths = Files.walk(dir)) {
            paths.sorted((a, b) -> b.compareTo(a)).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException e) {
                    log.warn("Konnte temporäre Datei nicht löschen: {}", p);
                }
            });
        } catch (IOException e) {
            log.warn("Konnte temporäres Verzeichnis nicht löschen: {}", dir);
        }
    }
}
