package com.dave.filestorage.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class StorageMetrics {

    public final Counter uploadSuccess;
    public final Counter uploadFailure;
    public final Counter downloadSuccess;
    public final Counter downloadNotFound;

    public StorageMetrics(MeterRegistry registry) {
        this.uploadSuccess = Counter.builder("filestorage.uploads")
                .tag("status", "success")
                .description("Successful file uploads")
                .register(registry);
        this.uploadFailure = Counter.builder("filestorage.uploads")
                .tag("status", "failure")
                .description("Failed file uploads")
                .register(registry);
        this.downloadSuccess = Counter.builder("filestorage.downloads")
                .tag("status", "success")
                .description("Successful file downloads")
                .register(registry);
        this.downloadNotFound = Counter.builder("filestorage.downloads")
                .tag("status", "not_found")
                .description("Download requests for missing files")
                .register(registry);
    }
}
