package com.dave.filestorage.storage;

import com.dave.filestorage.dto.WebhookConfigDto;
import java.util.List;

/**
 * Provider-agnostic contract for S3 event webhook management.
 *
 * <p>MinIO implementation uses a live SSE stream via {@code listenBucketNotification}.
 * Garage implementation uses scheduled polling (configurable interval via
 * {@code storage.notification.polling.enabled=true}).
 */
public interface NotificationService {

    /**
     * Registers an HTTP endpoint to receive notifications when objects are created or removed in a bucket.
     *
     * @param config DTO containing the bucket name, webhook URL, and list of S3 event patterns to subscribe to
     * @return the persisted webhook configuration, including any generated identifier
     */
    WebhookConfigDto registerWebhook(WebhookConfigDto config);

    /**
     * Marks a webhook registration as inactive so no further events are delivered.
     *
     * @param id identifier of the webhook registration to deregister
     */
    void deregisterWebhook(String id);

    /**
     * Returns all active webhook registrations, optionally filtered by bucket.
     *
     * @param bucket bucket name to filter by, or {@code null} to return webhooks for all buckets
     * @return list of active {@link WebhookConfigDto} registrations
     */
    List<WebhookConfigDto> listWebhooks(String bucket);
}
