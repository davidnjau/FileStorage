package com.dave.filestorage.controller;

import com.dave.filestorage.model.Files;
import com.dave.filestorage.payload.FileUploadResponse;
import com.dave.filestorage.service.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
public class FilesController {

    @Autowired
    private FileStorageService fileStorageService;

    @PostMapping("/fileupload")
    public FileUploadResponse uploadFile(@RequestParam("file") MultipartFile file) {
        Files files = fileStorageService.storeFile(file);

        String fileDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/filedownload/")
                .path(files.getId())
                .toUriString();

        return new FileUploadResponse(files.getName(), fileDownloadUri,
                file.getContentType(), file.getSize());
    }

    @GetMapping("/filedownload/{fileId}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileId) {
        Files dbFile = fileStorageService.getFile(fileId);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(dbFile.getType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "/attachment; filename=\"" + dbFile.getName() + "\"")
                .body(new ByteArrayResource(dbFile.getFilecontent()));
    }
}
