package com.dave.filestorage.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * Webhook configuration for storage event notifications.
 */
@Schema(description = "Webhook configuration for storage event notifications")
public class WebhookConfigDto {

    @Schema(description = "Webhook registration ID")
    private String id;

    @NotBlank(message = "bucket is required")
    @Schema(description = "Bucket to monitor for events")
    private String bucket;

    @NotBlank(message = "webhookUrl is required")
    @Schema(description = "HTTP endpoint that will receive POST requests on matching events")
    private String webhookUrl;

    @NotEmpty(message = "events must not be empty")
    @Schema(description = "S3 event names to subscribe to", example = "[\"s3:ObjectCreated:*\", \"s3:ObjectRemoved:*\"]")
    private List<String> events;

    public WebhookConfigDto() {
    }

    public WebhookConfigDto(String bucket, String webhookUrl, List<String> events) {
        this.bucket = bucket;
        this.webhookUrl = webhookUrl;
        this.events = events;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public String getWebhookUrl() {
        return webhookUrl;
    }

    public void setWebhookUrl(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    public List<String> getEvents() {
        return events;
    }

    public void setEvents(List<String> events) {
        this.events = events;
    }
}
