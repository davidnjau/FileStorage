package com.dave.filestorage.dto;

import java.util.Date;

public class PresignedUploadResponseDto {

    private String uploadUrl;
    private String objectName;
    private String bucket;
    private Date expiresAt;

    public PresignedUploadResponseDto() {
    }

    public PresignedUploadResponseDto(String uploadUrl, String objectName, String bucket, Date expiresAt) {
        this.uploadUrl = uploadUrl;
        this.objectName = objectName;
        this.bucket = bucket;
        this.expiresAt = expiresAt;
    }

    public String getUploadUrl() {
        return uploadUrl;
    }

    public void setUploadUrl(String uploadUrl) {
        this.uploadUrl = uploadUrl;
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

    public Date getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Date expiresAt) {
        this.expiresAt = expiresAt;
    }
}
