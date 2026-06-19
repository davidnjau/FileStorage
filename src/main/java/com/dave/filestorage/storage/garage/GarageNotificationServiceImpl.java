package com.dave.filestorage.storage.garage;

import com.dave.filestorage.db.GaragePollState;
import com.dave.filestorage.db.GaragePollStateRepository;
import com.dave.filestorage.db.NotificationWebhookConfig;
import com.dave.filestorage.db.NotificationWebhookConfigRepository;
import com.dave.filestorage.dto.WebhookConfigDto;
import com.dave.filestorage.storage.NotificationService;
import com.dave.filestorage.util.WebhookUrlValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.util.*;
import java.util.stream.Collectors;

@Service
@ConditionalOnProperty(name = "storage.provider", havingValue = "garage")
public class GarageNotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(GarageNotificationServiceImpl.class);

    @Autowired
    private NotificationWebhookConfigRepository webhookConfigRepository;

    @Autowired
    private GaragePollStateRepository pollStateRepository;

    @Autowired
    private S3Client garageS3Client;

    @org.springframework.beans.factory.annotation.Value("${storage.notification.polling.enabled:false}")
    private boolean pollingEnabled;

    private final RestTemplate restTemplate;

    public GarageNotificationServiceImpl() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
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
                .map(c -> {
                    WebhookConfigDto dto = new WebhookConfigDto(c.getBucket(), c.getWebhookUrl(), c.getEvents());
                    dto.setId(c.getId());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Scheduled(fixedDelayString = "${storage.notification.polling.interval-ms:30000}")
    public void pollForChanges() {
        if (!pollingEnabled) return;

        List<String> buckets = webhookConfigRepository.findByActiveTrue().stream()
                .map(NotificationWebhookConfig::getBucket)
                .distinct()
                .collect(Collectors.toList());

        for (String bucket : buckets) {
            try {
                Set<String> currentKeys = listAllObjectKeys(bucket);
                Optional<GaragePollState> stateOpt = pollStateRepository.findByBucket(bucket);

                if (stateOpt.isPresent()) {
                    Set<String> previousKeys = stateOpt.get().getObjectKeys();

                    Set<String> created = new HashSet<>(currentKeys);
                    created.removeAll(previousKeys);

                    Set<String> deleted = new HashSet<>(previousKeys);
                    deleted.removeAll(currentKeys);

                    if (!created.isEmpty() || !deleted.isEmpty()) {
                        List<NotificationWebhookConfig> hooks =
                                webhookConfigRepository.findByBucketAndActiveTrue(bucket);
                        created.forEach(key -> notifyWebhooks(hooks, bucket, key, "ObjectCreated"));
                        deleted.forEach(key -> notifyWebhooks(hooks, bucket, key, "ObjectRemoved"));
                        log.info("Garage poll [{}]: +{} created, -{} deleted",
                                bucket, created.size(), deleted.size());
                    }

                    GaragePollState state = stateOpt.get();
                    state.setObjectKeys(currentKeys);
                    state.setLastPolledAt(new Date());
                    pollStateRepository.save(state);
                } else {
                    pollStateRepository.save(new GaragePollState(bucket, currentKeys, new Date()));
                    log.info("Garage poll [{}]: initialised state with {} objects", bucket, currentKeys.size());
                }
            } catch (Exception e) {
                log.error("Garage polling error for bucket [{}]: {}", bucket, e.getMessage(), e);
            }
        }
    }

    private Set<String> listAllObjectKeys(String bucket) {
        Set<String> keys = new HashSet<>();
        String continuationToken = null;
        do {
            ListObjectsV2Request.Builder req = ListObjectsV2Request.builder().bucket(bucket);
            if (continuationToken != null) req.continuationToken(continuationToken);
            ListObjectsV2Response resp = garageS3Client.listObjectsV2(req.build());
            resp.contents().stream().map(S3Object::key).forEach(keys::add);
            continuationToken = resp.isTruncated() ? resp.nextContinuationToken() : null;
        } while (continuationToken != null);
        return keys;
    }

    private void notifyWebhooks(List<NotificationWebhookConfig> hooks, String bucket,
                                String objectKey, String eventType) {
        Map<String, String> payload = new HashMap<>();
        payload.put("bucket", bucket);
        payload.put("key", objectKey);
        payload.put("eventType", eventType);
        payload.put("timestamp", new Date().toString());

        hooks.forEach(hook -> postToWebhook(hook.getWebhookUrl(), payload));
    }

    private void postToWebhook(String url, Object payload) {
        try {
            restTemplate.postForEntity(url, payload, String.class);
        } catch (Exception e) {
            log.warn("Failed to post to webhook [{}]: {}", url, e.getMessage());
        }
    }
}
