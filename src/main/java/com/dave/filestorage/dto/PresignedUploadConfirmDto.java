package com.dave.filestorage.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Confirm a completed direct upload and persist metadata.
 */
@Schema(description = "Confirm a completed direct upload and persist metadata")
public class PresignedUploadConfirmDto {

    @Schema(description = "Object key from the presigned upload response")
    private String objectName;

    @Schema(description = "Bucket from the presigned upload response")
    private String bucket;

    @Schema(description = "Original filename to store in metadata")
    private String originalFilename;

    @Schema(description = "Visibility used when generating the presigned URL")
    private Boolean isPublic;

    public PresignedUploadConfirmDto() {
    }

    public PresignedUploadConfirmDto(String objectName, String bucket, String originalFilename, Boolean isPublic) {
        this.objectName = objectName;
        this.bucket = bucket;
        this.originalFilename = originalFilename;
        this.isPublic = isPublic;
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

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public Boolean getIsPublic() {
        return isPublic;
    }

    public void setIsPublic(Boolean isPublic) {
        this.isPublic = isPublic;
    }
}
