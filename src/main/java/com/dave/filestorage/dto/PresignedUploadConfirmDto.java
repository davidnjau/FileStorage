package com.dave.filestorage.dto;

public class PresignedUploadConfirmDto {

    private String objectName;
    private String bucket;
    private String originalFilename;
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
