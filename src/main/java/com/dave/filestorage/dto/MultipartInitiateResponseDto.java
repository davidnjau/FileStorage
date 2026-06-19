package com.dave.filestorage.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response from initiating a multipart upload.
 */
@Schema(description = "Response from initiating a multipart upload")
public class MultipartInitiateResponseDto {

    @Schema(description = "Opaque upload ID to reference in subsequent part uploads and completion")
    private String uploadId;

    @Schema(description = "Full object key path that will be used in the storage backend")
    private String objectName;

    @Schema(description = "Target bucket name")
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
