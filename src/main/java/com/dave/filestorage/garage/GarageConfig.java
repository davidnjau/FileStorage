package com.dave.filestorage.garage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

@Configuration
@ConditionalOnProperty(name = "storage.provider", havingValue = "garage")
public class GarageConfig {

    @Value("${garage.url}")
    private String garageUrl;

    @Value("${garage.accessKey}")
    private String garageAccessKey;

    @Value("${garage.secretKey}")
    private String garageSecretKey;

    @Value("${garage.region:garage}")
    private String garageRegion;

    @Bean
    public S3Client garageS3Client() {
        return S3Client.builder()
                .endpointOverride(URI.create(garageUrl))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(garageAccessKey, garageSecretKey)))
                .region(Region.of(garageRegion))
                .forcePathStyle(true)
                .build();
    }

    @Bean
    public S3Presigner garageS3Presigner() {
        return S3Presigner.builder()
                .endpointOverride(URI.create(garageUrl))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(garageAccessKey, garageSecretKey)))
                .region(Region.of(garageRegion))
                .build();
    }
}
