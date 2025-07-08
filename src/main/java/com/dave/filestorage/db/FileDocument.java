package com.dave.filestorage.db;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.Map;

@Document(collection = "file_storage_metadata")
public class FileDocument {

    @Id
    private String id;

    private String originalFilename;
    private String objectName;
    private String bucket;
    private long size;
    private String contentType;

    @Indexed
    private String etag;
    private Date lastModified;
    private String presignedUrl;
    private Date uploadedAt;

    private boolean archived = false; // Optional: Add a flag for archiving

    // Optional: Add uploader ID or tags for tracking
    private String uploadedBy;
    private Map<String, String> customMetadata;

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
                        Map<String, String> customMetadata) {
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
}