package com.dave.filestorage.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * Request body to finalise a multipart upload.
 */
@Schema(description = "Request body to finalise a multipart upload")
public class MultipartCompleteRequestDto {

    @Schema(description = "Upload ID from the initiate response")
    private String uploadId;

    @Schema(description = "Object key from the initiate response")
    private String objectName;

    @Schema(description = "Bucket from the initiate response")
    private String bucket;

    @Schema(description = "All completed parts in ascending partNumber order")
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
