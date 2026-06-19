package com.dave.filestorage.controller;

import com.dave.filestorage.dto.*;
import com.dave.filestorage.storage.NotificationService;
import com.dave.filestorage.storage.ObjectStorageService;
import com.dave.filestorage.storage.S3NamingSanitizer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(FilesController.class);

    @Autowired
    private ObjectStorageService objectStorageService;

    @Autowired
    private NotificationService notificationService;

    @Operation(summary = "Upload a File", description = "Uploads a file to the configured S3-compatible backend.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "File uploaded successfully",
                    content = @Content(schema = @Schema(implementation = FileDocumentDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request",
                    content = @Content(examples = @ExampleObject(value = ""))),
            @ApiResponse(responseCode = "500", description = "Storage backend error",
                    content = @Content(examples = @ExampleObject(value = "")))})
    @PostMapping("upload")
    public ResponseEntity<com.dave.filestorage.dto.ApiResponse<FileDocumentDto>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "fileType", required = false) String fileType,
            @RequestParam(value = "isPublic", required = false, defaultValue = "true") Boolean isPublic) throws Exception {

        boolean finalIsPublic = isPublic == null || isPublic;
        String effectiveFileType = S3NamingSanitizer.sanitizeOrDefault(fileType);
        FileDocumentDto result = objectStorageService.uploadFile(file, effectiveFileType, finalIsPublic);
        log.info("File uploaded: {} size={}", result.getFileName(), result.getSize());
        return ResponseEntity.ok(com.dave.filestorage.dto.ApiResponse.success(result));
    }

    @Operation(summary = "Download a File", description = "Download by ETag. Supports Range header for partial/streaming content.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Full file content"),
            @ApiResponse(responseCode = "206", description = "Partial content (Range request)"),
            @ApiResponse(responseCode = "404", description = "File not found"),
            @ApiResponse(responseCode = "416", description = "Range not satisfiable")})
    @GetMapping("download/{eTagId}")
    public ResponseEntity<?> downloadFileByETag(
            @PathVariable String eTagId,
            @RequestHeader(value = HttpHeaders.RANGE, required = false) String rangeHeader) throws Exception {

        if (rangeHeader != null) {
            if (!rangeHeader.startsWith("bytes=")) {
                return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
                        .body(com.dave.filestorage.dto.ApiResponse.error("INVALID_RANGE", "Range header must use bytes= unit"));
            }
            try {
                String[] rangeParts = rangeHeader.substring(6).split("-");
                long start = Long.parseLong(rangeParts[0].trim());
                long end = rangeParts.length > 1 && !rangeParts[1].trim().isEmpty()
                        ? Long.parseLong(rangeParts[1].trim()) : -1L;
                if (start < 0 || (end >= 0 && end < start)) {
                    return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
                            .body(com.dave.filestorage.dto.ApiResponse.error("INVALID_RANGE", "Invalid byte range: " + rangeHeader));
                }
                RangeDownloadResult result = objectStorageService.downloadFileRange(eTagId, start, end);
                if (result == null) return ResponseEntity.notFound().build();
                return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                        .header(HttpHeaders.CONTENT_RANGE, "bytes " + result.getRangeStart()
                                + "-" + result.getRangeEnd() + "/" + result.getTotalSize())
                        .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                        .contentLength(result.getContentLength())
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .body(new InputStreamResource(result.getData()));
            } catch (NumberFormatException ex) {
                return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
                        .body(com.dave.filestorage.dto.ApiResponse.error("INVALID_RANGE", "Malformed range header: " + rangeHeader));
            }
        }

        InputStream inputStream = objectStorageService.downloadFileEtag(eTagId);
        if (inputStream == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + eTagId + "\"")
                .body(new InputStreamResource(inputStream));
    }

    @Operation(summary = "Refresh presigned URL", description = "Regenerates and returns a fresh presigned URL for a private file.")
    @GetMapping("refresh/{id}")
    public ResponseEntity<com.dave.filestorage.dto.ApiResponse<FileDocumentDto>> getFileUrl(@PathVariable String id) throws Exception {
        FileDocumentDto result = objectStorageService.getFileDocumentInformation(id);
        return ResponseEntity.ok(com.dave.filestorage.dto.ApiResponse.success(result));
    }

    // ── Multipart ──────────────────────────────────────────────────────────────

    @Operation(summary = "Initiate multipart upload", description = "Starts a multipart upload session. Returns uploadId and object coordinates.")
    @PostMapping("multipart/initiate")
    public ResponseEntity<com.dave.filestorage.dto.ApiResponse<MultipartInitiateResponseDto>> initiateMultipart(
            @RequestParam String filename,
            @RequestParam(required = false) String fileType,
            @RequestParam(required = false, defaultValue = "application/octet-stream") String contentType,
            @RequestParam(required = false, defaultValue = "true") Boolean isPublic) throws Exception {

        String effectiveFileType = S3NamingSanitizer.sanitizeOrDefault(fileType);
        MultipartInitiateResponseDto result = objectStorageService.initiateMultipartUpload(
                filename, effectiveFileType, contentType, Boolean.TRUE.equals(isPublic));
        return ResponseEntity.ok(com.dave.filestorage.dto.ApiResponse.success(result));
    }

    @Operation(summary = "Upload a part", description = "Uploads one part of an in-progress multipart upload.")
    @PostMapping("multipart/{uploadId}/part/{partNumber}")
    public ResponseEntity<com.dave.filestorage.dto.ApiResponse<String>> uploadPart(
            @PathVariable String uploadId,
            @PathVariable int partNumber,
            @RequestParam String bucket,
            @RequestParam String objectName,
            @RequestParam("part") MultipartFile part) throws Exception {

        String eTag = objectStorageService.uploadPart(bucket, objectName, uploadId,
                partNumber, part.getInputStream(), part.getSize());
        return ResponseEntity.ok(com.dave.filestorage.dto.ApiResponse.success(eTag));
    }

    @Operation(summary = "Complete multipart upload", description = "Finalises the multipart upload and persists metadata.")
    @PostMapping("multipart/complete")
    public ResponseEntity<com.dave.filestorage.dto.ApiResponse<FileDocumentDto>> completeMultipart(
            @org.springframework.web.bind.annotation.RequestBody MultipartCompleteRequestDto request) throws Exception {
        return ResponseEntity.ok(com.dave.filestorage.dto.ApiResponse.success(
                objectStorageService.completeMultipartUpload(request)));
    }

    @Operation(summary = "Abort multipart upload", description = "Aborts an in-progress multipart upload and removes uploaded parts.")
    @DeleteMapping("multipart/{uploadId}/abort")
    public ResponseEntity<com.dave.filestorage.dto.ApiResponse<String>> abortMultipart(
            @PathVariable String uploadId,
            @RequestParam String bucket,
            @RequestParam String objectName) throws Exception {
        objectStorageService.abortMultipartUpload(bucket, objectName, uploadId);
        return ResponseEntity.ok(com.dave.filestorage.dto.ApiResponse.success("Multipart upload aborted"));
    }

    // ── Presigned PUT ──────────────────────────────────────────────────────────

    @Operation(summary = "Generate presigned PUT URL", description = "Returns a short-lived URL for direct browser-to-storage upload.")
    @PostMapping("presigned-upload")
    public ResponseEntity<com.dave.filestorage.dto.ApiResponse<PresignedUploadResponseDto>> generatePresignedUploadUrl(
            @org.springframework.web.bind.annotation.RequestBody PresignedUploadRequestDto request) throws Exception {
        String effectiveFileType = S3NamingSanitizer.sanitizeOrDefault(request.getFileType());
        PresignedUploadResponseDto result = objectStorageService.generatePresignedUploadUrl(
                request.getFilename(), effectiveFileType, request.getContentType(),
                Boolean.TRUE.equals(request.getIsPublic()));
        return ResponseEntity.ok(com.dave.filestorage.dto.ApiResponse.success(result));
    }

    @Operation(summary = "Confirm presigned upload", description = "After the client has PUT the file via the presigned URL, call this to persist metadata.")
    @PostMapping("presigned-upload/confirm")
    public ResponseEntity<com.dave.filestorage.dto.ApiResponse<FileDocumentDto>> confirmPresignedUpload(
            @org.springframework.web.bind.annotation.RequestBody PresignedUploadConfirmDto confirm) throws Exception {
        return ResponseEntity.ok(com.dave.filestorage.dto.ApiResponse.success(
                objectStorageService.confirmPresignedUpload(confirm)));
    }

    // ── Versioning ─────────────────────────────────────────────────────────────

    @Operation(summary = "Download a specific version", description = "Downloads a named version of a file identified by ETag.")
    @GetMapping("download/{eTagId}/version")
    public ResponseEntity<?> downloadFileByVersion(
            @PathVariable String eTagId,
            @RequestParam String versionId) throws Exception {
        InputStream inputStream = objectStorageService.downloadFileEtagWithVersion(eTagId, versionId);
        if (inputStream == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + eTagId + "\"")
                .body(new InputStreamResource(inputStream));
    }

    @Operation(summary = "List all versions", description = "Lists all stored versions of a file. Supports pagination.")
    @GetMapping("{eTagId}/versions")
    public ResponseEntity<com.dave.filestorage.dto.ApiResponse<PagedResult<FileVersionDto>>> listVersions(
            @PathVariable String eTagId,
            @Parameter(description = "Zero-based page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Maximum results per page (1-200)") @RequestParam(defaultValue = "50") int size) throws Exception {
        java.util.List<FileVersionDto> all = objectStorageService.listFileVersions(eTagId);
        int clampedSize = Math.min(Math.max(size, 1), 200);
        int from = page * clampedSize;
        java.util.List<FileVersionDto> pageContent = from >= all.size()
                ? java.util.Collections.emptyList()
                : all.subList(from, Math.min(from + clampedSize, all.size()));
        return ResponseEntity.ok(com.dave.filestorage.dto.ApiResponse.success(
                new PagedResult<>(pageContent, page, clampedSize, all.size())));
    }

    // ── Copy / Move ────────────────────────────────────────────────────────────

    @Operation(summary = "Copy object", description = "Server-side copy of an object, optionally to a different bucket or filename.")
    @PostMapping("copy")
    public ResponseEntity<com.dave.filestorage.dto.ApiResponse<ObjectCopyResponseDto>> copyObject(
            @org.springframework.web.bind.annotation.RequestBody ObjectCopyRequestDto request) throws Exception {
        return ResponseEntity.ok(com.dave.filestorage.dto.ApiResponse.success(
                objectStorageService.copyObject(request)));
    }

    @Operation(summary = "Move object", description = "Server-side move (copy + delete source) of an object.")
    @PostMapping("move")
    public ResponseEntity<com.dave.filestorage.dto.ApiResponse<ObjectCopyResponseDto>> moveObject(
            @org.springframework.web.bind.annotation.RequestBody ObjectCopyRequestDto request) throws Exception {
        return ResponseEntity.ok(com.dave.filestorage.dto.ApiResponse.success(
                objectStorageService.moveObject(request)));
    }

    // ── Webhooks ───────────────────────────────────────────────────────────────

    @Operation(summary = "Register webhook", description = "Registers a URL to receive notifications when objects are created or removed.")
    @PostMapping("webhooks")
    public ResponseEntity<com.dave.filestorage.dto.ApiResponse<WebhookConfigDto>> registerWebhook(
            @org.springframework.web.bind.annotation.RequestBody WebhookConfigDto config) throws Exception {
        return ResponseEntity.ok(com.dave.filestorage.dto.ApiResponse.success(
                notificationService.registerWebhook(config)));
    }

    @Operation(summary = "Deregister webhook", description = "Soft-deactivates a registered webhook by ID.")
    @DeleteMapping("webhooks/{id}")
    public ResponseEntity<com.dave.filestorage.dto.ApiResponse<String>> deregisterWebhook(@PathVariable String id) throws Exception {
        notificationService.deregisterWebhook(id);
        return ResponseEntity.ok(com.dave.filestorage.dto.ApiResponse.success("Webhook deregistered"));
    }

    @Operation(summary = "List webhooks", description = "Returns registered webhooks, optionally filtered by bucket. Supports pagination.")
    @GetMapping("webhooks")
    public ResponseEntity<com.dave.filestorage.dto.ApiResponse<PagedResult<WebhookConfigDto>>> listWebhooks(
            @Parameter(description = "Filter by bucket name") @RequestParam(required = false) String bucket,
            @Parameter(description = "Zero-based page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Maximum results per page (1-100)") @RequestParam(defaultValue = "20") int size) throws Exception {
        java.util.List<WebhookConfigDto> all = notificationService.listWebhooks(bucket);
        int clampedSize = Math.min(Math.max(size, 1), 100);
        int from = page * clampedSize;
        java.util.List<WebhookConfigDto> pageContent = from >= all.size()
                ? java.util.Collections.emptyList()
                : all.subList(from, Math.min(from + clampedSize, all.size()));
        return ResponseEntity.ok(com.dave.filestorage.dto.ApiResponse.success(
                new PagedResult<>(pageContent, page, clampedSize, all.size())));
    }
}
