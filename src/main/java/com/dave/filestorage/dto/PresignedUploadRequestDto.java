package com.dave.filestorage.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Request a presigned PUT URL for direct browser-to-storage upload.
 */
@Schema(description = "Request a presigned PUT URL for direct browser-to-storage upload")
public class PresignedUploadRequestDto {

    @NotBlank(message = "filename is required")
    @Schema(description = "Original filename including extension", example = "report.pdf")
    private String filename;

    @NotBlank(message = "contentType is required")
    @Schema(description = "MIME type of the file", example = "application/pdf")
    private String contentType;

    @Schema(description = "Logical category used as a path prefix", example = "invoices")
    private String fileType;

    @Schema(description = "Whether the file should be publicly accessible without signing")
    private Boolean isPublic;

    public PresignedUploadRequestDto() {
    }

    public PresignedUploadRequestDto(String filename, String contentType, String fileType, Boolean isPublic) {
        this.filename = filename;
        this.contentType = contentType;
        this.fileType = fileType;
        this.isPublic = isPublic;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public Boolean getIsPublic() {
        return isPublic;
    }

    public void setIsPublic(Boolean isPublic) {
        this.isPublic = isPublic;
    }
}
