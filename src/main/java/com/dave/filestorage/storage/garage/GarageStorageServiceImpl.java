package com.dave.filestorage.storage.garage;

import com.dave.filestorage.db.FileDocument;
import com.dave.filestorage.db.FileDocumentRepository;
import com.dave.filestorage.db.FileDocumentService;
import com.dave.filestorage.config.StorageProperties;
import com.dave.filestorage.dto.*;
import com.dave.filestorage.storage.ObjectStorageService;
import com.dave.filestorage.storage.S3NamingSanitizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Service
@ConditionalOnProperty(name = "storage.provider", havingValue = "garage")
public class GarageStorageServiceImpl implements ObjectStorageService {

    @Autowired
    private S3Client garageS3Client;

    @Autowired
    private S3Presigner garageS3Presigner;

    @Autowired
    private GarageBucketService garageBucketService;

    @Autowired
    private FileDocumentService fileDocumentService;

    @Autowired
    private GarageFileMetadataWorker garageFileMetadataWorker;

    @Autowired
    private FileDocumentRepository fileDocumentRepository;

    @Value("${garage.url}")
    private String garageUrl;

    @Value("${garage.expiry.hours}")
    private String expiryHours;

    @Autowired
    private StorageProperties storageProperties;

    @PostConstruct
    public void initBuckets() {
        garageBucketService.createBuckets();
    }

