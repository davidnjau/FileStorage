package com.dave.filestorage.db;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.Map;

/**
 * MongoDB document storing file metadata alongside the binary stored in S3.
 */
@Schema(description = "MongoDB document storing file metadata alongside the binary stored in S3")
@Document(collection = "file_storage_metadata")
public class FileDocument {

    @Id
    @Indexed
    @Schema(description = "MongoDB document ID")
    private String id;

    @Schema(description = "Filename as provided by the uploader")
    private String originalFilename;

    @Schema(description = "Full S3 object key including path prefix")
    private String objectName;

    @Schema(description = "S3 bucket name")
    private String bucket;

    @Schema(description = "File size in bytes")
    private long size;

    @Schema(description = "MIME content type")
    private String contentType;

    @Indexed
    @Schema(description = "S3 ETag / content hash")
    private String etag;

    @Schema(description = "Last-modified timestamp from the storage backend")
    private Date lastModified;

    private String presignedUrl;

    @Schema(description = "When the file was uploaded to this service")
    private Date uploadedAt;

    @Schema(description = "Expiry time of the presigned URL (private files only)")
    private Date expiryDateTime;

    @Schema(description = "Soft-delete flag — archived files are excluded from URL refresh queries")
    private boolean archived = false;

    private String uploadedBy;
    private Map<String, String> customMetadata;
    private String fileUrl;

    @Schema(description = "Whether the file is publicly accessible without signing")
    private boolean isPublic;

    @Schema(description = "Presigned URL lifetime in hours")
    private Integer expiryHourTime;

    @Schema(description = "Multipart upload ID (present during in-progress multipart uploads)")
    private String uploadId;

    @Schema(description = "Multipart upload lifecycle: in_progress | completed | aborted")
    private String uploadStatus;

    private Integer totalParts;

    @Schema(description = "S3 version ID (populated when bucket versioning is enabled)")
    private String versionId;

    @Schema(description = "Server-side encryption algorithm, e.g. AES256 (null if not encrypted)")
    private String sseAlgorithm;

    // Getters, Setters, Constructors


    public FileDocument() {
    }

    /**
     * Constructs a new FileDocument with the specified details.
     *
     * @param id the unique identifier of the file document
     * @param originalFilename the original name of the file
     * @param objectName the name of the object in storage
     * @param bucket the storage bucket where the file is stored
     * @param size the size of the file in bytes
     * @param contentType the MIME type of the file
     * @param etag the entity tag for the file
     * @param lastModified the date when the file was last modified
     * @param presignedUrl the presigned URL for accessing the file
     * @param uploadedAt the date when the file was uploaded
     * @param uploadedBy the identifier of the user who uploaded the file
     * @param customMetadata additional metadata associated with the file
     */
    public FileDocument(String id, String originalFilename, String objectName, String bucket,
                        long size, String contentType, String etag, Date lastModified,
                        String presignedUrl, Date uploadedAt, String uploadedBy,
                        Map<String, String> customMetadata, String fileUrl) {
        this.id = id;
        this.originalFilename = originalFilename;
        this.objectName = objectName;
        this.bucket = bucket;
        this.size = size;
        this.contentType = contentType;
        this.etag = etag;
        this.lastModified = lastModified;
        this.presignedUrl = presignedUrl;
        this.uploadedAt = uploadedAt;
        this.uploadedBy = uploadedBy;
        this.customMetadata = customMetadata;
        this.fileUrl = fileUrl;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public String getObjectName() {
        return objectName;
    }

    public void setObjectName(String objectName) {
        this.objectName = objectName;
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getEtag() {
        return etag;
    }

    public void setEtag(String etag) {
        this.etag = etag;
    }

    public Date getLastModified() {
        return lastModified;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    public String getPresignedUrl() {
        return presignedUrl;
    }

    public void setPresignedUrl(String presignedUrl) {
        this.presignedUrl = presignedUrl;
    }

    public Date getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(Date uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public String getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(String uploadedBy) {
        this.uploadedBy = uploadedBy;
    }

    public Map<String, String> getCustomMetadata() {
        return customMetadata;
    }

    public void setCustomMetadata(Map<String, String> customMetadata) {
        this.customMetadata = customMetadata;
    }

    public boolean isArchived() {
        return archived;
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public void setPublic(boolean aPublic) {
        isPublic = aPublic;
    }

    public Date getExpiryDateTime() {
        return expiryDateTime;
    }

    public void setExpiryDateTime(Date expiryDateTime) {
        this.expiryDateTime = expiryDateTime;
    }

    public Integer getExpiryHourTime() {
        return expiryHourTime;
    }

    public void setExpiryHourTime(Integer expiryHourTime) {
        this.expiryHourTime = expiryHourTime;
    }

    public String getUploadId() {
        return uploadId;
    }

    public void setUploadId(String uploadId) {
        this.uploadId = uploadId;
    }

    public String getUploadStatus() {
        return uploadStatus;
    }

    public void setUploadStatus(String uploadStatus) {
        this.uploadStatus = uploadStatus;
    }

    public Integer getTotalParts() {
        return totalParts;
    }

    public void setTotalParts(Integer totalParts) {
        this.totalParts = totalParts;
    }

    public String getVersionId() {
        return versionId;
    }

    public void setVersionId(String versionId) {
        this.versionId = versionId;
    }

    public String getSseAlgorithm() {
        return sseAlgorithm;
    }

    public void setSseAlgorithm(String sseAlgorithm) {
        this.sseAlgorithm = sseAlgorithm;
    }
}