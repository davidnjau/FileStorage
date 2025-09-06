package com.dave.filestorage.dto;

import java.util.Date;

public class FileDocumentDto {

    private String fileId; //This will be the eTag from AWS S3 or minIO S3 bucket
    private String fileName;
    private Date uploadedAt;
    private long size;
    private String url;

    public FileDocumentDto() {
    }

    public FileDocumentDto(String fileId, String fileName, Date uploadedAt, long size, String url) {
        this.fileId = fileId;
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