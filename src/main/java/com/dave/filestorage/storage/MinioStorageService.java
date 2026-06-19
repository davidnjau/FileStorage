package com.dave.filestorage.storage;

import com.dave.filestorage.dto.FileDocumentDto;
import com.dave.filestorage.dto.FileVersionDto;
import com.dave.filestorage.dto.MultipartCompleteRequestDto;
import com.dave.filestorage.dto.MultipartInitiateResponseDto;
import com.dave.filestorage.dto.ObjectCopyRequestDto;
import com.dave.filestorage.dto.ObjectCopyResponseDto;
import com.dave.filestorage.dto.PresignedUploadConfirmDto;
import com.dave.filestorage.dto.PresignedUploadResponseDto;
import com.dave.filestorage.dto.RangeDownloadResult;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;

public interface MinioStorageService {
    /**
     * Uploads a file to the Minio storage service.
     *
     * @param file the file to be uploaded, represented as a MultipartFile
     * @param fileType the type of the file being uploaded, used for categorization
     * @return a FileDocumentDto containing metadata about the uploaded file
     * @throws Exception if an error occurs during the upload process
     */
    FileDocumentDto uploadFile(MultipartFile file, String fileType, boolean isPublic) throws Exception;

    /**
     * Downloads a file from the Minio storage service using its ETag.
     *
     * @param etag the ETag of the file to be downloaded, used as a unique identifier
     * @return an InputStream to read the contents of the downloaded file
     * @throws Exception if an error occurs during the download process
     */
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
