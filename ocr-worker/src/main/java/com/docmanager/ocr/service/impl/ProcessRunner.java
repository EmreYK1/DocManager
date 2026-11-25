package com.docmanager.ocr.service.impl;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Startet externe Prozesse. Eigene Klasse, damit Tests sie mocken können.
 */
@Component
public class ProcessRunner {

    /**
     * Ergebnis eines Prozessaufrufs.
     *
     * @param exitCode Exit-Code (bei Timeout -1)
     * @param stdout   Standardausgabe
     * @param stderr   Fehlerausgabe
     * @param timedOut true, wenn der Prozess wegen Timeout abgebrochen wurde
     */
    public record Result(int exitCode, String stdout, String stderr, boolean timedOut) {}

    public Result run(List<String> command, Duration timeout) throws IOException, InterruptedException {
        Process process = new ProcessBuilder(command).start();
        process.getOutputStream().close();

        CompletableFuture<String> out = CompletableFuture.supplyAsync(() -> read(process.getInputStream()));
        CompletableFuture<String> err = CompletableFuture.supplyAsync(() -> read(process.getErrorStream()));

        try {
            boolean finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                process.waitFor();
                return new Result(-1, out.join(), err.join(), true);
            }
            return new Result(process.exitValue(), out.join(), err.join(), false);
        } catch (InterruptedException e) {
            process.destroyForcibly();
            throw e;
        }
    }

    private static String read(InputStream in) {
        try (in) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "";
        }
    }
}
