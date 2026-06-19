package com.dave.filestorage.minio;

import io.minio.CreateMultipartUploadResponse;
import io.minio.MinioAsyncClient;
import io.minio.UploadPartResponse;
import io.minio.messages.Part;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.InputStream;

/**
 * Exposes MinIO's protected multipart-upload methods from S3Base.
 */
@Component
public class MinioMultipartHelper extends MinioAsyncClient {

    @Autowired
    public MinioMultipartHelper(MinioAsyncClient asyncClient) {
        super(asyncClient);
    }

    public String initiateMultipart(String bucket, String objectName) throws Exception {
        CreateMultipartUploadResponse resp =
            createMultipartUpload(bucket, null, objectName, null, null);
        return resp.result().uploadId();
    }

    public String uploadMultipartPart(String bucket, String objectName, InputStream data,
                                       long partSize, String uploadId, int partNumber) throws Exception {
        UploadPartResponse resp =
            uploadPart(bucket, null, objectName, data, partSize, uploadId, partNumber, null, null);
        return resp.etag();
    }

    public void abortMultipart(String bucket, String objectName, String uploadId) throws Exception {
        abortMultipartUpload(bucket, null, objectName, uploadId, null, null);
    }

    public void completeMultipart(String bucket, String objectName, String uploadId, Part[] parts) throws Exception {
        completeMultipartUpload(bucket, null, objectName, uploadId, parts, null, null);
    }
}
