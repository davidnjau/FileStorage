package com.dave.filestorage.storage.minio;

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
public class MinioFileMetadataWorker {

    @Autowired
    private FileDocumentService fileDocumentService;

    @Autowired
    private MinioClient minioClient;

    @Async
    public void persistMetadataAsync(FileDocument doc) {
        Map<String, String> tags = new HashMap<>();
        tags.put("archived", "false");

        try {
            fileDocumentService.save(doc);
            tags.put("status", "success");
            tagObjectInMinIO(doc, tags);

        } catch (Exception persistenceEx) {
            tags.put("status", "failed");

            try {
                tagObjectInMinIO(doc, tags);
            } catch (Exception taggingEx) {
                System.err.printf("Failed to tag MinIO object [%s]: %s%n", doc.getObjectName(), taggingEx.getMessage());
            }

            System.err.printf("Failed to persist metadata for [%s]: %s%n", doc.getObjectName(), persistenceEx.getMessage());
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
