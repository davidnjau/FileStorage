package com.dave.filestorage.storage.minio;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.SetBucketLifecycleArgs;
import io.minio.SetBucketPolicyArgs;
import io.minio.SetBucketVersioningArgs;
import io.minio.messages.AbortIncompleteMultipartUpload;
import io.minio.messages.Expiration;
import io.minio.messages.LifecycleConfiguration;
import io.minio.messages.LifecycleRule;
import io.minio.messages.RuleFilter;
import io.minio.messages.Status;
import io.minio.messages.VersioningConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.ZonedDateTime;
import java.util.LinkedList;
import java.util.List;

@Service
@ConditionalOnProperty(name = "storage.provider", havingValue = "minio", matchIfMissing = true)
public class MinioBucketService {

    @Resource
    private MinioClient minioClient;

    @Value("${minio.publicBucketName:public-bucket}")
    private String publicBucketName;

    @Value("${minio.privateBucketName:private-bucket}")
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
            // Public bucket
            if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(publicBucketName).build())) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(publicBucketName).build());
                setPublicBucketPolicy(publicBucketName);
            }
            if (publicExpiryDays > 0) {
                applyLifecycleRules(publicBucketName, publicExpiryDays);
            }
            if (versioningEnabled) {
                enableVersioning(publicBucketName);
            }

            // Private bucket
            if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(privateBucketName).build())) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(privateBucketName).build());
                setPrivateBucketPolicy(privateBucketName);
            }
            if (privateExpiryDays > 0) {
                applyLifecycleRules(privateBucketName, privateExpiryDays);
            }
            if (versioningEnabled) {
                enableVersioning(privateBucketName);
            }

        } catch (Exception e) {
            throw new RuntimeException("Error creating MinIO buckets", e);
        }
    }

    private void setPublicBucketPolicy(String bucketName) throws Exception {
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

        minioClient.setBucketPolicy(
                SetBucketPolicyArgs.builder()
                        .bucket(bucketName)
                        .config(policy)
                        .build()
        );
    }

    private void setPrivateBucketPolicy(String bucketName) throws Exception {
        String policy = "{\n" +
                "  \"Version\": \"2012-10-17\",\n" +
                "  \"Statement\": []\n" +
                "}";

        minioClient.setBucketPolicy(
                SetBucketPolicyArgs.builder()
                        .bucket(bucketName)
                        .config(policy)
                        .build()
        );
    }

    private void applyLifecycleRules(String bucketName, int expiryDays) throws Exception {
        List<LifecycleRule> rules = new LinkedList<>();
        rules.add(new LifecycleRule(
            Status.ENABLED,
            new AbortIncompleteMultipartUpload(multipartExpiryDays),
            new Expiration((ZonedDateTime) null, expiryDays, null),
            new RuleFilter(""),
            "auto-expire-" + bucketName,
            null, null, null));
        minioClient.setBucketLifecycle(
            SetBucketLifecycleArgs.builder()
                .bucket(bucketName)
                .config(new LifecycleConfiguration(rules))
                .build());
    }

    private void enableVersioning(String bucketName) throws Exception {
        minioClient.setBucketVersioning(
            SetBucketVersioningArgs.builder()
                .bucket(bucketName)
                .config(new VersioningConfiguration(VersioningConfiguration.Status.ENABLED, false))
                .build());
    }

    public String getPublicBucketName() {
        return publicBucketName;
    }

    public String getPrivateBucketName() {
        return privateBucketName;
    }
}
