package com.dave.filestorage.dto;

public class ObjectCopyRequestDto {

    private String sourceEtag;
    private String destinationBucket;
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
