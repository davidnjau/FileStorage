package com.dave.filestorage.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.InputStream;

/**
 * Internal result holder for a partial content (byte-range) download.
 * Not exposed directly in API responses.
 */
@Schema(description = "Internal result holder for a partial content (byte-range) download — not exposed directly in responses")
public class RangeDownloadResult {

    private InputStream data;
    private long contentLength;
    private long rangeStart;
    private long rangeEnd;
    private long totalSize;

    public RangeDownloadResult() {
    }

    public RangeDownloadResult(InputStream data, long contentLength, long rangeStart, long rangeEnd, long totalSize) {
        this.data = data;
        this.contentLength = contentLength;
        this.rangeStart = rangeStart;
        this.rangeEnd = rangeEnd;
        this.totalSize = totalSize;
    }

    public InputStream getData() {
        return data;
    }

    public void setData(InputStream data) {
        this.data = data;
    }

    public long getContentLength() {
        return contentLength;
    }

    public void setContentLength(long contentLength) {
        this.contentLength = contentLength;
    }

    public long getRangeStart() {
        return rangeStart;
    }

    public void setRangeStart(long rangeStart) {
        this.rangeStart = rangeStart;
    }

    public long getRangeEnd() {
        return rangeEnd;
    }

    public void setRangeEnd(long rangeEnd) {
        this.rangeEnd = rangeEnd;
    }

    public long getTotalSize() {
        return totalSize;
    }

    public void setTotalSize(long totalSize) {
        this.totalSize = totalSize;
    }
}
