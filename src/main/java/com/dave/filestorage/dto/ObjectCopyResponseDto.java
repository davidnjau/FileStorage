package com.dave.filestorage.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Result of a server-side copy or move operation.
 */
@Schema(description = "Result of a server-side copy or move operation")
public class ObjectCopyResponseDto {

    @Schema(description = "ETag of the newly created copy")
    private String newEtag;

    @Schema(description = "Full object key of the copy in the storage backend")
    private String newObjectName;

    @Schema(description = "Bucket where the copy was created")
    private String bucket;

    @Schema(description = "Access URL of the copy")
    private String url;

    public ObjectCopyResponseDto() {
    }

    public ObjectCopyResponseDto(String newEtag, String newObjectName, String bucket, String url) {
        this.newEtag = newEtag;
        this.newObjectName = newObjectName;
        this.bucket = bucket;
        this.url = url;
    }

    public String getNewEtag() {
        return newEtag;
    }

    public void setNewEtag(String newEtag) {
        this.newEtag = newEtag;
    }

    public String getNewObjectName() {
        return newObjectName;
    }

    public void setNewObjectName(String newObjectName) {
        this.newObjectName = newObjectName;
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
