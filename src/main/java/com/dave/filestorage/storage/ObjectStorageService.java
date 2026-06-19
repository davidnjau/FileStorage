package com.dave.filestorage.storage;

import com.dave.filestorage.dto.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;

/**
 * Provider-agnostic contract for S3-compatible file storage operations.
 *
 * <p>Implementations are selected at startup via {@code storage.provider=minio|garage}.
 * The active implementation is injected into {@link com.dave.filestorage.controller.FilesController} by Spring.
 */
public interface ObjectStorageService {

    /**
     * Uploads a file to the active S3 backend and asynchronously persists metadata to MongoDB.
     *
     * @param file     multipart file from the HTTP request
     * @param fileType logical category used as a path prefix in the object key (sanitised before use)
     * @param isPublic {@code true} to generate a direct public URL; {@code false} for a presigned URL
     * @return DTO containing the ETag, filename, size, and access URL
     * @throws Exception if the upload or stat call fails
     */
    FileDocumentDto uploadFile(MultipartFile file, String fileType, boolean isPublic) throws Exception;

    /**
     * Downloads the full content of a file by its ETag.
     *
     * @param etag ETag of the file to download
     * @return raw {@link InputStream} of the file bytes
     * @throws Exception if no file with the given ETag exists or the download fails
     */
    InputStream downloadFileEtag(String etag) throws Exception;

    /**
     * Retrieves and refreshes metadata for a stored file, regenerating the presigned URL if needed.
     *
     * @param id MongoDB document ID of the file
     * @return DTO with updated access URL and metadata
     */
    FileDocumentDto getFileDocumentInformation(String id);

    /**
     * Initiates a multipart upload session on the storage backend.
     *
     * @param filename    original filename of the file to be uploaded
     * @param fileType    logical category used as a path prefix
     * @param contentType MIME type of the file
     * @param isPublic    {@code true} for a public file; {@code false} for a private file
     * @return DTO containing the {@code uploadId}, {@code objectName}, and {@code bucket} for subsequent calls
     * @throws Exception if the backend rejects the initiate request
     */
    MultipartInitiateResponseDto initiateMultipartUpload(String filename, String fileType, String contentType, boolean isPublic) throws Exception;

    /**
     * Uploads one part of an ongoing multipart upload.
     *
     * <p>Parts must be at least 5 MB, except for the final part.
     *
     * @param bucket     bucket that holds the in-progress upload
     * @param objectName full object key of the in-progress upload
     * @param uploadId   upload session ID from {@link #initiateMultipartUpload}
     * @param partNumber 1-based index of this part
     * @param data       raw bytes of the part
     * @param partSize   byte length of {@code data}
     * @return ETag of the uploaded part (required for the complete call)
     * @throws Exception if the part upload fails
     */
    String uploadPart(String bucket, String objectName, String uploadId, int partNumber, InputStream data, long partSize) throws Exception;

    /**
     * Assembles all uploaded parts into the final object and persists metadata to MongoDB.
     *
     * @param request contains the {@code uploadId}, {@code objectName}, {@code bucket}, and ordered list of completed parts
     * @return DTO with the assembled file's ETag, filename, size, and access URL
     * @throws Exception if the backend rejects the complete request or any part is missing
     */
    FileDocumentDto completeMultipartUpload(MultipartCompleteRequestDto request) throws Exception;

    /**
     * Cancels an in-progress multipart upload and releases all uploaded parts from storage.
     *
     * @param bucket     bucket that holds the in-progress upload
     * @param objectName full object key of the in-progress upload
     * @param uploadId   upload session ID to abort
     * @throws Exception if the abort request fails
     */
    void abortMultipartUpload(String bucket, String objectName, String uploadId) throws Exception;

    /**
     * Generates a time-limited presigned PUT URL for direct browser-to-storage upload.
     *
     * <p>After the client completes the PUT, call {@link #confirmPresignedUpload} to persist metadata.
     *
     * @param filename    original filename including extension
     * @param fileType    logical category used as a path prefix
     * @param contentType MIME type of the file
     * @param isPublic    {@code true} for a public file; {@code false} for a private file
     * @return DTO containing the presigned URL, object key, bucket, and expiry timestamp
     * @throws Exception if URL generation fails
     */
    PresignedUploadResponseDto generatePresignedUploadUrl(String filename, String fileType, String contentType, boolean isPublic) throws Exception;

    /**
     * Confirms a completed presigned upload by fetching object metadata from storage and persisting a {@code FileDocument}.
     *
     * @param confirm DTO containing the {@code objectName}, {@code bucket}, original filename, and visibility flag
     * @return DTO with the persisted file's ETag, filename, size, and access URL
     * @throws Exception if the object is not found or metadata persistence fails
     */
    FileDocumentDto confirmPresignedUpload(PresignedUploadConfirmDto confirm) throws Exception;

    /**
     * Downloads a byte range of a file, supporting HTTP 206 Partial Content.
     *
     * @param etag       ETag of the file to download
     * @param rangeStart first byte offset (inclusive)
     * @param rangeEnd   last byte offset (inclusive), or {@code -1} to read to end of file
     * @return result holder with the data stream, range boundaries, and total file size; {@code null} if not found
     * @throws Exception if the download fails
     */
    RangeDownloadResult downloadFileRange(String etag, long rangeStart, long rangeEnd) throws Exception;

    /**
     * Downloads a specific version of a file.
     *
     * <p>Requires versioning to be enabled on the bucket.
     *
     * @param etag      ETag of the file
     * @param versionId version ID as returned by {@link #listFileVersions}
     * @return raw {@link InputStream} of the versioned file bytes, or {@code null} if not found
     * @throws Exception if the download fails
     */
    InputStream downloadFileEtagWithVersion(String etag, String versionId) throws Exception;

    /**
     * Lists all stored versions of a file in reverse-chronological order.
     *
     * @param etag ETag of the file whose versions to list
     * @return ordered list of {@link FileVersionDto}; the latest version has {@code isLatest=true}
     * @throws Exception if the backend query fails
     */
    List<FileVersionDto> listFileVersions(String etag) throws Exception;

    /**
     * Performs a server-side copy of an existing file without re-uploading from the client.
     *
     * @param request contains the source ETag, optional destination bucket, and optional destination filename
     * @return DTO with the new file's ETag, object key, bucket, and access URL
     * @throws Exception if the source file is not found or the copy operation fails
     */
    ObjectCopyResponseDto copyObject(ObjectCopyRequestDto request) throws Exception;

    /**
     * Moves a file by performing a server-side copy followed by soft-archiving the source in MongoDB.
     *
     * @param request contains the source ETag, optional destination bucket, and optional destination filename
     * @return DTO with the moved file's ETag, object key, bucket, and access URL
     * @throws Exception if the source file is not found or the move operation fails
     */
    ObjectCopyResponseDto moveObject(ObjectCopyRequestDto request) throws Exception;
}
