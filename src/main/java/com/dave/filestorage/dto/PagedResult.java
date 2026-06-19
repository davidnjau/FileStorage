package com.dave.filestorage.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Paginated list result")
public class PagedResult<T> {

    @Schema(description = "Items on this page")
    private final List<T> content;

    @Schema(description = "Zero-based page number")
    private final int page;

    @Schema(description = "Maximum items per page")
    private final int size;

    @Schema(description = "Total matching items across all pages")
    private final long totalElements;

    @Schema(description = "Total number of pages")
    private final int totalPages;

    public PagedResult(List<T> content, int page, int size, long totalElements) {
        this.content = content;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = size == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
    }

    public List<T> getContent() { return content; }
    public int getPage() { return page; }
    public int getSize() { return size; }
    public long getTotalElements() { return totalElements; }
    public int getTotalPages() { return totalPages; }
    public boolean isHasNext() { return page < totalPages - 1; }
}
