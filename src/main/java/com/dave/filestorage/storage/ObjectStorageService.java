package com.dave.filestorage.storage;

import com.dave.filestorage.dto.FileDocumentDto;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

public interface ObjectStorageService {

    FileDocumentDto uploadFile(MultipartFile file, String fileType, boolean isPublic) throws Exception;

    InputStream downloadFileEtag(String etag) throws Exception;

    FileDocumentDto getFileDocumentInformation(String id);
}
