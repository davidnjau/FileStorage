package com.dave.filestorage.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request a server-side copy or move of an existing file.
 */
@Schema(description = "Request a server-side copy or move of an existing file")
public class ObjectCopyRequestDto {

    @Schema(description = "ETag of the source file to copy")
    private String sourceEtag;

    @Schema(description = "Target bucket — defaults to the source bucket if omitted")
    private String destinationBucket;

    @Schema(description = "New filename — defaults to the source filename if omitted")
    private String destinationFilename;

    public ObjectCopyRequestDto() {
    }

    public ObjectCopyRequestDto(String sourceEtag, String destinationBucket, String destinationFilename) {
        this.sourceEtag = sourceEtag;
        this.destinationBucket = destinationBucket;
        this.destinationFilename = destinationFilename;
    }

    public String getSourceEtag() {
        return sourceEtag;
    }

    public void setSourceEtag(String sourceEtag) {
        this.sourceEtag = sourceEtag;
    }

    public String getDestinationBucket() {
        return destinationBucket;
    }

    public void setDestinationBucket(String destinationBucket) {
        this.destinationBucket = destinationBucket;
    }

    public String getDestinationFilename() {
        return destinationFilename;
    }

    public void setDestinationFilename(String destinationFilename) {
        this.destinationFilename = destinationFilename;
    }
}
