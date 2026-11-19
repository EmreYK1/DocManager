package com.docmanager.ocr.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Echter Test mit installiertem Tesseract und Ghostscript; sonst übersprungen.
 */
@EnabledIf("toolsInstalled")
class TesseractOcrServiceRealTest {

    static boolean toolsInstalled() {
        return available("tesseract", "--version") && (available("gs", "--version") || available("gswin64c", "--version"));
    }

    private static boolean available(String... cmd) {
        try {
            Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
            p.getInputStream().readAllBytes();
            return p.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    @Test
    void recognizesHelloInHelloWorldPdf() throws Exception {
        Path pdf = Files.createTempFile("HelloWorld", ".pdf");
        try (var in = getClass().getResourceAsStream("/HelloWorld.pdf")) {
            Files.copy(in, pdf, StandardCopyOption.REPLACE_EXISTING);
        }
        try {
            String gs = available("gs", "--version") ? "gs" : "gswin64c";
            var service = new TesseractOcrService(new ProcessRunner(), "eng", 120, gs, "tesseract");
            assertThat(service.extractText(pdf)).containsIgnoringCase("Hello");
        } finally {
            Files.deleteIfExists(pdf);
        }
    }
}
