package com.dave.filestorage.dto;

import java.util.List;

public class WebhookConfigDto {

    private String bucket;
    private String webhookUrl;
    private List<String> events;

    public WebhookConfigDto() {
    }

    public WebhookConfigDto(String bucket, String webhookUrl, List<String> events) {
        this.bucket = bucket;
        this.webhookUrl = webhookUrl;
        this.events = events;
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
