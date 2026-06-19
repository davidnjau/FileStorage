package com.dave.filestorage.dto;

public class PresignedUploadRequestDto {

    private String filename;
    private String contentType;
    private String fileType;
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