    @Override
    public FileDocumentDto uploadFile(MultipartFile file, String fileType, boolean isPublic) {
        Objects.requireNonNull(file, "File cannot be null");
        Objects.requireNonNull(fileType, "File type cannot be null");

        try (InputStream inputStream = file.getInputStream()) {

            String bucketName = isPublic ?
                    garageBucketService.getPublicBucketName() :
                    garageBucketService.getPrivateBucketName();

            String fileExtension = Optional.ofNullable(file.getOriginalFilename())
                    .filter(name -> name.contains("."))
                    .map(name -> name.substring(name.lastIndexOf('.') + 1).toLowerCase())
                    .orElse("unknown");

            ZonedDateTime now = ZonedDateTime.now();
            String year = String.valueOf(now.getYear());
            String monthName = now.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
            String day = String.format("%02d", now.getDayOfMonth());

            String objectName = String.format(
                    "%s/%s/%s/%s/%s/%s",
                    year,
                    S3NamingSanitizer.sanitizeOrDefault(fileType),
                    fileExtension,
                    S3NamingSanitizer.sanitizeOrDefault(monthName),
                    day,
                    Objects.requireNonNull(file.getOriginalFilename())
            );

            PutObjectRequest.Builder putReqBuilder = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectName)
                    .contentType(file.getContentType() != null ? file.getContentType() : "application/octet-stream");
            if (storageProperties.getEncryption().getSseS3().isEnabled()) {
                putReqBuilder.serverSideEncryption(ServerSideEncryption.AES256);
            }
            PutObjectResponse putResp = garageS3Client.putObject(
                    putReqBuilder.build(),
                    RequestBody.fromInputStream(inputStream, file.getSize())
            );

            HeadObjectResponse head = garageS3Client.headObject(
                    HeadObjectRequest.builder()
                            .bucket(bucketName)
                            .key(objectName)
                            .build()
            );

            String rawEtag = head.eTag().replace("\"", "");

            FileDocument doc = new FileDocument();
            String url = buildFileUrl(isPublic, bucketName, objectName, now, doc);

            doc.setUploadedAt(Date.from(now.toInstant()));
            doc.setOriginalFilename(file.getOriginalFilename());
            doc.setObjectName(objectName);
            doc.setBucket(bucketName);
            doc.setSize(head.contentLength());
            doc.setContentType(head.contentType());
            doc.setEtag(rawEtag);
            doc.setLastModified(Date.from(head.lastModified()));
            doc.setUploadedBy(null);
            doc.setFileUrl(url);
            doc.setArchived(false);
            doc.setPublic(isPublic);
            doc.setVersionId(putResp.versionId());
            if (storageProperties.getEncryption().getSseS3().isEnabled()) doc.setSseAlgorithm("AES256");
            garageFileMetadataWorker.persistMetadataAsync(doc);

            return new FileDocumentDto(
                    doc.getEtag(),
                    doc.getEtag(),
                    doc.getOriginalFilename(),
                    doc.getLastModified(),
                    doc.getSize(),
                    url
            );

        } catch (Exception e) {
            throw new RuntimeException("Failed to upload file to Garage", e);
        }
    }

    @Override
    public InputStream downloadFileEtag(String etag) throws Exception {
        FileDocument fileDocument = fileDocumentService.findByEtag(etag);
        if (fileDocument != null) {
            return garageS3Client.getObject(
                    GetObjectRequest.builder()
                            .bucket(fileDocument.getBucket())
                            .key(fileDocument.getObjectName())
                            .build()
            );
        }
        return null;
    }

    @Override
    public FileDocumentDto getFileDocumentInformation(String id) {
        try {
            Optional<FileDocument> fileDocOpt = fileDocumentRepository
                    .findFirstByIdOrEtagAndIsPublicFalseAndArchivedFalse(id, id);

            if (fileDocOpt.isPresent()) {
                FileDocument fileDocument = fileDocOpt.get();
                String url = buildFileUrl(fileDocument.isPublic(), fileDocument.getBucket(),
                        fileDocument.getObjectName(), ZonedDateTime.now(), fileDocument);
                fileDocumentRepository.save(fileDocument);

                return new FileDocumentDto(
                        fileDocument.getId(),
                        fileDocument.getEtag(),
                        fileDocument.getOriginalFilename(),
                        fileDocument.getLastModified(),
                        fileDocument.getSize(),
                        url
                );
            }
            return null;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public MultipartInitiateResponseDto initiateMultipartUpload(String filename, String fileType,
            String contentType, boolean isPublic) throws Exception {
        String bucketName = isPublic ? garageBucketService.getPublicBucketName()
                                     : garageBucketService.getPrivateBucketName();
        ZonedDateTime now = ZonedDateTime.now();
        String ext = filename.contains(".")
                ? filename.substring(filename.lastIndexOf('.') + 1).toLowerCase() : "unknown";
        String monthName = now.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
        String objectName = String.format("%s/%s/%s/%s/%02d/%s",
                now.getYear(), S3NamingSanitizer.sanitizeOrDefault(fileType), ext,
                S3NamingSanitizer.sanitizeOrDefault(monthName), now.getDayOfMonth(), filename);

        CreateMultipartUploadRequest.Builder req = CreateMultipartUploadRequest.builder()
                .bucket(bucketName).key(objectName).contentType(contentType);
        if (storageProperties.getEncryption().getSseS3().isEnabled()) req.serverSideEncryption(ServerSideEncryption.AES256);
        CreateMultipartUploadResponse resp = garageS3Client.createMultipartUpload(req.build());

        FileDocument doc = new FileDocument();
        doc.setOriginalFilename(filename);
        doc.setObjectName(objectName);
        doc.setBucket(bucketName);
        doc.setUploadId(resp.uploadId());
        doc.setUploadStatus("in_progress");
        doc.setPublic(isPublic);
        doc.setUploadedAt(Date.from(now.toInstant()));
        fileDocumentRepository.save(doc);

        return new MultipartInitiateResponseDto(resp.uploadId(), objectName, bucketName);
    }

    @Override
    public String uploadPart(String bucket, String objectName, String uploadId,
            int partNumber, InputStream data, long partSize) throws Exception {
        UploadPartResponse resp = garageS3Client.uploadPart(
                UploadPartRequest.builder()
                        .bucket(bucket).key(objectName)
                        .uploadId(uploadId).partNumber(partNumber).build(),
                RequestBody.fromInputStream(data, partSize));
        return resp.eTag();
    }

    @Override
    public FileDocumentDto completeMultipartUpload(MultipartCompleteRequestDto request) throws Exception {
        List<CompletedPart> parts = request.getParts().stream()
                .map(p -> CompletedPart.builder()
                        .partNumber(p.getPartNumber()).eTag(p.getETag()).build())
                .collect(Collectors.toList());

        CompleteMultipartUploadResponse resp = garageS3Client.completeMultipartUpload(
                CompleteMultipartUploadRequest.builder()
                        .bucket(request.getBucket()).key(request.getObjectName())
                        .uploadId(request.getUploadId())
                        .multipartUpload(CompletedMultipartUpload.builder().parts(parts).build())
                        .build());

        HeadObjectResponse head = garageS3Client.headObject(
                HeadObjectRequest.builder()
                        .bucket(request.getBucket()).key(request.getObjectName()).build());

        ZonedDateTime now = ZonedDateTime.now();
        Optional<FileDocument> existing = fileDocumentRepository
                .findFirstByBucketAndObjectName(request.getBucket(), request.getObjectName());
        FileDocument doc = existing.orElse(new FileDocument());
        boolean isPublic = doc.isPublic();
        String url = buildFileUrl(isPublic, request.getBucket(), request.getObjectName(), now, doc);

        doc.setEtag(head.eTag().replace("\"", ""));
        doc.setSize(head.contentLength());
        doc.setContentType(head.contentType());
        doc.setLastModified(Date.from(head.lastModified()));
        doc.setUploadStatus("completed");
        doc.setVersionId(resp.versionId());
        doc.setArchived(false);
        fileDocumentRepository.save(doc);

        return new FileDocumentDto(doc.getId(), doc.getEtag(), doc.getOriginalFilename(),
                doc.getLastModified(), doc.getSize(), url);
    }

    @Override
    public void abortMultipartUpload(String bucket, String objectName, String uploadId) throws Exception {
        garageS3Client.abortMultipartUpload(
                AbortMultipartUploadRequest.builder()
                        .bucket(bucket).key(objectName).uploadId(uploadId).build());
        fileDocumentRepository.findFirstByBucketAndObjectName(bucket, objectName)
                .ifPresent(doc -> {
                    doc.setUploadStatus("aborted");
                    fileDocumentRepository.save(doc);
                });
    }

    @Override
    public PresignedUploadResponseDto generatePresignedUploadUrl(String filename, String fileType,
            String contentType, boolean isPublic) throws Exception {
        String bucketName = isPublic ? garageBucketService.getPublicBucketName()
                                     : garageBucketService.getPrivateBucketName();
        ZonedDateTime now = ZonedDateTime.now();
        String ext = filename.contains(".")
                ? filename.substring(filename.lastIndexOf('.') + 1).toLowerCase() : "unknown";
        String monthName = now.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
        String objectName = String.format("%s/%s/%s/%s/%02d/%s",
                now.getYear(), S3NamingSanitizer.sanitizeOrDefault(fileType), ext,
                S3NamingSanitizer.sanitizeOrDefault(monthName), now.getDayOfMonth(), filename);

        int presignedPutExpiryMinutes = storageProperties.getPresigned().getPut().getExpiryMinutes();
        PresignedPutObjectRequest presigned = garageS3Presigner.presignPutObject(
                PutObjectPresignRequest.builder()
                        .signatureDuration(Duration.ofMinutes(presignedPutExpiryMinutes))
                        .putObjectRequest(r -> r.bucket(bucketName).key(objectName).contentType(contentType))
                        .build());

        Date expiresAt = Date.from(now.plusMinutes(presignedPutExpiryMinutes).toInstant());
        return new PresignedUploadResponseDto(
                presigned.url().toString(), objectName, bucketName, expiresAt);
    }

    @Override
    public FileDocumentDto confirmPresignedUpload(PresignedUploadConfirmDto confirm) throws Exception {
        HeadObjectResponse head = garageS3Client.headObject(
                HeadObjectRequest.builder()
                        .bucket(confirm.getBucket()).key(confirm.getObjectName()).build());

        ZonedDateTime now = ZonedDateTime.now();
        boolean isPublic = Boolean.TRUE.equals(confirm.getIsPublic());
        FileDocument doc = new FileDocument();
        String url = buildFileUrl(isPublic, confirm.getBucket(), confirm.getObjectName(), now, doc);

        doc.setOriginalFilename(confirm.getOriginalFilename());
        doc.setObjectName(confirm.getObjectName());
        doc.setBucket(confirm.getBucket());
        doc.setSize(head.contentLength());
        doc.setContentType(head.contentType());
        doc.setEtag(head.eTag().replace("\"", ""));
        doc.setLastModified(Date.from(head.lastModified()));
        doc.setUploadedAt(Date.from(now.toInstant()));
        doc.setPublic(isPublic);
        doc.setArchived(false);
        if (storageProperties.getEncryption().getSseS3().isEnabled()) doc.setSseAlgorithm("AES256");
        garageFileMetadataWorker.persistMetadataAsync(doc);

        return new FileDocumentDto(doc.getEtag(), doc.getEtag(), doc.getOriginalFilename(),
                doc.getLastModified(), doc.getSize(), url);
    }

    @Override
    public RangeDownloadResult downloadFileRange(String etag, long rangeStart, long rangeEnd) throws Exception {
        FileDocument doc = fileDocumentService.findByEtag(etag);
        if (doc == null) return null;

        HeadObjectResponse head = garageS3Client.headObject(
                HeadObjectRequest.builder()
                        .bucket(doc.getBucket()).key(doc.getObjectName()).build());
        long totalSize = head.contentLength();
        long end = rangeEnd < 0 ? totalSize - 1 : Math.min(rangeEnd, totalSize - 1);

        ResponseInputStream<GetObjectResponse> stream = garageS3Client.getObject(
                GetObjectRequest.builder()
                        .bucket(doc.getBucket()).key(doc.getObjectName())
                        .range("bytes=" + rangeStart + "-" + end)
                        .build());

        return new RangeDownloadResult(stream, end - rangeStart + 1, rangeStart, end, totalSize);
    }

    @Override
    public InputStream downloadFileEtagWithVersion(String etag, String versionId) throws Exception {
        FileDocument doc = fileDocumentService.findByEtag(etag);
        if (doc == null) return null;
        GetObjectRequest.Builder req = GetObjectRequest.builder()
                .bucket(doc.getBucket()).key(doc.getObjectName());
        if (versionId != null && !versionId.isEmpty()) req.versionId(versionId);
        return garageS3Client.getObject(req.build());
    }

    @Override
    public List<FileVersionDto> listFileVersions(String etag, int page, int size) throws Exception {
        FileDocument doc = fileDocumentService.findByEtag(etag);
        if (doc == null) return Collections.emptyList();

        int fetchLimit = (page + 1) * size;
        ListObjectVersionsResponse resp = garageS3Client.listObjectVersions(
                ListObjectVersionsRequest.builder()
                        .bucket(doc.getBucket()).prefix(doc.getObjectName())
                        .maxKeys(fetchLimit)
                        .build());

        List<FileVersionDto> fetched = resp.versions().stream()
                .map(v -> new FileVersionDto(
                        v.versionId(),
                        Date.from(v.lastModified()),
                        v.size(),
                        v.eTag().replace("\"", ""),
                        v.isLatest()))
                .collect(Collectors.toList());

        int from = page * size;
        if (from >= fetched.size()) return Collections.emptyList();
        return fetched.subList(from, Math.min(from + size, fetched.size()));
    }

    @Override
    public ObjectCopyResponseDto copyObject(ObjectCopyRequestDto request) throws Exception {
        FileDocument src = fileDocumentService.findByEtag(request.getSourceEtag());
        if (src == null) throw new RuntimeException("Source file not found");

        String destBucket = request.getDestinationBucket() != null
                ? request.getDestinationBucket() : src.getBucket();
        String destFilename = request.getDestinationFilename() != null
                ? request.getDestinationFilename() : src.getOriginalFilename();
        String destObjectName = src.getObjectName()
                .substring(0, src.getObjectName().lastIndexOf('/') + 1) + destFilename;

        CopyObjectResponse copyResp = garageS3Client.copyObject(
                CopyObjectRequest.builder()
                        .sourceBucket(src.getBucket()).sourceKey(src.getObjectName())
                        .destinationBucket(destBucket).destinationKey(destObjectName)
                        .build());

        String newEtag = copyResp.copyObjectResult().eTag().replace("\"", "");
        ZonedDateTime now = ZonedDateTime.now();
        FileDocument newDoc = new FileDocument();
        boolean isPublic = src.isPublic();
        String url = buildFileUrl(isPublic, destBucket, destObjectName, now, newDoc);

        newDoc.setOriginalFilename(destFilename);
        newDoc.setObjectName(destObjectName);
        newDoc.setBucket(destBucket);
        newDoc.setSize(src.getSize());
        newDoc.setContentType(src.getContentType());
        newDoc.setEtag(newEtag);
        newDoc.setLastModified(Date.from(now.toInstant()));
        newDoc.setUploadedAt(Date.from(now.toInstant()));
        newDoc.setPublic(isPublic);
        newDoc.setArchived(false);
        fileDocumentRepository.save(newDoc);

        return new ObjectCopyResponseDto(newEtag, destObjectName, destBucket, url);
    }

    @Override
    public ObjectCopyResponseDto moveObject(ObjectCopyRequestDto request) throws Exception {
        FileDocument src = fileDocumentService.findByEtag(request.getSourceEtag());
        ObjectCopyResponseDto result = copyObject(request);
        garageS3Client.deleteObject(
                DeleteObjectRequest.builder()
                        .bucket(src.getBucket()).key(src.getObjectName()).build());
        src.setArchived(true);
        fileDocumentRepository.save(src);
        return result;
    }

    private String buildFileUrl(boolean isPublic, String bucketName, String objectName,
                                ZonedDateTime now, FileDocument doc) {
        if (isPublic) {
            String baseUrl = garageUrl.endsWith("/") ? garageUrl.substring(0, garageUrl.length() - 1) : garageUrl;
            String url = String.format("%s/%s/%s", baseUrl, bucketName, objectName);
            doc.setFileUrl(url);
            return url;
        }

        int hours = Integer.parseInt(expiryHours);
        PresignedGetObjectRequest presigned = garageS3Presigner.presignGetObject(
                GetObjectPresignRequest.builder()
                        .signatureDuration(Duration.ofHours(hours))
                        .getObjectRequest(r -> r.bucket(bucketName).key(objectName))
                        .build()
        );

        String url = presigned.url().toString();
        doc.setFileUrl(url);
        doc.setExpiryDateTime(Date.from(now.plusHours(hours).toInstant()));
        doc.setExpiryHourTime(hours);
        return url;
    }
}
