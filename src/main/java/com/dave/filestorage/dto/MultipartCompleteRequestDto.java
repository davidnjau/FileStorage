package com.dave.filestorage.dto;

import java.util.List;

public class MultipartCompleteRequestDto {

    private String uploadId;
    private String objectName;
    private String bucket;
    private List<CompletedPartDto> parts;

    public MultipartCompleteRequestDto() {
    }

    public MultipartCompleteRequestDto(String uploadId, String objectName, String bucket, List<CompletedPartDto> parts) {
        this.uploadId = uploadId;
        this.objectName = objectName;
        this.bucket = bucket;
        this.parts = parts;
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

    public List<CompletedPartDto> getParts() {
        return parts;
    }

    public void setParts(List<CompletedPartDto> parts) {
        this.parts = parts;
    }
}
