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
    private NotificationService notificationService;

    @Operation(
        summary = "Upload a file",
        description = "Uploads a file to the active S3 backend and persists metadata to MongoDB. " +
            "Public files return a direct URL; private files return a presigned URL valid for the configured expiry period."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "File uploaded successfully",
            content = @Content(schema = @Schema(implementation = FileDocumentDto.class))),
        @ApiResponse(responseCode = "400", description = "Upload failed",
            content = @Content(schema = @Schema(implementation = ResponseBodyDto.class)))
    })
    @PostMapping("upload")
    public ResponseEntity<?> uploadFile(
            @Parameter(description = "File to upload", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "Logical category used as a path prefix in the object key (e.g. invoices, avatars)")
            @RequestParam(value = "fileType", required = false) String fileType,
            @Parameter(description = "true = direct public URL, false = presigned private URL (default: true)")
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

    @Operation(
        summary = "Download a file",
        description = "Downloads a file by its ETag. Supports partial content via the standard HTTP Range header " +
            "(e.g. Range: bytes=0-1023). Returns 206 Partial Content when a range is requested."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Full file content"),
        @ApiResponse(responseCode = "206", description = "Partial content (byte-range response)"),
        @ApiResponse(responseCode = "400", description = "File not found or invalid ETag",
            content = @Content(schema = @Schema(implementation = ResponseBodyDto.class)))
    })
    @GetMapping("download/{eTagId}")
    public ResponseEntity<?> downloadFileByETag(
            @Parameter(description = "ETag of the file to download")
            @PathVariable String eTagId,
            @Parameter(description = "Optional byte range, e.g. bytes=0-1023", example = "bytes=0-1023")
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

    @Operation(
        summary = "Refresh presigned URL",
        description = "Regenerates a fresh presigned URL for a private file. Useful when the previous URL has expired."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Fresh URL generated",
            content = @Content(schema = @Schema(implementation = FileDocumentDto.class))),
        @ApiResponse(responseCode = "400", description = "File not found",
            content = @Content(schema = @Schema(implementation = ResponseBodyDto.class)))
    })
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
    @Operation(
        summary = "Initiate multipart upload",
        description = "Starts a multipart upload session. Returns an uploadId and objectName to use in subsequent part uploads."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Session created",
            content = @Content(schema = @Schema(implementation = MultipartInitiateResponseDto.class))),
        @ApiResponse(responseCode = "400", description = "Failed to initiate",
            content = @Content(schema = @Schema(implementation = ResponseBodyDto.class)))
    })
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

    @Operation(
        summary = "Upload a multipart part",
        description = "Uploads one part of a multipart upload. Parts must be at least 5 MB except for the last part. " +
            "Returns the ETag of the part — store it for the complete request."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Part uploaded, returns ETag",
            content = @Content(schema = @Schema(implementation = ResponseBodyDto.class))),
        @ApiResponse(responseCode = "400", description = "Part upload failed",
            content = @Content(schema = @Schema(implementation = ResponseBodyDto.class)))
    })
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

    @Operation(
        summary = "Complete multipart upload",
        description = "Assembles all uploaded parts into the final object. Parts must be provided in ascending partNumber order."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "File assembled and metadata persisted",
            content = @Content(schema = @Schema(implementation = FileDocumentDto.class))),
        @ApiResponse(responseCode = "400", description = "Completion failed",
            content = @Content(schema = @Schema(implementation = ResponseBodyDto.class)))
    })
    @PostMapping("multipart/complete")
    public ResponseEntity<?> completeMultipart(@org.springframework.web.bind.annotation.RequestBody MultipartCompleteRequestDto request) {
        try {
            return ResponseEntity.ok(objectStorageService.completeMultipartUpload(request));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ResponseBodyDto("Failed to complete multipart upload."));
        }
    }

    @Operation(
        summary = "Abort multipart upload",
        description = "Cancels the multipart upload and releases all uploaded parts from storage."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Upload aborted"),
        @ApiResponse(responseCode = "400", description = "Abort failed",
            content = @Content(schema = @Schema(implementation = ResponseBodyDto.class)))
    })
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
    @Operation(
        summary = "Generate presigned PUT URL",
        description = "Generates a time-limited URL the client can use to PUT a file directly to storage without routing " +
            "through this server. After the PUT completes, call /presigned-upload/confirm to persist metadata."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Presigned URL generated",
            content = @Content(schema = @Schema(implementation = PresignedUploadResponseDto.class))),
        @ApiResponse(responseCode = "400", description = "URL generation failed",
            content = @Content(schema = @Schema(implementation = ResponseBodyDto.class)))
    })
    @PostMapping("presigned-upload")
    public ResponseEntity<?> generatePresignedUploadUrl(@org.springframework.web.bind.annotation.RequestBody PresignedUploadRequestDto request) {
        try {
            String effectiveFileType = S3NamingSanitizer.sanitizeOrDefault(request.getFileType());
            return ResponseEntity.ok(objectStorageService.generatePresignedUploadUrl(
                    request.getFilename(), effectiveFileType, request.getContentType(),
                    Boolean.TRUE.equals(request.getIsPublic())));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ResponseBodyDto("Failed to generate presigned upload URL."));
        }
    }

    @Operation(
        summary = "Confirm presigned upload",
        description = "Fetches the object metadata from storage and persists a FileDocument to MongoDB. " +
            "Call this after the client has successfully PUT the file to the presigned URL."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Metadata persisted",
            content = @Content(schema = @Schema(implementation = FileDocumentDto.class))),
        @ApiResponse(responseCode = "400", description = "Object not found or metadata persistence failed",
            content = @Content(schema = @Schema(implementation = ResponseBodyDto.class)))
    })
    @PostMapping("presigned-upload/confirm")
    public ResponseEntity<?> confirmPresignedUpload(@org.springframework.web.bind.annotation.RequestBody PresignedUploadConfirmDto confirm) {
        try {
            return ResponseEntity.ok(objectStorageService.confirmPresignedUpload(confirm));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ResponseBodyDto("Failed to confirm upload."));
        }
    }

    // Versioning
    @Operation(
        summary = "Download a specific file version",
        description = "Downloads a specific version of a file identified by its ETag and versionId. " +
            "Requires versioning to be enabled on the bucket."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "File version downloaded"),
        @ApiResponse(responseCode = "400", description = "Version not found",
            content = @Content(schema = @Schema(implementation = ResponseBodyDto.class)))
    })
    @GetMapping("download/{eTagId}/version")
    public ResponseEntity<?> downloadFileByVersion(
            @PathVariable String eTagId,
            @Parameter(description = "Version ID as returned by the list-versions endpoint")
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

    @Operation(
        summary = "List file versions",
        description = "Returns all stored versions of a file in reverse-chronological order. " +
            "The latest version is marked with isLatest=true."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Version list returned"),
        @ApiResponse(responseCode = "400", description = "File not found",
            content = @Content(schema = @Schema(implementation = ResponseBodyDto.class)))
    })
    @GetMapping("{eTagId}/versions")
    public ResponseEntity<?> listVersions(@PathVariable String eTagId) {
        try {
            return ResponseEntity.ok(objectStorageService.listFileVersions(eTagId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ResponseBodyDto("Failed to list file versions."));
        }
    }

    // Copy/Move
    @Operation(
        summary = "Copy a file",
        description = "Performs a server-side copy — the binary is duplicated inside storage without re-uploading from the client. " +
            "The copy lands in the same bucket unless destinationBucket is specified."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "File copied",
            content = @Content(schema = @Schema(implementation = ObjectCopyResponseDto.class))),
        @ApiResponse(responseCode = "400", description = "Source not found or copy failed",
            content = @Content(schema = @Schema(implementation = ResponseBodyDto.class)))
    })
    @PostMapping("copy")
    public ResponseEntity<?> copyObject(@org.springframework.web.bind.annotation.RequestBody ObjectCopyRequestDto request) {
        try {
            return ResponseEntity.ok(objectStorageService.copyObject(request));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ResponseBodyDto("Failed to copy file."));
        }
    }

    @Operation(
        summary = "Move a file",
        description = "Server-side copy followed by deletion of the source. The source FileDocument is soft-archived in MongoDB."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "File moved",
            content = @Content(schema = @Schema(implementation = ObjectCopyResponseDto.class))),
        @ApiResponse(responseCode = "400", description = "Move failed",
            content = @Content(schema = @Schema(implementation = ResponseBodyDto.class)))
    })
    @PostMapping("move")
    public ResponseEntity<?> moveObject(@org.springframework.web.bind.annotation.RequestBody ObjectCopyRequestDto request) {
        try {
            return ResponseEntity.ok(objectStorageService.moveObject(request));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ResponseBodyDto("Failed to move file."));
        }
    }

    // Webhooks
    @Operation(
        summary = "Register a webhook",
        description = "Registers an HTTP endpoint to receive notifications when objects are created or removed in a bucket. " +
            "MinIO uses a live SSE stream; Garage uses scheduled polling (enable via storage.notification.polling.enabled=true)."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Webhook registered",
            content = @Content(schema = @Schema(implementation = WebhookConfigDto.class))),
        @ApiResponse(responseCode = "400", description = "Registration failed",
            content = @Content(schema = @Schema(implementation = ResponseBodyDto.class)))
    })
    @PostMapping("webhooks")
    public ResponseEntity<?> registerWebhook(@org.springframework.web.bind.annotation.RequestBody WebhookConfigDto config) {
        try {
            return ResponseEntity.ok(notificationService.registerWebhook(config));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ResponseBodyDto("Failed to register webhook."));
        }
    }

    @Operation(summary = "Deregister a webhook", description = "Marks the webhook as inactive. No further events will be delivered.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Webhook deregistered"),
        @ApiResponse(responseCode = "400", description = "Deregistration failed",
            content = @Content(schema = @Schema(implementation = ResponseBodyDto.class)))
    })
    @DeleteMapping("webhooks/{id}")
    public ResponseEntity<?> deregisterWebhook(@PathVariable String id) {
        try {
            notificationService.deregisterWebhook(id);
            return ResponseEntity.ok(new ResponseBodyDto("Webhook deregistered."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ResponseBodyDto("Failed to deregister webhook."));
        }
    }

    @Operation(summary = "List webhooks", description = "Returns all active webhook registrations, optionally filtered by bucket.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Webhook list returned"),
        @ApiResponse(responseCode = "400", description = "Query failed",
            content = @Content(schema = @Schema(implementation = ResponseBodyDto.class)))
    })
    @GetMapping("webhooks")
    public ResponseEntity<?> listWebhooks(
            @Parameter(description = "Filter by bucket name — omit to return webhooks for all buckets")
            @RequestParam(required = false) String bucket) {
        try {
            return ResponseEntity.ok(notificationService.listWebhooks(bucket));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ResponseBodyDto("Failed to list webhooks."));
        }
    }
}
