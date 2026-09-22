package com.docmanager.document.repository;
import com.docmanager.document.entity.Folder;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import java.util.List;

public interface FolderRepository extends JpaRepository<Folder, UUID> {
    List<Folder> findByParent(Folder parent);
}
