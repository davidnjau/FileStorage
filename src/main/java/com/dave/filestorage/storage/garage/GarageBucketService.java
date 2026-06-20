package com.dave.filestorage.storage.garage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import jakarta.annotation.Resource;
import java.util.Arrays;
import java.util.List;

@Service
@ConditionalOnProperty(name = "storage.provider", havingValue = "garage")
public class GarageBucketService {

    @Resource
    private S3Client garageS3Client;

    @Value("${garage.publicBucketName:public-bucket}")
    private String publicBucketName;

    @Value("${garage.privateBucketName:private-bucket}")
    private String privateBucketName;

    @Value("${storage.lifecycle.public.expiry-days:0}")
    private int publicExpiryDays;

    @Value("${storage.lifecycle.private.expiry-days:0}")
    private int privateExpiryDays;

    @Value("${storage.lifecycle.multipart.expiry-days:7}")
    private int multipartExpiryDays;

    @Value("${storage.versioning.enabled:false}")
    private boolean versioningEnabled;

    @Value("${storage.cors.allowed-origins:*}")
    private String allowedOrigins;

    @Value("${storage.cors.max-age-seconds:3600}")
    private int corsMaxAge;

    public void createBuckets() {
        try {
            if (!bucketExists(publicBucketName)) {
                garageS3Client.createBucket(CreateBucketRequest.builder().bucket(publicBucketName).build());
                setPublicBucketPolicy(publicBucketName);
                applyLifecycleRules(publicBucketName, publicExpiryDays);
                if (versioningEnabled) enableVersioning(publicBucketName);
                applyCorsConfiguration(publicBucketName);
            }

            if (!bucketExists(privateBucketName)) {
                garageS3Client.createBucket(CreateBucketRequest.builder().bucket(privateBucketName).build());
                setPrivateBucketPolicy(privateBucketName);
                applyLifecycleRules(privateBucketName, privateExpiryDays);
                if (versioningEnabled) enableVersioning(privateBucketName);
                applyCorsConfiguration(privateBucketName);
            }

        } catch (Exception e) {
            throw new RuntimeException("Error creating Garage buckets", e);
        }
    }

    private boolean bucketExists(String bucketName) {
        try {
            garageS3Client.headBucket(HeadBucketRequest.builder().bucket(bucketName).build());
            return true;
        } catch (NoSuchBucketException e) {
            return false;
        }
    }

    private void setPublicBucketPolicy(String bucketName) {
        String policy = "{\n" +
                "  \"Version\": \"2012-10-17\",\n" +
                "  \"Statement\": [\n" +
                "    {\n" +
                "      \"Effect\": \"Allow\",\n" +
                "      \"Principal\": \"*\",\n" +
                "      \"Action\": [\"s3:GetObject\"],\n" +
                "      \"Resource\": [\"arn:aws:s3:::" + bucketName + "/*\"]\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        garageS3Client.putBucketPolicy(PutBucketPolicyRequest.builder()
                .bucket(bucketName)
                .policy(policy)
                .build());
    }

    private void setPrivateBucketPolicy(String bucketName) {
        String policy = "{\n" +
                "  \"Version\": \"2012-10-17\",\n" +
                "  \"Statement\": []\n" +
                "}";

        garageS3Client.putBucketPolicy(PutBucketPolicyRequest.builder()
                .bucket(bucketName)
                .policy(policy)
                .build());
    }

    private void applyLifecycleRules(String bucketName, int expiryDays) {
        LifecycleRule rule = LifecycleRule.builder()
                .id("auto-expire-" + bucketName)
                .status(ExpirationStatus.ENABLED)
                .filter(LifecycleRuleFilter.builder().prefix("").build())
                .expiration(LifecycleExpiration.builder().days(expiryDays).build())
                .abortIncompleteMultipartUpload(
                        AbortIncompleteMultipartUpload.builder()
                                .daysAfterInitiation(multipartExpiryDays).build())
                .build();
        garageS3Client.putBucketLifecycleConfiguration(
                PutBucketLifecycleConfigurationRequest.builder()
                        .bucket(bucketName)
                        .lifecycleConfiguration(BucketLifecycleConfiguration.builder()
                                .rules(rule).build())
                        .build());
    }

    private void enableVersioning(String bucketName) {
        garageS3Client.putBucketVersioning(
                PutBucketVersioningRequest.builder()
                        .bucket(bucketName)
                        .versioningConfiguration(VersioningConfiguration.builder()
                                .status(BucketVersioningStatus.ENABLED).build())
                        .build());
    }

    private void applyCorsConfiguration(String bucketName) {
        List<String> origins = Arrays.asList(allowedOrigins.split(","));
        CORSRule corsRule = CORSRule.builder()
                .allowedOrigins(origins)
                .allowedMethods("GET", "PUT", "POST", "DELETE", "HEAD")
                .allowedHeaders("*")
                .exposeHeaders("ETag", "x-amz-version-id")
                .maxAgeSeconds(corsMaxAge)
                .build();
        garageS3Client.putBucketCors(
                PutBucketCorsRequest.builder()
                        .bucket(bucketName)
                        .corsConfiguration(CORSConfiguration.builder()
                                .corsRules(corsRule).build())
                        .build());
    }

    public String getPublicBucketName() {
        return publicBucketName;
    }

    public String getPrivateBucketName() {
        return privateBucketName;
    }
}
