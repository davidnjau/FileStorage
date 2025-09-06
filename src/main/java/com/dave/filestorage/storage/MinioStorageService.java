package com.dave.filestorage.storage;

import com.dave.filestorage.dto.FileDocumentDto;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

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
}