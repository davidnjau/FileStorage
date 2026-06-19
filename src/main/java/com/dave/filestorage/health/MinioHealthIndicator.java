package com.dave.filestorage.health;

import io.minio.ListBucketsArgs;
import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component("minioStorage")
@ConditionalOnProperty(name = "storage.provider", havingValue = "minio", matchIfMissing = true)
public class MinioHealthIndicator implements HealthIndicator {

    @Autowired
    private MinioClient minioClient;

    @Override
    public Health health() {
        try {
            minioClient.listBuckets(ListBucketsArgs.builder().build());
            return Health.up().withDetail("provider", "minio").build();
        } catch (Exception e) {
            return Health.down()
                    .withDetail("provider", "minio")
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
