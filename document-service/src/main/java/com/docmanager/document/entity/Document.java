package com.docmanager.document.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "documents")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String filename;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "uploaded_at", nullable = false)
    private Instant uploadedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentStatus status;

    @ToString.Exclude
    @Column(name = "ocr_text", columnDefinition = "text")
    private String ocrText;

    @ToString.Exclude
    @Column(name = "summary", columnDefinition = "text")
    private String summary;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "folder_id")
    private Folder folder;

    @Column(name = "object_key")
    private String objectKey;

    // Kommentare werden mit dem Dokument gelöscht
    @OneToMany(mappedBy = "document", cascade = CascadeType.REMOVE)
    private List<Comment> comments = new ArrayList<>();

    public Document(String filename, String contentType, long sizeBytes, Folder folder) {
        this(filename, contentType, sizeBytes, folder, null);
    }

    public Document(String filename, String contentType, long sizeBytes, Folder folder, String objectKey) {
        this.filename = filename;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.folder = folder;
        this.objectKey = objectKey;
        this.uploadedAt = Instant.now();
        this.status = DocumentStatus.OCR_PENDING;
    }
}