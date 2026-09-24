package com.docmanager.document.repository;

import com.docmanager.document.entity.Comment;
import com.docmanager.document.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CommentRepository extends JpaRepository<Comment, UUID> {
    List<Comment> findByDocument(Document document);
}
