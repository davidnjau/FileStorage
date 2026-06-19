package com.dave.filestorage.garage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import javax.annotation.Resource;

@Service
public class GarageBucketService {

    @Resource
    private S3Client garageS3Client;

    @Value("${garage.publicBucketName:public-bucket}")
    private String publicBucketName;

    @Value("${garage.privateBucketName:private-bucket}")
    private String privateBucketName;

    public void createBuckets() {
        try {
            if (!bucketExists(publicBucketName)) {
                garageS3Client.createBucket(CreateBucketRequest.builder().bucket(publicBucketName).build());
                setPublicBucketPolicy(publicBucketName);
            }

            if (!bucketExists(privateBucketName)) {
                garageS3Client.createBucket(CreateBucketRequest.builder().bucket(privateBucketName).build());
                setPrivateBucketPolicy(privateBucketName);
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

    public String getPublicBucketName() {
        return publicBucketName;
    }

    public String getPrivateBucketName() {
        return privateBucketName;
    }
}
