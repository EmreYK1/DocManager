package com.docmanager.document.controller;

import com.docmanager.document.entity.Comment;
import com.docmanager.document.entity.Document;
import com.docmanager.document.exception.GlobalExceptionHandler;
import com.docmanager.document.service.CommentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CommentController.class)
@Import(GlobalExceptionHandler.class)
class CommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CommentService commentService;

    @Test
    @DisplayName("PUT nur mit content → HTTP 200")
    void update_onlyContent_returns200() throws Exception {
        UUID documentId = UUID.randomUUID();
        UUID commentId = UUID.randomUUID();

        Document document = new Document("HelloWorld.pdf", "application/pdf", 12L, null, null);
        ReflectionTestUtils.setField(document, "id", documentId);
        Comment comment = new Comment("Aktualisierter Text", "Max", document);
        ReflectionTestUtils.setField(comment, "id", commentId);

        when(commentService.update(eq(documentId), eq(commentId), eq("Aktualisierter Text")))
                .thenReturn(comment);

        mockMvc.perform(put("/documents/{documentId}/comments/{id}", documentId, commentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"Aktualisierter Text"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(commentId.toString()))
                .andExpect(jsonPath("$.content").value("Aktualisierter Text"))
                .andExpect(jsonPath("$.author").value("Max"));

        verify(commentService).update(documentId, commentId, "Aktualisierter Text");
    }

    @Test
    @DisplayName("PUT mit leerem content → HTTP 400 mit fields.content")
    void update_emptyContent_returns400WithFieldsContent() throws Exception {
        UUID documentId = UUID.randomUUID();
        UUID commentId = UUID.randomUUID();

        mockMvc.perform(put("/documents/{documentId}/comments/{id}", documentId, commentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fields.content").exists());

        verify(commentService, never()).update(any(), any(), any());
    }
}
