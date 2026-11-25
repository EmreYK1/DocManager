package com.docmanager.document.exception;

import com.docmanager.document.controller.DocumentController;
import com.docmanager.document.service.DocumentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DocumentController.class)
@Import(GlobalExceptionHandler.class)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DocumentService documentService;

    @Test
    @DisplayName("NotFoundException → HTTP 404 mit status im Body")
    void notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(documentService.findById(id)).thenThrow(new NotFoundException("Dokument nicht gefunden"));

        mockMvc.perform(get("/documents/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.fields").doesNotExist());
    }

    @Test
    @DisplayName("MethodArgumentNotValidException → HTTP 400")
    void validation_returns400() throws Exception {
        String tooLong = "a".repeat(256);

        mockMvc.perform(patch("/documents/{id}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"filename\": \"" + tooLong + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validierungsfehler"))
                .andExpect(jsonPath("$.fields.filename").exists());
    }

    @Test
    @DisplayName("Gleichzeitige Änderung → HTTP 409")
    void concurrentChange_returns409() throws Exception {
        UUID id = UUID.randomUUID();
        when(documentService.findById(id)).thenThrow(new OptimisticLockingFailureException("version"));

        mockMvc.perform(get("/documents/{id}", id))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("StorageException → HTTP 500 ohne Stack-Trace")
    void storage_returns500() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new StorageException("Dateioperation fehlgeschlagen"))
                .when(documentService)
                .delete(id);

        mockMvc.perform(delete("/documents/{id}", id))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("Storage-Fehler: Dateioperation fehlgeschlagen"))
                .andExpect(jsonPath("$.fields").doesNotExist());
    }

    @Test
    @DisplayName("Unbehandelte Exception → HTTP 500 generische Meldung")
    void unexpected_returns500() throws Exception {
        UUID id = UUID.randomUUID();
        when(documentService.findById(id)).thenThrow(new RuntimeException("geheime interne Details"));

        MvcResult result = mockMvc.perform(get("/documents/{id}", id))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("Interner Serverfehler"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.stackTrace").doesNotExist())
                .andReturn();
        assertThat(result.getResponse().getContentAsString())
                .doesNotContain("geheime interne Details")
                .doesNotContain("at com.docmanager");
    }

    @Test
    @DisplayName("Fehler in der Anfrage behalten ihren Status statt als 500 zu enden")
    void clientErrors_keepTheirStatus() throws Exception {
        mockMvc.perform(get("/documents/keine-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
        mockMvc.perform(get("/gibt-es-nicht"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
        mockMvc.perform(put("/documents/{id}", UUID.randomUUID()))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405));
    }

    @Test
    @DisplayName("Zu große Datei → HTTP 413")
    void uploadTooLarge_returns413() throws Exception {
        UUID id = UUID.randomUUID();
        when(documentService.findById(id)).thenThrow(new MaxUploadSizeExceededException(1));

        mockMvc.perform(get("/documents/{id}", id))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.status").value(413));
    }
}
