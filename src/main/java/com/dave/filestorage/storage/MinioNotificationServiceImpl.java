package com.dave.filestorage.storage;

import com.dave.filestorage.db.NotificationWebhookConfig;
import com.dave.filestorage.db.NotificationWebhookConfigRepository;
import com.dave.filestorage.dto.WebhookConfigDto;
import io.minio.CloseableIterator;
import io.minio.ListenBucketNotificationArgs;
import io.minio.MinioClient;
import io.minio.Result;
import io.minio.messages.NotificationRecords;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MinioNotificationServiceImpl {

    @Autowired
    private MinioClient minioClient;

    @Autowired
    private NotificationWebhookConfigRepository webhookConfigRepository;

    @Value("${storage.notification.listener.enabled:false}")
    private boolean listenerEnabled;

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

    @Async
    @PostConstruct
    public void startListeners() {
        if (!listenerEnabled) return;
        webhookConfigRepository.findByActiveTrue().stream()
            .map(NotificationWebhookConfig::getBucket)
            .distinct()
            .forEach(this::listenBucket);
    }

    private void listenBucket(String bucket) {
        try {
            CloseableIterator<Result<NotificationRecords>> stream =
                minioClient.listenBucketNotification(
                    ListenBucketNotificationArgs.builder()
                        .bucket(bucket).prefix("").suffix("")
                        .events(new String[]{"s3:ObjectCreated:*", "s3:ObjectRemoved:*"})
                        .build());
            while (stream.hasNext()) {
                NotificationRecords records = stream.next().get();
                if (records.events() == null) continue;
                List<NotificationWebhookConfig> hooks =
                    webhookConfigRepository.findByBucketAndActiveTrue(bucket);
                records.events().forEach(event ->
                    hooks.forEach(hook -> postToWebhook(hook.getWebhookUrl(), event)));
            }
        } catch (Exception e) {
            System.err.printf("Notification listener error for bucket [%s]: %s%n",
                bucket, e.getMessage());
        }
    }

    private void postToWebhook(String url, Object payload) {
        try {
            restTemplate.postForEntity(url, payload, String.class);
        } catch (Exception e) {
            System.err.printf("Failed to post to webhook [%s]: %s%n", url, e.getMessage());
        }
    }
}
