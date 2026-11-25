package com.docmanager.genai.service;

public interface SummaryService {

    /**
     * Fasst einen Text in wenigen deutschen Sätzen zusammen.
     *
     * @throws com.docmanager.genai.exception.GenAiException wenn keine Zusammenfassung erstellt werden konnte
     */
    String summarize(String text);
}
