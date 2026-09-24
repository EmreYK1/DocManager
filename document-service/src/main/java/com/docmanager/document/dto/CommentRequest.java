package com.docmanager.document.dto;

import lombok.Data;

@Data
public class CommentRequest {
    private String content;
    private String author;
}
