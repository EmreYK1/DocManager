package com.docmanager.genai.service.impl;

import com.docmanager.genai.config.GeminiConfig;
import com.docmanager.genai.service.SummaryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/** Ruft die echte Gemini-API auf, mit den Werten aus application.properties. Läuft nur mit gesetztem Key. */
@EnabledIfEnvironmentVariable(named = "GEMINI_API_KEY", matches = ".*\\S.*")
@SpringBootTest(
        classes = {GeminiConfig.class, GeminiSummaryService.class},
        webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ImportAutoConfiguration(JacksonAutoConfiguration.class)
class GeminiSummaryServiceRealTest {

    @Autowired
    private SummaryService summaryService;

    @Test
    void summarizesSampleText() {
        String summary = summaryService.summarize("""
                The city library will be closed from March 3 to March 17 because the heating system is being
                replaced. Borrowed books do not have to be returned during this time, and all due dates are
                extended automatically. The online catalogue and e-book lending remain available.
                """);

        System.out.println("Zusammenfassung von Gemini: " + summary);
        assertThat(summary).isNotBlank().hasSizeLessThan(800);
    }
}
