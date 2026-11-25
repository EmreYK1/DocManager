package com.docmanager.genai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Meldet beim Start, ob der Zugang zu Google Gemini konfiguriert ist. Der Key wird nie geloggt. */
@Component
class GeminiConfigurationCheck implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(GeminiConfigurationCheck.class);

    private final String apiKey;
    private final String model;

    GeminiConfigurationCheck(
            @Value("${app.gemini.api-key}") String apiKey, @Value("${app.gemini.model}") String model) {
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (apiKey.isBlank()) {
            // Kein Abbruch: Sonst endet der Container, und die Ursache geht leicht unter
            log.error("GEMINI_API_KEY ist nicht gesetzt, Google Gemini kann nicht genutzt werden. "
                    + "Den Key in die Datei .env eintragen (Vorlage: .env.example, Anleitung: README) "
                    + "und danach 'docker compose up -d genai-worker' ausführen.");
            return;
        }
        log.info("Gemini ist konfiguriert, Modell: {}", model);
    }
}
