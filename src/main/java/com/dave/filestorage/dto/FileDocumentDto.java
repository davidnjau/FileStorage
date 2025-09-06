package com.dave.filestorage.dto;

import java.util.Date;

public class FileDocumentDto {

    private String fileId; //This is the unique identifier for the file document from mongoDB
    private String eTagId; //This is the ETag of the file in MinIO
    private String fileName;
    private Date uploadedAt;
    private long size;
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