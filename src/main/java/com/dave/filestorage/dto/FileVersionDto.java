package com.dave.filestorage.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Date;

/**
 * One version of a stored file.
 */
@Schema(description = "One version of a stored file")
public class FileVersionDto {

    @Schema(description = "Opaque version identifier assigned by the storage backend")
    private String versionId;

    @Schema(description = "When this version was stored")
    private Date lastModified;

    @Schema(description = "Size of this version in bytes")
    private long size;

    @Schema(description = "ETag of this version")
    private String etag;

    @Schema(description = "True if this is the current (most recent) version")
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
