package com.dave.filestorage.db;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataMongoTest
@Testcontainers
class NotificationWebhookConfigRepositoryTest {

    @Container
    static MongoDBContainer mongo = new MongoDBContainer("mongo:6");

    @DynamicPropertySource
    static void mongoProps(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongo::getReplicaSetUrl);
    }

    @Autowired
    NotificationWebhookConfigRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void findByBucketAndActiveTrue_returnsOnlyActive() {
        repository.save(webhook("bucket-a", "https://example.com/hook1", true));
        repository.save(webhook("bucket-a", "https://example.com/hook2", false));
        repository.save(webhook("bucket-b", "https://example.com/hook3", true));

        List<NotificationWebhookConfig> results = repository.findByBucketAndActiveTrue("bucket-a");
        assertEquals(1, results.size());
        assertEquals("https://example.com/hook1", results.get(0).getWebhookUrl());
    }

    @Test
    void findByActiveTrue_returnsAllActiveAcrossBuckets() {
        repository.save(webhook("bucket-a", "https://example.com/hook1", true));
        repository.save(webhook("bucket-b", "https://example.com/hook2", true));
        repository.save(webhook("bucket-c", "https://example.com/hook3", false));

        List<NotificationWebhookConfig> results = repository.findByActiveTrue();
        assertEquals(2, results.size());
    }

    @Test
    void deactivate_webhookNoLongerReturnedInActiveQuery() {
        NotificationWebhookConfig saved = repository.save(
                webhook("bucket-a", "https://example.com/hook", true));

        saved.setActive(false);
        repository.save(saved);

        List<NotificationWebhookConfig> results = repository.findByBucketAndActiveTrue("bucket-a");
        assertTrue(results.isEmpty());
    }

    private NotificationWebhookConfig webhook(String bucket, String url, boolean active) {
        NotificationWebhookConfig cfg = new NotificationWebhookConfig();
        cfg.setBucket(bucket);
        cfg.setWebhookUrl(url);
        cfg.setEvents(Arrays.asList("s3:ObjectCreated:*"));
        cfg.setActive(active);
        return cfg;
    }
}
