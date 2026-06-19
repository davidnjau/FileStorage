package com.dave.filestorage.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Date;

/**
 * File metadata returned after a successful upload or URL refresh.
 */
@Schema(description = "File metadata returned after a successful upload or URL refresh")
public class FileDocumentDto {

    @Schema(description = "Internal document ID")
    private String fileId; //This is the unique identifier for the file document from mongoDB

    @Schema(description = "ETag (content hash) assigned by the storage backend")
    private String eTagId; //This is the ETag of the file in MinIO

    @Schema(description = "Original filename as uploaded by the client")
    private String fileName;

    @Schema(description = "Timestamp of last modification in the storage backend")
    private Date uploadedAt;

    @Schema(description = "File size in bytes")
    private long size;

    @Schema(description = "Access URL — direct for public files, presigned for private files")
    private String url;

    public FileDocumentDto() {
    }

    public FileDocumentDto(String fileId, String eTagId, String fileName, Date uploadedAt, long size, String url) {
        this.fileId = fileId;
        this.eTagId = eTagId;
        this.fileName = fileName;
        this.uploadedAt = uploadedAt;
        this.size = size;
        this.url = url;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public String geteTagId() {
        return eTagId;
    }

    public void seteTagId(String eTagId) {
        this.eTagId = eTagId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Date getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(Date uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}