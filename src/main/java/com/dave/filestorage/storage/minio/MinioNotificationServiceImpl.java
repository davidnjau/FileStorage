package com.dave.filestorage.storage.minio;

import com.dave.filestorage.db.NotificationWebhookConfig;
import com.dave.filestorage.db.NotificationWebhookConfigRepository;
import com.dave.filestorage.dto.WebhookConfigDto;
import com.dave.filestorage.storage.NotificationService;
import com.dave.filestorage.util.WebhookUrlValidator;
import io.minio.CloseableIterator;
import io.minio.ListenBucketNotificationArgs;
import io.minio.MinioClient;
import io.minio.Result;
import io.minio.messages.NotificationRecords;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@ConditionalOnProperty(name = "storage.provider", havingValue = "minio", matchIfMissing = true)
public class MinioNotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(MinioNotificationServiceImpl.class);

    @Autowired
    private MinioClient minioClient;

    @Autowired
    private NotificationWebhookConfigRepository webhookConfigRepository;

    @Value("${storage.notification.listener.enabled:false}")
    private boolean listenerEnabled;

    private final ConcurrentHashMap<String, CloseableIterator<Result<NotificationRecords>>> activeListeners = new ConcurrentHashMap<>();

    private final RestTemplate restTemplate;

    public MinioNotificationServiceImpl() {
        org.springframework.http.client.SimpleClientHttpRequestFactory factory =
                new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);
        factory.setReadTimeout(10_000);
        this.restTemplate = new RestTemplate(factory);
    }

    @Override
    public WebhookConfigDto registerWebhook(WebhookConfigDto config) {
        WebhookUrlValidator.validate(config.getWebhookUrl());
        NotificationWebhookConfig entity = new NotificationWebhookConfig();
        entity.setBucket(config.getBucket());
        entity.setWebhookUrl(config.getWebhookUrl());
        entity.setEvents(config.getEvents());
        entity.setActive(true);
        NotificationWebhookConfig saved = webhookConfigRepository.save(entity);
        config.setId(saved.getId());
        return config;
    }

    @Override
    public void deregisterWebhook(String id) {
        webhookConfigRepository.findById(id).ifPresent(cfg -> {
            cfg.setActive(false);
            webhookConfigRepository.save(cfg);
        });
    }

    @Override
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

    @javax.annotation.PreDestroy
    public void stopListeners() {
        log.info("Stopping {} MinIO notification listeners", activeListeners.size());
        activeListeners.forEach((bucket, iterator) -> {
            try {
                iterator.close();
                log.info("Stopped listener for bucket [{}]", bucket);
            } catch (Exception e) {
                log.warn("Error closing listener for bucket [{}]: {}", bucket, e.getMessage());
            }
        });
        activeListeners.clear();
    }

    private void listenBucket(String bucket) {
        try {
            CloseableIterator<Result<NotificationRecords>> stream =
                minioClient.listenBucketNotification(
                    ListenBucketNotificationArgs.builder()
                        .bucket(bucket).prefix("").suffix("")
                        .events(new String[]{"s3:ObjectCreated:*", "s3:ObjectRemoved:*"})
                        .build());
            activeListeners.put(bucket, stream);
            while (stream.hasNext()) {
                NotificationRecords records = stream.next().get();
                if (records.events() == null) continue;
                List<NotificationWebhookConfig> hooks =
                    webhookConfigRepository.findByBucketAndActiveTrue(bucket);
                records.events().forEach(event ->
                    hooks.forEach(hook -> postToWebhook(hook.getWebhookUrl(), event)));
            }
        } catch (Exception e) {
            log.error("Notification listener error for bucket [{}]: {}", bucket, e.getMessage(), e);
        }
    }

    private void postToWebhook(String url, Object payload) {
        try {
            restTemplate.postForEntity(url, payload, String.class);
        } catch (Exception e) {
            log.warn("Failed to post to webhook [{}]: {}", url, e.getMessage());
        }
    }
}
