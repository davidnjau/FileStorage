package com.dave.filestorage.controller;

import com.dave.filestorage.dto.FileDocumentDto;
import com.dave.filestorage.dto.ResponseBodyDto;
import com.dave.filestorage.storage.MinioStorageService;
import com.dave.filestorage.storage.S3NamingSanitizer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.InputStream;

@Tag(description = "Use these resource to save file in the system.", name = "File system module")
@RequestMapping("/files")
@RestController
public class FilesController {

    @Autowired
    private MinioStorageService minioStorageService;

    @Operation(summary = "Upload a File", description = "Uploads a file to the server's file system.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "${api.response-codes.ok.desc}"),
            @ApiResponse(responseCode = "400", description = "${api.response-codes.badRequest.desc}",
                    content = { @Content(examples = { @ExampleObject(value = "") }) }),
            @ApiResponse(responseCode = "404", description = "${api.response-codes.notFound.desc}",
                    content = { @Content(examples = { @ExampleObject(value = "") }) }) })
    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "fileType", required = false) String fileType,
            @RequestParam(value = "isPublic", required = false, defaultValue = "true") Boolean isPublic) {

        try {

            boolean finalIsPublic = isPublic == null || isPublic;

            String effectiveFileType = S3NamingSanitizer.sanitizeOrDefault(fileType);

            FileDocumentDto fileDocumentDto = minioStorageService.uploadFile(file, effectiveFileType, finalIsPublic);

            return ResponseEntity.ok(fileDocumentDto);
        } catch (Exception e) {
            e.printStackTrace();
            ResponseBodyDto responseBody = new ResponseBodyDto(
                    "Failed to upload file. Please try again later."
            );
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(responseBody);
        }

    }

    @Operation(summary = "Download a File", description = "Dowload a file to the server's file system.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "${api.response-codes.ok.desc}"),
            @ApiResponse(responseCode = "400", description = "${api.response-codes.badRequest.desc}",
                    content = { @Content(examples = { @ExampleObject(value = "") }) }),
            @ApiResponse(responseCode = "404", description = "${api.response-codes.notFound.desc}",
                    content = { @Content(examples = { @ExampleObject(value = "") }) }) })
    @GetMapping("/download/{eTagId}")
    public ResponseEntity<?> downloadFileByETag(@PathVariable String eTagId) {

        try{
            InputStream inputStream = minioStorageService.downloadFileEtag(eTagId);
            InputStreamResource resource = new InputStreamResource(inputStream);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + eTagId + "\"")
                    .body(resource);

        }catch (Exception e){
            ResponseBodyDto responseBody = new ResponseBodyDto(
                    "The provided id does not exist or is invalid. Please try again."
            );
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(responseBody);
        }

    }
}
