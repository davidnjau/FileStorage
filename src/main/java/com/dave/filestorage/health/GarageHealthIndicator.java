package com.dave.filestorage.health;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListBucketsResponse;

@Component("garageStorage")
@ConditionalOnProperty(name = "storage.provider", havingValue = "garage")
public class GarageHealthIndicator implements HealthIndicator {

    @Autowired
    private S3Client garageS3Client;

    @Override
    public Health health() {
        try {
            ListBucketsResponse resp = garageS3Client.listBuckets();
            return Health.up()
                    .withDetail("provider", "garage")
                    .withDetail("buckets", resp.buckets().size())
                    .build();
        } catch (Exception e) {
            return Health.down()
                    .withDetail("provider", "garage")
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
