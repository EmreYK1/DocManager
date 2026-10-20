package com.docmanager.document.exception;

import com.docmanager.document.controller.DocumentController;
import com.docmanager.document.dto.ErrorResponse;
import com.docmanager.document.service.DocumentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DocumentController.class)
@Import(GlobalExceptionHandler.class)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DocumentService documentService;

    @Test
    @DisplayName("NotFoundException → HTTP 404 mit status im Body")
    void notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(documentService.findById(id))
                .thenThrow(new NotFoundException("Dokument nicht gefunden"));

        MvcResult result = mockMvc.perform(get("/documents/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.stackTrace").doesNotExist())
                .andReturn();

        assertValidErrorBody(result);
    }

    @Test
    @DisplayName("MethodArgumentNotValidException → HTTP 400")
    void validation_returns400() throws Exception {
        String invalidBody = """
                {
                  "filename": "",
                  "contentType": "application/pdf",
                  "sizeBytes": 100
                }
                """;

        MvcResult result = mockMvc.perform(post("/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.stackTrace").doesNotExist())
                .andReturn();

        assertValidErrorBody(result);
    }

    @Test
    @DisplayName("MessagingException → HTTP 500 ohne Stack-Trace")
    void messaging_returns500() throws Exception {
        when(documentService.create(anyString(), anyString(), anyLong(), nullable(UUID.class)))
                .thenThrow(new MessagingException(
                        "Nachricht konnte nicht gesendet werden", null));

        String validBody = """
                {
                  "filename": "test.pdf",
                  "contentType": "application/pdf",
                  "sizeBytes": 100
                }
                """;

        MvcResult result = mockMvc.perform(post("/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value(
                        "Messaging-Fehler: Nachricht konnte nicht gesendet werden"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.stackTrace").doesNotExist())
                .andReturn();

        assertValidErrorBody(result);
        assertThat(result.getResponse().getContentAsString())
                .doesNotContain("at com.docmanager");
    }

    @Test
    @DisplayName("Unbehandelte Exception → HTTP 500 generische Meldung")
    void unexpected_returns500() throws Exception {
        UUID id = UUID.randomUUID();
        when(documentService.findById(id))
                .thenThrow(new RuntimeException("geheime interne Details"));

        MvcResult result = mockMvc.perform(get("/documents/{id}", id))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("Interner Serverfehler"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.stackTrace").doesNotExist())
                .andReturn();

        assertValidErrorBody(result);
        assertThat(result.getResponse().getContentAsString())
                .doesNotContain("geheime interne Details")
                .doesNotContain("at com.docmanager");
    }

    private void assertValidErrorBody(MvcResult result) throws Exception {
        String body = result.getResponse().getContentAsString();
        assertThat(body).doesNotContain("stackTrace");
        assertThat(body).doesNotContain("\tat ");

        ErrorResponse error = objectMapper.readValue(body, ErrorResponse.class);
        assertThat(error.timestamp()).isNotNull();
        // Round-trip: Instant → ISO-8601 String → Instant.parse muss gültig sein
        Instant parsed = Instant.parse(error.timestamp().toString());
        assertThat(parsed).isEqualTo(error.timestamp());
    }
}
