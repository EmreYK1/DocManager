package com.docmanager.genai.messaging;

import com.docmanager.genai.exception.GenAiException;
import com.docmanager.genai.service.SummaryService;
import com.docmanager.messaging.SummaryRequestMessage;
import com.docmanager.messaging.SummaryResultMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SummaryListenerTest {

    @Mock
    private SummaryService summaryService;

    @Mock
    private SummaryResultPublisher publisher;

    @InjectMocks
    private SummaryListener listener;

    private final UUID id = UUID.randomUUID();
    private final SummaryRequestMessage request = new SummaryRequestMessage(id, "Text");

    @Test
    void publishesSummary() {
        when(summaryService.summarize("Text")).thenReturn("Kurz");

        listener.handleSummaryRequest(request);

        verify(publisher).publish(SummaryResultMessage.succeeded(id, "Kurz"));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " \n "})
    void reportsEmptyTextWithoutCallingGemini(String text) {
        listener.handleSummaryRequest(new SummaryRequestMessage(id, text));

        verify(publisher).publish(SummaryResultMessage.failed(id, "Kein Text zum Zusammenfassen vorhanden"));
        verifyNoInteractions(summaryService);
    }

    @Test
    void geminiErrorIsAResult() {
        when(summaryService.summarize("Text")).thenThrow(new GenAiException("Status 503"));

        listener.handleSummaryRequest(request);

        verify(publisher).publish(SummaryResultMessage.failed(id, "Zusammenfassung fehlgeschlagen: Status 503"));
    }

    @Test
    void technicalErrorIsRethrown() {
        when(summaryService.summarize("Text")).thenReturn("Kurz");
        doThrow(new AmqpException("RabbitMQ weg")).when(publisher).publish(any());

        assertThatThrownBy(() -> listener.handleSummaryRequest(request)).isInstanceOf(AmqpException.class);

        verify(publisher, times(1)).publish(any());
    }
}
