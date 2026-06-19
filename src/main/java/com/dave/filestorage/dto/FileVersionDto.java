package com.dave.filestorage.dto;

import java.util.Date;

public class FileVersionDto {

    private String versionId;
    private Date lastModified;
    private long size;
    private String etag;
    private boolean isLatest;

    public FileVersionDto() {
    }

    public FileVersionDto(String versionId, Date lastModified, long size, String etag, boolean isLatest) {
        this.versionId = versionId;
        this.lastModified = lastModified;
        this.size = size;
        this.etag = etag;
        this.isLatest = isLatest;
    }

    public String getVersionId() {
        return versionId;
    }

    public void setVersionId(String versionId) {
        this.versionId = versionId;
    }

    public Date getLastModified() {
        return lastModified;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getEtag() {
        return etag;
    }

    public void setEtag(String etag) {
        this.etag = etag;
    }

    public boolean isLatest() {
        return isLatest;
    }

    public void setLatest(boolean latest) {
        isLatest = latest;
    }
}
