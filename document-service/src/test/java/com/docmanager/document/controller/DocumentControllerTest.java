package com.docmanager.document.controller;

import com.docmanager.document.entity.Document;
import com.docmanager.document.entity.Folder;
import com.docmanager.document.exception.GlobalExceptionHandler;
import com.docmanager.document.exception.SearchException;
import com.docmanager.document.service.DocumentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DocumentController.class)
@Import(GlobalExceptionHandler.class)
class DocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DocumentService documentService;

    @Test
    @DisplayName("Gültiges PDF → HTTP 201 Created mit Dokument-Metadaten")
    void upload_validPdf_returns201Created() throws Exception {
        byte[] content = "%PDF-1.4 Hello World".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "HelloWorld.pdf",
                "application/pdf",
                content
        );

        UUID docId = UUID.randomUUID();
        Document doc = new Document("HelloWorld.pdf", "application/pdf", content.length, null);
        doc.setId(docId);

        when(documentService.upload(any(MultipartFile.class), nullable(UUID.class)))
                .thenReturn(doc);

        mockMvc.perform(multipart("/documents/upload").file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(docId.toString()))
                .andExpect(jsonPath("$.filename").value("HelloWorld.pdf"))
                .andExpect(jsonPath("$.contentType").value("application/pdf"))
                .andExpect(jsonPath("$.sizeBytes").value(content.length))
                .andExpect(jsonPath("$.status").value("OCR_PENDING"));

        verify(documentService).upload(any(MultipartFile.class), isNull());
    }

    @Test
    @DisplayName("Gültiges PDF mit folderId → HTTP 201 Created mit folderId")
    void upload_validPdfWithFolderId_returns201Created() throws Exception {
        byte[] content = "%PDF-1.4 Hello World".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "HelloWorld.pdf",
                "application/pdf",
                content
        );

        UUID folderId = UUID.randomUUID();
        Folder folder = new Folder("TestFolder", null);
        folder.setId(folderId);

        UUID docId = UUID.randomUUID();
        Document doc = new Document("HelloWorld.pdf", "application/pdf", content.length, folder);
        doc.setId(docId);

        when(documentService.upload(any(MultipartFile.class), eq(folderId)))
                .thenReturn(doc);

        mockMvc.perform(multipart("/documents/upload")
                        .file(file)
                        .param("folderId", folderId.toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(docId.toString()))
                .andExpect(jsonPath("$.filename").value("HelloWorld.pdf"))
                .andExpect(jsonPath("$.folderId").value(folderId.toString()))
                .andExpect(jsonPath("$.status").value("OCR_PENDING"));

        verify(documentService).upload(any(MultipartFile.class), eq(folderId));
    }

    @Test
    @DisplayName("Nicht-PDF (z. B. README.md) → HTTP 400 Bad Request mit deutscher Meldung")
    void upload_nonPdf_returns400BadRequest() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "README.md",
                "text/markdown",
                "# DocManager".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/documents/upload").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Nur PDF-Dateien sind erlaubt"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.stackTrace").doesNotExist());

        verify(documentService, never()).upload(any(), any());
    }

    @Test
    @DisplayName("Leere Datei → HTTP 400 Bad Request")
    void upload_emptyFile_returns400BadRequest() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "empty.pdf",
                "application/pdf",
                new byte[0]
        );

        mockMvc.perform(multipart("/documents/upload").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Die Datei darf nicht leer sein"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.stackTrace").doesNotExist());

        verify(documentService, never()).upload(any(), any());
    }

    @Test
    @DisplayName("Fehlendes Feld file → HTTP 400 Bad Request")
    void upload_missingFileField_returns400BadRequest() throws Exception {
        mockMvc.perform(multipart("/documents/upload"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Feld 'file' fehlt"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.stackTrace").doesNotExist());

        verify(documentService, never()).upload(any(), any());
    }

    // ─── search ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /documents/search?q=Hello → 200 mit Trefferliste")
    void search_returnsMatchingDocuments() throws Exception {
        Document doc = new Document("HelloWorld.pdf", "application/pdf", 10L, null);
        doc.setId(UUID.randomUUID());
        when(documentService.search("Hello")).thenReturn(List.of(doc));

        mockMvc.perform(get("/documents/search").param("q", "Hello"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].filename").value("HelloWorld.pdf"));
    }

    @Test
    @DisplayName("Liste und Suche enthalten keinen OCR-Text, die Detailansicht schon")
    void listAndSearch_omitOcrText_detailIncludesIt() throws Exception {
        Document doc = new Document("HelloWorld.pdf", "application/pdf", 10L, null);
        UUID id = UUID.randomUUID();
        doc.setId(id);
        doc.setOcrText("Hello World");
        doc.setSummary("Eine Zusammenfassung");
        when(documentService.findAll()).thenReturn(List.of(doc));
        when(documentService.search("Hello")).thenReturn(List.of(doc));
        when(documentService.findById(id)).thenReturn(doc);

        mockMvc.perform(get("/documents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].filename").value("HelloWorld.pdf"))
                .andExpect(jsonPath("$[0].ocrText").doesNotExist())
                .andExpect(jsonPath("$[0].summary").doesNotExist());
        mockMvc.perform(get("/documents/search").param("q", "Hello"))
                .andExpect(jsonPath("$[0].ocrText").doesNotExist());
        mockMvc.perform(get("/documents/" + id))
                .andExpect(jsonPath("$.ocrText").value("Hello World"))
                .andExpect(jsonPath("$.summary").value("Eine Zusammenfassung"));
    }

    @Test
    @DisplayName("GET /documents/search ohne Treffer → 200 mit leerer Liste")
    void search_noHits_returnsEmptyList() throws Exception {
        when(documentService.search("xyz")).thenReturn(List.of());

        mockMvc.perform(get("/documents/search").param("q", "xyz"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("GET /documents/search?q= (leer) → 400")
    void search_emptyQuery_returns400() throws Exception {
        mockMvc.perform(get("/documents/search").param("q", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verify(documentService, never()).search(any());
    }

    @Test
    @DisplayName("GET /documents/search ohne q → 400")
    void search_missingQuery_returns400() throws Exception {
        mockMvc.perform(get("/documents/search"))
                .andExpect(status().isBadRequest());

        verify(documentService, never()).search(any());
    }

    @Test
    @DisplayName("GET /documents/search bei Elasticsearch-Ausfall → 503")
    void search_elasticsearchDown_returns503() throws Exception {
        when(documentService.search("Hello")).thenThrow(new SearchException("down", new RuntimeException()));

        mockMvc.perform(get("/documents/search").param("q", "Hello"))
                .andExpect(status().isServiceUnavailable());
    }
}

