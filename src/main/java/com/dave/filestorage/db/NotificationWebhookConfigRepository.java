package com.dave.filestorage.db;

import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface NotificationWebhookConfigRepository extends MongoRepository<NotificationWebhookConfig, String> {
    List<NotificationWebhookConfig> findByBucketAndActiveTrue(String bucket);
    List<NotificationWebhookConfig> findByActiveTrue();
}
