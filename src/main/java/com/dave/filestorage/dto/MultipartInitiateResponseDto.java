package com.dave.filestorage.dto;

public class MultipartInitiateResponseDto {

    private String uploadId;
    private String objectName;
    private String bucket;

    public MultipartInitiateResponseDto() {
    }

    public MultipartInitiateResponseDto(String uploadId, String objectName, String bucket) {
        this.uploadId = uploadId;
        this.objectName = objectName;
        this.bucket = bucket;
    }

    public String getUploadId() {
        return uploadId;
    }

    public void setUploadId(String uploadId) {
        this.uploadId = uploadId;
    }

    public String getObjectName() {
        return objectName;
    }

    public void setObjectName(String objectName) {
        this.objectName = objectName;
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }
}
