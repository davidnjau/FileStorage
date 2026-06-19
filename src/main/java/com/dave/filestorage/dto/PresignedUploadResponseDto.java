package com.dave.filestorage.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Date;

/**
 * Presigned PUT URL for direct upload from a browser or mobile client.
 */
@Schema(description = "Presigned PUT URL for direct upload from a browser or mobile client")
public class PresignedUploadResponseDto {

    @Schema(description = "PUT this URL directly with the file bytes — no auth headers required")
    private String uploadUrl;

    @Schema(description = "Pass this back to the confirm endpoint after upload")
    private String objectName;

    @Schema(description = "Bucket the file will land in")
    private String bucket;

    @Schema(description = "URL expiry timestamp — upload must complete before this time")
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
