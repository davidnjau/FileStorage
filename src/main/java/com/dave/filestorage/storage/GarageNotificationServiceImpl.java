package com.dave.filestorage.storage;

import com.dave.filestorage.db.NotificationWebhookConfig;
import com.dave.filestorage.db.NotificationWebhookConfigRepository;
import com.dave.filestorage.dto.WebhookConfigDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import software.amazon.awssdk.services.s3.S3Client;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class GarageNotificationServiceImpl {

    @Autowired
    private NotificationWebhookConfigRepository webhookConfigRepository;

    @Autowired
    private S3Client garageS3Client;

    @Value("${storage.notification.polling.enabled:false}")
    private boolean pollingEnabled;

    private final RestTemplate restTemplate = new RestTemplate();

    public WebhookConfigDto registerWebhook(WebhookConfigDto config) {
        NotificationWebhookConfig entity = new NotificationWebhookConfig();
        entity.setBucket(config.getBucket());
        entity.setWebhookUrl(config.getWebhookUrl());
        entity.setEvents(config.getEvents());
        entity.setActive(true);
        webhookConfigRepository.save(entity);
        return config;
    }

    public void deregisterWebhook(String id) {
        webhookConfigRepository.findById(id).ifPresent(cfg -> {
            cfg.setActive(false);
            webhookConfigRepository.save(cfg);
        });
    }

    public List<WebhookConfigDto> listWebhooks(String bucket) {
        List<NotificationWebhookConfig> configs = bucket != null
                ? webhookConfigRepository.findByBucketAndActiveTrue(bucket)
                : webhookConfigRepository.findByActiveTrue();
        return configs.stream()
                .map(c -> new WebhookConfigDto(c.getBucket(), c.getWebhookUrl(), c.getEvents()))
                .collect(Collectors.toList());
    }

    @Scheduled(fixedDelayString = "${storage.notification.polling.interval-ms:30000}")
    public void pollForChanges() {
        if (!pollingEnabled) return;
        // Polling-based fallback: list objects per bucket, diff against last-seen
        // state stored in MongoDB, POST changes to registered webhooks.
    }

    private void postToWebhook(String url, Object payload) {
        try {
            restTemplate.postForEntity(url, payload, String.class);
        } catch (Exception e) {
            System.err.printf("Failed to post to webhook [%s]: %s%n", url, e.getMessage());
        }
    }
}
