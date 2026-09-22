package com.docmanager.document.repository;
import com.docmanager.document.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface DocumentRepository extends JpaRepository<Document, UUID> {
}
