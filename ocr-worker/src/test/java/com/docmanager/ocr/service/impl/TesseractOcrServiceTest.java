package com.docmanager.ocr.service.impl;

import com.docmanager.ocr.exception.OcrException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TesseractOcrServiceTest {

    private static final Path PDF = Path.of("input.pdf");

    @Mock
    private ProcessRunner runner;

    private TesseractOcrService service;
    private final AtomicReference<Path> tempDir = new AtomicReference<>();

    @BeforeEach
    void setUp() {
        service = new TesseractOcrService(runner, "deu+eng", 5, "gs", "tesseract");
    }

    /** Simuliert Ghostscript: legt zwei Seitenbilder im Zielverzeichnis an. */
    private ProcessRunner.Result fakeGhostscript(List<String> cmd) throws Exception {
        String outArg = cmd.stream()
                .filter(a -> a.startsWith("-sOutputFile="))
                .findFirst()
                .orElseThrow();
        Path dir = Path.of(outArg.substring("-sOutputFile=".length())).getParent();
        tempDir.set(dir);
        Files.writeString(dir.resolve("page-001.png"), "x");
        Files.writeString(dir.resolve("page-002.png"), "x");
        return new ProcessRunner.Result(0, "", "", false);
    }

    @Test
    void joinsTextOfAllPages_andDeletesTempFiles() throws Exception {
        when(runner.run(any(), any(Duration.class))).thenAnswer(inv -> {
            List<String> cmd = inv.getArgument(0);
            if (cmd.get(0).equals("gs")) {
                return fakeGhostscript(cmd);
            }
            String page = Path.of(cmd.get(1)).getFileName().toString();
            return new ProcessRunner.Result(0, page.equals("page-001.png") ? "Seite eins\n" : "Page two\n", "", false);
        });

        String text = service.extractText(PDF);

        assertThat(text).isEqualTo("Seite eins\nPage two");
        assertThat(tempDir.get()).doesNotExist();
    }

    @Test
    void tesseractExitCode1_throwsOcrException_andDeletesTempFiles() throws Exception {
        when(runner.run(any(), any(Duration.class))).thenAnswer(inv -> {
            List<String> cmd = inv.getArgument(0);
            return cmd.get(0).equals("gs") ? fakeGhostscript(cmd) : new ProcessRunner.Result(1, "", "boom", false);
        });

        assertThatThrownBy(() -> service.extractText(PDF))
                .isInstanceOf(OcrException.class)
                .hasMessageContaining("Exit-Code 1");
        assertThat(tempDir.get()).doesNotExist();
    }

    @Test
    void timeout_throwsOcrException_andDeletesTempFiles() throws Exception {
        when(runner.run(any(), any(Duration.class))).thenAnswer(inv -> {
            List<String> cmd = inv.getArgument(0);
            return cmd.get(0).equals("gs") ? fakeGhostscript(cmd) : new ProcessRunner.Result(-1, "", "", true);
        });

        assertThatThrownBy(() -> service.extractText(PDF))
                .isInstanceOf(OcrException.class)
                .hasMessageContaining("Timeout");
        assertThat(tempDir.get()).doesNotExist();
    }
}
