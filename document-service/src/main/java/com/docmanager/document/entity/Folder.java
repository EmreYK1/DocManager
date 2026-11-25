package com.docmanager.document.entity;

import com.docmanager.document.exception.InvalidRequestException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "folders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Folder {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    // mein Elternordner (null = ich bin ganz oben)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Folder parent;

    public Folder(String name, Folder parent) {
        this.name = name;
        this.parent = parent;
    }

    public void rename(String newName) {
        this.name = newName;
    }

    /** Ein Ordner darf nicht unter sich selbst oder einem seiner Unterordner landen. */
    public void moveTo(Folder newParent) {
        List<Folder> visited = new ArrayList<>(); // schützt vor bereits vorhandenen Zyklen in den Daten
        for (Folder f = newParent; f != null && !visited.contains(f); f = f.getParent()) {
            if (f == this || (id != null && id.equals(f.getId()))) {
                throw new InvalidRequestException("Ein Ordner kann nicht in sich selbst verschoben werden");
            }
            visited.add(f);
        }
        this.parent = newParent;
    }
}
