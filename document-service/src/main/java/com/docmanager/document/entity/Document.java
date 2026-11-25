package com.docmanager.document.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "documents")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Verhindert, dass sich gleichzeitige Änderungen gegenseitig überschreiben. */
    @Version
    private long version;

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

    @Column(name = "ocr_text", columnDefinition = "text")
    private String ocrText;

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

    public Document(String filename, String contentType, long sizeBytes, Folder folder, String objectKey) {
        this.filename = filename;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.folder = folder;
        this.objectKey = objectKey;
        this.uploadedAt = Instant.now();
        this.status = DocumentStatus.OCR_PENDING;
    }

    public void rename(String newFilename) {
        this.filename = newFilename;
    }

    /** {@code null} bedeutet: in keinem Ordner. */
    public void moveTo(Folder target) {
        this.folder = target;
    }

    /**
     * @return {@code false}, wenn kein Ergebnis mehr erwartet wird, z. B. weil dieselbe Nachricht
     *         zweimal ankommt. Das Dokument bleibt dann unverändert.
     */
    public boolean completeOcr(String text) {
        if (status != DocumentStatus.OCR_PENDING) {
            return false;
        }
        ocrText = text;
        status = DocumentStatus.OCR_DONE;
        return true;
    }

    /** Wie {@link #completeOcr}, aber für eine gescheiterte Texterkennung. */
    public boolean failOcr() {
        if (status != DocumentStatus.OCR_PENDING) {
            return false;
        }
        status = DocumentStatus.FAILED;
        return true;
    }

    /** Wie {@link #failOcr}, aber für eine gescheiterte Zusammenfassung. Der OCR-Text bleibt erhalten. */
    public boolean failSummary() {
        if (status != DocumentStatus.OCR_DONE) {
            return false;
        }
        status = DocumentStatus.SUMMARY_FAILED;
        return true;
    }

    /** Durchsuchbar ist ein Dokument erst, wenn sein Text erkannt wurde. */
    public boolean isSearchable() {
        return status == DocumentStatus.OCR_DONE
                || status == DocumentStatus.SUMMARY_DONE
                || status == DocumentStatus.SUMMARY_FAILED;
    }
}
