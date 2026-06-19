package com.dave.filestorage.db;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.List;

@Document(collection = "notification_webhook_configs")
public class NotificationWebhookConfig {

    @Id
    private String id;
    private String bucket;
    private String webhookUrl;
    private List<String> events;
    private boolean active;

    public NotificationWebhookConfig() {
    }

    public NotificationWebhookConfig(String id, String bucket, String webhookUrl, List<String> events, boolean active) {
        this.id = id;
        this.bucket = bucket;
        this.webhookUrl = webhookUrl;
        this.events = events;
        this.active = active;
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

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
