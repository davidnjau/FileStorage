package com.dave.filestorage.storage.minio;

import com.dave.filestorage.db.FileDocument;
import com.dave.filestorage.db.FileDocumentService;
import io.minio.MinioClient;
import io.minio.SetObjectTagsArgs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "storage.provider", havingValue = "minio", matchIfMissing = true)
public class MinioFileMetadataWorker {

    private static final Logger log = LoggerFactory.getLogger(MinioFileMetadataWorker.class);

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
                log.error("Failed to tag MinIO object [{}]: {}", doc.getObjectName(), taggingEx.getMessage());
            }

            log.error("Failed to persist metadata for [{}]: {}", doc.getObjectName(), persistenceEx.getMessage(), persistenceEx);
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
