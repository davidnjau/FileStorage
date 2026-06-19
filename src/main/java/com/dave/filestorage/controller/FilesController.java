package com.dave.filestorage.controller;

import com.dave.filestorage.dto.*;
import com.dave.filestorage.storage.GarageNotificationServiceImpl;
import com.dave.filestorage.storage.ObjectStorageService;
import com.dave.filestorage.storage.S3NamingSanitizer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

@Tag(description = "Use these resource to save file in the system.", name = "File system module")
@RequestMapping("/files/")
@RestController
public class FilesController {

    @Autowired
    private ObjectStorageService objectStorageService;

    @Autowired
    private GarageNotificationServiceImpl notificationService;

    @Operation(summary = "Upload a File", description = "Uploads a file to the server's file system.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "${api.response-codes.ok.desc}"),
            @ApiResponse(responseCode = "400", description = "${api.response-codes.badRequest.desc}",
                    content = { @Content(examples = { @ExampleObject(value = "") }) }),
            @ApiResponse(responseCode = "404", description = "${api.response-codes.notFound.desc}",
                    content = { @Content(examples = { @ExampleObject(value = "") }) }) })
    @PostMapping("upload")
    public ResponseEntity<?> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "fileType", required = false) String fileType,
            @RequestParam(value = "isPublic", required = false, defaultValue = "true") Boolean isPublic) {

        try {
            boolean finalIsPublic = isPublic == null || isPublic;
            String effectiveFileType = S3NamingSanitizer.sanitizeOrDefault(fileType);
            FileDocumentDto fileDocumentDto = objectStorageService.uploadFile(file, effectiveFileType, finalIsPublic);
            return ResponseEntity.ok(fileDocumentDto);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseBodyDto("Failed to upload file. Please try again later."));
        }
    }

    @Operation(summary = "Download a File", description = "Dowload a file to the server's file system.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "${api.response-codes.ok.desc}"),
            @ApiResponse(responseCode = "400", description = "${api.response-codes.badRequest.desc}",
                    content = { @Content(examples = { @ExampleObject(value = "") }) }),
            @ApiResponse(responseCode = "404", description = "${api.response-codes.notFound.desc}",
                    content = { @Content(examples = { @ExampleObject(value = "") }) }) })
    @GetMapping("download/{eTagId}")
    public ResponseEntity<?> downloadFileByETag(
            @PathVariable String eTagId,
            @RequestHeader(value = HttpHeaders.RANGE, required = false) String rangeHeader) {

        try {
            if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
                String[] rangeParts = rangeHeader.substring(6).split("-");
                long start = Long.parseLong(rangeParts[0]);
                long end = rangeParts.length > 1 && !rangeParts[1].isEmpty()
                        ? Long.parseLong(rangeParts[1]) : -1L;
                RangeDownloadResult result = objectStorageService.downloadFileRange(eTagId, start, end);
                if (result == null) return ResponseEntity.notFound().build();
                return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                        .header(HttpHeaders.CONTENT_RANGE, "bytes " + result.getRangeStart()
                                + "-" + result.getRangeEnd() + "/" + result.getTotalSize())
                        .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                        .contentLength(result.getContentLength())
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .body(new InputStreamResource(result.getData()));
            }

            InputStream inputStream = objectStorageService.downloadFileEtag(eTagId);
            InputStreamResource resource = new InputStreamResource(inputStream);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + eTagId + "\"")
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseBodyDto("The provided id does not exist or is invalid. Please try again."));
        }
    }

    @GetMapping("refresh/{id}")
    public ResponseEntity<?> getFileUrl(@PathVariable String id) {
        try {
            FileDocumentDto fileDocumentDto = objectStorageService.getFileDocumentInformation(id);
            return ResponseEntity.ok(fileDocumentDto);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseBodyDto("Failed to upload file. Please try again later."));
        }
    }

    // Multipart
    @PostMapping("multipart/initiate")
    public ResponseEntity<?> initiateMultipart(
            @RequestParam String filename,
            @RequestParam(required = false) String fileType,
            @RequestParam(required = false, defaultValue = "application/octet-stream") String contentType,
            @RequestParam(required = false, defaultValue = "true") Boolean isPublic) {
        try {
            String effectiveFileType = S3NamingSanitizer.sanitizeOrDefault(fileType);
            return ResponseEntity.ok(objectStorageService.initiateMultipartUpload(
                    filename, effectiveFileType, contentType, Boolean.TRUE.equals(isPublic)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ResponseBodyDto("Failed to initiate multipart upload."));
        }
    }

    @PostMapping("multipart/{uploadId}/part/{partNumber}")
    public ResponseEntity<?> uploadPart(
            @PathVariable String uploadId,
            @PathVariable int partNumber,
            @RequestParam String bucket,
            @RequestParam String objectName,
            @RequestParam("part") MultipartFile part) {
        try {
            String eTag = objectStorageService.uploadPart(bucket, objectName, uploadId,
                    partNumber, part.getInputStream(), part.getSize());
            return ResponseEntity.ok(new ResponseBodyDto(eTag));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ResponseBodyDto("Failed to upload part."));
        }
    }

    @PostMapping("multipart/complete")
    public ResponseEntity<?> completeMultipart(@RequestBody MultipartCompleteRequestDto request) {
        try {
            return ResponseEntity.ok(objectStorageService.completeMultipartUpload(request));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ResponseBodyDto("Failed to complete multipart upload."));
        }
    }

    @DeleteMapping("multipart/{uploadId}/abort")
    public ResponseEntity<?> abortMultipart(
            @PathVariable String uploadId,
            @RequestParam String bucket,
            @RequestParam String objectName) {
        try {
            objectStorageService.abortMultipartUpload(bucket, objectName, uploadId);
            return ResponseEntity.ok(new ResponseBodyDto("Multipart upload aborted."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ResponseBodyDto("Failed to abort multipart upload."));
        }
    }

    // Presigned PUT
    @PostMapping("presigned-upload")
    public ResponseEntity<?> generatePresignedUploadUrl(@RequestBody PresignedUploadRequestDto request) {
        try {
            String effectiveFileType = S3NamingSanitizer.sanitizeOrDefault(request.getFileType());
            return ResponseEntity.ok(objectStorageService.generatePresignedUploadUrl(
                    request.getFilename(), effectiveFileType, request.getContentType(),
                    Boolean.TRUE.equals(request.getIsPublic())));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ResponseBodyDto("Failed to generate presigned upload URL."));
        }
    }

    @PostMapping("presigned-upload/confirm")
    public ResponseEntity<?> confirmPresignedUpload(@RequestBody PresignedUploadConfirmDto confirm) {
        try {
            return ResponseEntity.ok(objectStorageService.confirmPresignedUpload(confirm));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ResponseBodyDto("Failed to confirm upload."));
        }
    }

    // Versioning
    @GetMapping("download/{eTagId}/version")
    public ResponseEntity<?> downloadFileByVersion(
            @PathVariable String eTagId,
            @RequestParam String versionId) {
        try {
            InputStream inputStream = objectStorageService.downloadFileEtagWithVersion(eTagId, versionId);
            if (inputStream == null) return ResponseEntity.notFound().build();
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + eTagId + "\"")
                    .body(new InputStreamResource(inputStream));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ResponseBodyDto("Failed to download file version."));
        }
    }

    @GetMapping("{eTagId}/versions")
    public ResponseEntity<?> listVersions(@PathVariable String eTagId) {
        try {
            return ResponseEntity.ok(objectStorageService.listFileVersions(eTagId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ResponseBodyDto("Failed to list file versions."));
        }
    }

    // Copy/Move
    @PostMapping("copy")
    public ResponseEntity<?> copyObject(@RequestBody ObjectCopyRequestDto request) {
        try {
            return ResponseEntity.ok(objectStorageService.copyObject(request));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ResponseBodyDto("Failed to copy file."));
        }
    }

    @PostMapping("move")
    public ResponseEntity<?> moveObject(@RequestBody ObjectCopyRequestDto request) {
        try {
            return ResponseEntity.ok(objectStorageService.moveObject(request));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ResponseBodyDto("Failed to move file."));
        }
    }

    // Webhooks
    @PostMapping("webhooks")
    public ResponseEntity<?> registerWebhook(@RequestBody WebhookConfigDto config) {
        try {
            return ResponseEntity.ok(notificationService.registerWebhook(config));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ResponseBodyDto("Failed to register webhook."));
        }
    }

    @DeleteMapping("webhooks/{id}")
    public ResponseEntity<?> deregisterWebhook(@PathVariable String id) {
        try {
            notificationService.deregisterWebhook(id);
            return ResponseEntity.ok(new ResponseBodyDto("Webhook deregistered."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ResponseBodyDto("Failed to deregister webhook."));
        }
    }

    @GetMapping("webhooks")
    public ResponseEntity<?> listWebhooks(@RequestParam(required = false) String bucket) {
        try {
            return ResponseEntity.ok(notificationService.listWebhooks(bucket));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ResponseBodyDto("Failed to list webhooks."));
        }
    }
}
