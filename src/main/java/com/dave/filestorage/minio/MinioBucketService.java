package com.dave.filestorage.minio;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.SetBucketPolicyArgs;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@ConditionalOnProperty(name = "storage.provider", havingValue = "minio", matchIfMissing = true)
public class MinioBucketService {

    @Resource
    private MinioClient minioClient;

    @Value("${minio.publicBucketName:public-bucket}")
    private String publicBucketName;

    @Value("${minio.privateBucketName:private-bucket}")
    private String privateBucketName;

    /**
     * Creates both public and private buckets at startup if they do not exist.
     */
    public void createBuckets() {
        try {
            // Create Public Bucket
            if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(publicBucketName).build())) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(publicBucketName).build());
                setPublicBucketPolicy(publicBucketName);
            }

            // Create Private Bucket
            if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(privateBucketName).build())) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(privateBucketName).build());
                setPrivateBucketPolicy(privateBucketName);
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
        // Default MinIO bucket policy is private, but we can enforce explicitly
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

    public String getPublicBucketName() {
        return publicBucketName;
    }

    public String getPrivateBucketName() {
        return privateBucketName;
    }
}
