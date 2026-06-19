package com.dave.filestorage.exception;

public class WebhookValidationException extends FileStorageException {
    public WebhookValidationException(String message) {
        super("INVALID_WEBHOOK_URL", message);
    }
}
