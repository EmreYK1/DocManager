package com.docmanager.document.controller;

import com.docmanager.document.dto.FolderRequest;
import com.docmanager.document.dto.FolderResponse;
import com.docmanager.document.entity.Folder;
import com.docmanager.document.mapper.FolderMapper;
import com.docmanager.document.service.FolderService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/folders")
public class FolderController {
    private final FolderService folderService;

    public FolderController(FolderService folderService) {
        this.folderService = folderService;
    }

    @PostMapping
    public FolderResponse create(@RequestBody FolderRequest request) {
        Folder parent = request.getParentId() != null
                ? folderService.findById(request.getParentId())
                : null;
        return FolderMapper.toResponse(folderService.save(new Folder(request.getName(), parent)));
    }

    @GetMapping("/{id}")
    public FolderResponse getOne(@PathVariable UUID id) {
        return FolderMapper.toResponse(folderService.findById(id));
    }

    @GetMapping
    public List<FolderResponse> getAll() {
        return folderService.findAll().stream()
                .map(FolderMapper::toResponse)
                .toList();
    }

    @GetMapping("/{id}/children")
    public List<FolderResponse> getChildren(@PathVariable UUID id) {
        return folderService.findChildren(id).stream()
                .map(FolderMapper::toResponse)
                .toList();
    }

    @PutMapping("/{id}")
    public FolderResponse update(@PathVariable UUID id, @RequestBody FolderRequest request) {
        Folder parent = request.getParentId() != null
                ? folderService.findById(request.getParentId())
                : null;
        return FolderMapper.toResponse(folderService.update(id, new Folder(request.getName(), parent)));
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        folderService.delete(id);
    }
}
