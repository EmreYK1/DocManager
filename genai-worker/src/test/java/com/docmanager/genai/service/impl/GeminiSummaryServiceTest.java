package com.docmanager.genai.service.impl;

import com.docmanager.genai.exception.SummaryException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GeminiSummaryServiceTest {

    private static final String URL = "http://gemini.test/v1beta/models/test-model:generateContent";

    private MockRestServiceServer server;
    private RestClient restClient;
    private GeminiSummaryService service;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://gemini.test");
        server = MockRestServiceServer.bindTo(builder).build();
        restClient = builder.build();
        service = new GeminiSummaryService(restClient, "test-key", "test-model", 20);
    }

    @Test
    void sendsPromptAndTextAndReturnsSummary() {
        server.expect(requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-goog-api-key", "test-key"))
                .andExpect(jsonPath("$.systemInstruction.parts[0].text").value(GeminiSummaryService.PROMPT))
                .andExpect(jsonPath("$.systemInstruction.role").doesNotExist())
                .andExpect(jsonPath("$.contents[0].role").value("user"))
                .andExpect(jsonPath("$.contents[0].parts[0].text").value("Beispieltext"))
                .andExpect(jsonPath("$.contents[0].parts[0].thought").doesNotExist())
                .andRespond(withSuccess("""
                        {"candidates": [{"finishReason": "STOP", "index": 0, "content": {"role": "model", "parts": [
                          {"text": "Überlegung", "thought": true},
                          {"text": " Eine kurze Zusammenfassung. ", "thoughtSignature": "abc"}
                        ]}}], "modelVersion": "test-model"}
                        """, MediaType.APPLICATION_JSON));

        assertThat(service.summarize("Beispieltext")).isEqualTo("Eine kurze Zusammenfassung.");
        server.verify();
    }

    @Test
    void truncatesLongTextAtWordBoundary() {
        expectText("eins zwei drei vier");

        service.summarize("eins zwei drei vier fünf sechs");

        server.verify();
    }

    @Test
    void truncatesTextWithoutSpacesWithoutSplittingSurrogatePairs() {
        expectText("a".repeat(19));

        service.summarize("a".repeat(19) + "😀" + "b".repeat(10));

        server.verify();
    }

    @Test
    void failsOnHttpError() {
        server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        assertThatThrownBy(() -> service.summarize("Text"))
                .isInstanceOf(SummaryException.class)
                .hasMessageContaining("429");
    }

    @Test
    void failsWhenGeminiIsUnreachable() {
        server.expect(requestTo(URL)).andRespond(withException(new IOException("timeout")));

        assertThatThrownBy(() -> service.summarize("Text"))
                .isInstanceOf(SummaryException.class)
                .hasMessageContaining("nicht erreichbar");
    }

    @Test
    void failsWhenPromptIsBlocked() {
        respondWith("""
                {"promptFeedback": {"blockReason": "SAFETY"}}""");

        assertThatThrownBy(() -> service.summarize("Text"))
                .isInstanceOf(SummaryException.class)
                .hasMessageContaining("SAFETY");
    }

    @Test
    void failsWhenAnswerIsIncomplete() {
        respondWith("""
                {"candidates": [{"finishReason": "MAX_TOKENS", "content": {"parts": [{"text": "Halb"}]}}]}""");

        assertThatThrownBy(() -> service.summarize("Text"))
                .isInstanceOf(SummaryException.class)
                .hasMessageContaining("MAX_TOKENS");
    }

    @Test
    void failsWhenAnswerIsEmpty() {
        respondWith("""
                {"candidates": [{"finishReason": "STOP", "content": {}}]}""");

        assertThatThrownBy(() -> service.summarize("Text"))
                .isInstanceOf(SummaryException.class)
                .hasMessageContaining("leere Antwort");
    }

    @Test
    void doesNotCallGeminiWithoutKeyOrText() {
        GeminiSummaryService withoutKey = new GeminiSummaryService(restClient, " ", "test-model", 20);

        assertThatThrownBy(() -> withoutKey.summarize("Text")).hasMessageContaining("GEMINI_API_KEY");
        assertThatThrownBy(() -> service.summarize("  ")).isInstanceOf(SummaryException.class);
        server.verify();
    }

    private void expectText(String expected) {
        server.expect(requestTo(URL))
                .andExpect(jsonPath("$.contents[0].parts[0].text").value(expected))
                .andRespond(withSuccess("""
                        {"candidates": [{"finishReason": "STOP", "content": {"parts": [{"text": "ok"}]}}]}""", MediaType.APPLICATION_JSON));
    }

    private void respondWith(String json) {
        server.expect(requestTo(URL)).andRespond(withSuccess(json, MediaType.APPLICATION_JSON));
    }
}
