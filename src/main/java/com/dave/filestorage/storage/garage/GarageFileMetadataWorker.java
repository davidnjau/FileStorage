package com.dave.filestorage.storage.garage;

import com.dave.filestorage.db.FileDocument;
import com.dave.filestorage.db.FileDocumentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectTaggingRequest;
import software.amazon.awssdk.services.s3.model.Tag;
import software.amazon.awssdk.services.s3.model.Tagging;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@ConditionalOnProperty(name = "storage.provider", havingValue = "garage")
public class GarageFileMetadataWorker {

    private static final Logger log = LoggerFactory.getLogger(GarageFileMetadataWorker.class);

    @Autowired
    private FileDocumentService fileDocumentService;

    @Autowired
    private S3Client garageS3Client;

    @Async
    public void persistMetadataAsync(FileDocument doc) {
        Map<String, String> tags = new HashMap<>();
        tags.put("archived", "false");

        try {
            fileDocumentService.save(doc);
            tags.put("status", "success");
            tagObjectInGarage(doc, tags);

        } catch (Exception persistenceEx) {
            tags.put("status", "failed");

            try {
                tagObjectInGarage(doc, tags);
            } catch (Exception taggingEx) {
                log.error("Failed to tag Garage object [{}]: {}", doc.getObjectName(), taggingEx.getMessage());
            }

            log.error("Failed to persist metadata for [{}]: {}", doc.getObjectName(), persistenceEx.getMessage(), persistenceEx);
        }
    }

    private void tagObjectInGarage(FileDocument doc, Map<String, String> tags) {
        List<Tag> tagList = tags.entrySet().stream()
                .map(e -> Tag.builder().key(e.getKey()).value(e.getValue()).build())
                .collect(Collectors.toList());

        garageS3Client.putObjectTagging(PutObjectTaggingRequest.builder()
                .bucket(doc.getBucket())
                .key(doc.getObjectName())
                .tagging(Tagging.builder().tagSet(tagList).build())
                .build());
    }
}
