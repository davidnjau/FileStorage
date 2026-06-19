package com.dave.filestorage.storage;


import com.dave.filestorage.db.FileDocument;
import com.dave.filestorage.db.FileDocumentService;
import io.minio.MinioClient;
import io.minio.SetObjectTagsArgs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "storage.provider", havingValue = "minio", matchIfMissing = true)
public class FileMetadataWorker {

    @Autowired
    private FileDocumentService fileDocumentService;

    @Autowired
    private MinioClient minioClient;

    /**
     * Asynchronously persists file metadata to MongoDB and updates the status tag in MinIO.
     * <p>
     * This method attempts to save the provided file document to a MongoDB database. If the operation
     * is successful, it tags the corresponding object in MinIO with a "success" status. If an exception
     * occurs during the save operation, it tags the object with a "failed" status instead.
     * </p>
     *
     * @param doc the file document containing metadata to be persisted. It must include the bucket name
     *            and object name for tagging in MinIO.
     */
    @Async
    public void persistMetadataAsync(FileDocument doc) {
        Map<String, String> tags = new HashMap<>();
        tags.put("archived", "false");

        try {
            // Persist metadata to MongoDB
            fileDocumentService.save(doc);
            tags.put("status", "success");

            // Tag in MinIO as success
            tagObjectInMinIO(doc, tags);

        } catch (Exception persistenceEx) {
            tags.put("status", "failed");

            try {
                // Tag in MinIO as failed
                tagObjectInMinIO(doc, tags);
            } catch (Exception taggingEx) {
                System.err.printf("❌ Failed to tag MinIO object [%s]: %s%n", doc.getObjectName(), taggingEx.getMessage());
            }

            System.err.printf("❌ Failed to persist metadata for [%s]: %s%n", doc.getObjectName(), persistenceEx.getMessage());
        }
    }

    private void tagObjectInMinIO(FileDocument doc, Map<String, String> tags) throws Exception {
        minioClient.setObjectTags(
                SetObjectTagsArgs.builder()
                        .bucket(doc.getBucket())
                        .object(doc.getObjectName())
                        .tags(tags)
                        .build()
        );
    }


}