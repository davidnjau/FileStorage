package com.dave.filestorage.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Represents one completed part of a multipart upload.
 */
@Schema(description = "Represents one completed part of a multipart upload")
public class CompletedPartDto {

    @Schema(description = "1-based part index", minimum = "1")
    private int partNumber;

    @Schema(description = "ETag returned by the server when the part was uploaded")
    private String eTag;

    public CompletedPartDto() {
    }

    public CompletedPartDto(int partNumber, String eTag) {
        this.partNumber = partNumber;
        this.eTag = eTag;
    }

    public int getPartNumber() {
        return partNumber;
    }

    public void setPartNumber(int partNumber) {
        this.partNumber = partNumber;
    }

    public String getETag() {
        return eTag;
    }

    public void setETag(String eTag) {
        this.eTag = eTag;
    }
}
