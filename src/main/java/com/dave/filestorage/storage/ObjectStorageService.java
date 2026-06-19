package com.dave.filestorage.storage;

import com.dave.filestorage.dto.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;

public interface ObjectStorageService {

    FileDocumentDto uploadFile(MultipartFile file, String fileType, boolean isPublic) throws Exception;

    InputStream downloadFileEtag(String etag) throws Exception;

    FileDocumentDto getFileDocumentInformation(String id);

    // Multipart
    MultipartInitiateResponseDto initiateMultipartUpload(String filename, String fileType, String contentType, boolean isPublic) throws Exception;

    String uploadPart(String bucket, String objectName, String uploadId, int partNumber, InputStream data, long partSize) throws Exception;

    FileDocumentDto completeMultipartUpload(MultipartCompleteRequestDto request) throws Exception;

    void abortMultipartUpload(String bucket, String objectName, String uploadId) throws Exception;

    // Presigned PUT
    PresignedUploadResponseDto generatePresignedUploadUrl(String filename, String fileType, String contentType, boolean isPublic) throws Exception;

    FileDocumentDto confirmPresignedUpload(PresignedUploadConfirmDto confirm) throws Exception;

    // Range download
    RangeDownloadResult downloadFileRange(String etag, long rangeStart, long rangeEnd) throws Exception;

    // Versioning
    InputStream downloadFileEtagWithVersion(String etag, String versionId) throws Exception;

    List<FileVersionDto> listFileVersions(String etag) throws Exception;

    // Copy/Move
    ObjectCopyResponseDto copyObject(ObjectCopyRequestDto request) throws Exception;

    ObjectCopyResponseDto moveObject(ObjectCopyRequestDto request) throws Exception;
}
