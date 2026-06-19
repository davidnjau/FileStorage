package com.dave.filestorage.storage;

import com.dave.filestorage.dto.WebhookConfigDto;
import java.util.List;

public interface NotificationService {
    WebhookConfigDto registerWebhook(WebhookConfigDto config);
    void deregisterWebhook(String id);
    List<WebhookConfigDto> listWebhooks(String bucket);
}
