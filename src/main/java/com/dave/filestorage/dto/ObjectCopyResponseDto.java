package com.dave.filestorage.dto;

public class ObjectCopyResponseDto {

    private String newEtag;
    private String newObjectName;
    private String bucket;
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
