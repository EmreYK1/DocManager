package com.docmanager.document.controller;

import com.docmanager.document.entity.Folder;
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
    public Folder create(@RequestBody Folder folder){
        return folderService.save(folder);
    }

    @GetMapping("/{id}")
    public Folder getOne(@PathVariable UUID id){
        return folderService.findById(id);
    }

    @GetMapping
    public List<Folder> getAll(){
        return folderService.findAll();
    }

    @PutMapping("/{id}")
    public Folder update(@PathVariable UUID id, @RequestBody Folder folder) {
        return folderService.update(id, folder);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        folderService.delete(id);
    }
}