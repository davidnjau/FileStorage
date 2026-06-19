package com.dave.filestorage.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Centralised storage configuration — all magic numbers live here.
 * Values are bound from application.properties under the {@code storage.*} prefix.
 */
@Component
@ConfigurationProperties(prefix = "storage")
public class StorageProperties {

    private final Multipart multipart = new Multipart();
    private final Lifecycle lifecycle = new Lifecycle();
    private final Presigned presigned = new Presigned();
    private final Encryption encryption = new Encryption();
    private final Notification notification = new Notification();
    private boolean versioningEnabled = true;

    public Multipart getMultipart() { return multipart; }
    public Lifecycle getLifecycle() { return lifecycle; }
    public Presigned getPresigned() { return presigned; }
    public Encryption getEncryption() { return encryption; }
    public Notification getNotification() { return notification; }
    public boolean isVersioningEnabled() { return versioningEnabled; }
    public void setVersioningEnabled(boolean versioningEnabled) { this.versioningEnabled = versioningEnabled; }

    public static class Multipart {
        private long thresholdBytes = 10_485_760;
        private long partSizeBytes = 5_242_880;

        public long getThresholdBytes() { return thresholdBytes; }
        public void setThresholdBytes(long thresholdBytes) { this.thresholdBytes = thresholdBytes; }
        public long getPartSizeBytes() { return partSizeBytes; }
        public void setPartSizeBytes(long partSizeBytes) { this.partSizeBytes = partSizeBytes; }
    }

    public static class Lifecycle {
        private int publicExpiryDays = 365;
        private int privateExpiryDays = 90;
        private int multipartExpiryDays = 7;

        public int getPublicExpiryDays() { return publicExpiryDays; }
        public void setPublicExpiryDays(int publicExpiryDays) { this.publicExpiryDays = publicExpiryDays; }
        public int getPrivateExpiryDays() { return privateExpiryDays; }
        public void setPrivateExpiryDays(int privateExpiryDays) { this.privateExpiryDays = privateExpiryDays; }
        public int getMultipartExpiryDays() { return multipartExpiryDays; }
        public void setMultipartExpiryDays(int multipartExpiryDays) { this.multipartExpiryDays = multipartExpiryDays; }
    }

    public static class Presigned {
        private Put put = new Put();
        public Put getPut() { return put; }

        public static class Put {
            private int expiryMinutes = 15;
            public int getExpiryMinutes() { return expiryMinutes; }
            public void setExpiryMinutes(int expiryMinutes) { this.expiryMinutes = expiryMinutes; }
        }
    }

    public static class Encryption {
        private SseS3 sseS3 = new SseS3();
        public SseS3 getSseS3() { return sseS3; }

        public static class SseS3 {
            private boolean enabled = false;
            public boolean isEnabled() { return enabled; }
            public void setEnabled(boolean enabled) { this.enabled = enabled; }
        }
    }

    public static class Notification {
        private boolean listenerEnabled = false;
        private boolean pollingEnabled = false;
        private long pollingIntervalMs = 30_000;

        public boolean isListenerEnabled() { return listenerEnabled; }
        public void setListenerEnabled(boolean listenerEnabled) { this.listenerEnabled = listenerEnabled; }
        public boolean isPollingEnabled() { return pollingEnabled; }
        public void setPollingEnabled(boolean pollingEnabled) { this.pollingEnabled = pollingEnabled; }
        public long getPollingIntervalMs() { return pollingIntervalMs; }
        public void setPollingIntervalMs(long pollingIntervalMs) { this.pollingIntervalMs = pollingIntervalMs; }
    }
}
