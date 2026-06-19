package com.dave.filestorage.storage.minio;

import com.dave.filestorage.db.FileDocument;
import com.dave.filestorage.db.FileDocumentRepository;
import com.dave.filestorage.db.FileDocumentService;
import com.dave.filestorage.dto.FileDocumentDto;
import com.dave.filestorage.dto.FileVersionDto;
import com.dave.filestorage.dto.MultipartCompleteRequestDto;
import com.dave.filestorage.dto.MultipartInitiateResponseDto;
import com.dave.filestorage.dto.ObjectCopyRequestDto;
import com.dave.filestorage.dto.ObjectCopyResponseDto;
import com.dave.filestorage.dto.PresignedUploadConfirmDto;
import com.dave.filestorage.dto.PresignedUploadResponseDto;
import com.dave.filestorage.dto.RangeDownloadResult;
import com.dave.filestorage.storage.ObjectStorageService;
import com.dave.filestorage.config.StorageProperties;
import com.dave.filestorage.storage.S3NamingSanitizer;
import io.minio.*;
import io.minio.errors.*;
import io.minio.http.Method;
import io.minio.messages.Item;
import io.minio.messages.Part;
import io.minio.ServerSideEncryptionS3;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.ZonedDateTime;
import java.time.format.TextStyle;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@ConditionalOnProperty(name = "storage.provider", havingValue = "minio", matchIfMissing = true)
public class MinioStorageServiceImpl implements ObjectStorageService {

    @Autowired
    private MinioClient minioClient;

    @Autowired
    private MinioMultipartHelper multipartHelper;

    @Autowired
    private FileDocumentService fileDocumentService;

    @Autowired
    private MinioFileMetadataWorker fileMetadataWorker;

    @Autowired
    private MinioBucketService minioBucketService;

    @Value("${minio.url}")
    private String minioUrl;

    @Value("${minio.expiry.hours}")
    private String expiryHours;

    @Autowired
    private FileDocumentRepository fileDocumentRepository;

    @Autowired
    private StorageProperties storageProperties;

    @PostConstruct
    public void initBuckets() {
        minioBucketService.createBuckets();
    }

    @Override
    public FileDocumentDto uploadFile(MultipartFile file, String fileType, boolean isPublic) {
        Objects.requireNonNull(file, "File cannot be null");
        Objects.requireNonNull(fileType, "File type cannot be null");

        try (InputStream inputStream = file.getInputStream()) {

            String bucketName = isPublic ?
                    minioBucketService.getPublicBucketName() :
                    minioBucketService.getPrivateBucketName();

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

            FileDocument doc = new FileDocument();

            PutObjectArgs.Builder putArgs = PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .stream(inputStream, file.getSize(), -1)
                    .contentType(file.getContentType());

            if (storageProperties.getEncryption().getSseS3().isEnabled()) {
                putArgs.sse(new ServerSideEncryptionS3());
                doc.setSseAlgorithm("AES256");
            }

            minioClient.putObject(putArgs.build());

            StatObjectResponse stat = minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );

            try {
                Iterable<Result<Item>> versions = minioClient.listObjects(
                    ListObjectsArgs.builder()
                        .bucket(bucketName).prefix(objectName)
                        .includeVersions(true).maxKeys(1).build());
                for (Result<Item> r : versions) {
                    doc.setVersionId(r.get().versionId());
                    break;
                }
            } catch (Exception ignored) {}

            String url = getMinioFileUrl(isPublic, bucketName, objectName, now, doc);

            doc.setUploadedAt(Date.from(now.toInstant()));
            doc.setOriginalFilename(file.getOriginalFilename());
            doc.setObjectName(objectName);
            doc.setBucket(bucketName);
            doc.setSize(stat.size());
            doc.setContentType(stat.contentType());
            doc.setEtag(stat.etag());
            doc.setLastModified(Date.from(stat.lastModified().toInstant()));
            doc.setUploadedBy(null);
            doc.setFileUrl(url);
            doc.setCustomMetadata(stat.userMetadata());
            doc.setArchived(false);
            doc.setPublic(isPublic);
            fileMetadataWorker.persistMetadataAsync(doc);

            return new FileDocumentDto(
                    doc.getEtag(),
                    doc.getEtag(),
                    doc.getOriginalFilename(),
                    doc.getLastModified(),
                    doc.getSize(),
                    url
            );

        } catch (Exception e) {
            throw new RuntimeException("Failed to upload file to MinIO", e);
        }
    }

    private String getMinioFileUrl(
            boolean isPublic,
            String bucketName,
            String objectName,
            ZonedDateTime now,
            FileDocument doc) throws ServerException, InsufficientDataException, ErrorResponseException,
            IOException, NoSuchAlgorithmException, InvalidKeyException,
            InvalidResponseException, XmlParserException, InternalException {
        String url;
        if (isPublic) {
            url = buildPublicUrl(minioUrl, bucketName, objectName);
        } else {
            url = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .method(Method.GET)
                            .expiry(Integer.parseInt(expiryHours), TimeUnit.HOURS)
                            .build()
            );
            Date oneHourLaterDate = Date
                    .from(now.plusHours(Long.parseLong(expiryHours)).toInstant());
            doc.setExpiryDateTime(oneHourLaterDate);
            doc.setExpiryHourTime(Integer.parseInt(expiryHours));
        }
        doc.setFileUrl(url);
        return url;
    }

    private String buildPublicUrl(String endpoint, String bucketName, String objectName) {
        String baseUrl = endpoint.endsWith("/") ? endpoint.substring(0, endpoint.length() - 1) : endpoint;
        return String.format("%s/%s/%s", baseUrl, bucketName, objectName);
    }

    @Override
    public InputStream downloadFileEtag(String etag) throws Exception {
        FileDocument fileDocument = fileDocumentService.findByEtag(etag);
        if (fileDocument != null) {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                           .bucket(fileDocument.getBucket())
                           .object(fileDocument.getObjectName())
                           .build());
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
                String url = getMinioFileUrl(fileDocument.isPublic(), fileDocument.getBucket(),
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
            } else {
                return null;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public MultipartInitiateResponseDto initiateMultipartUpload(String filename, String fileType,
            String contentType, boolean isPublic) throws Exception {
        String bucketName = isPublic ? minioBucketService.getPublicBucketName()
                                     : minioBucketService.getPrivateBucketName();
        ZonedDateTime now = ZonedDateTime.now();
        String ext = filename.contains(".") ? filename.substring(filename.lastIndexOf('.') + 1).toLowerCase() : "unknown";
        String monthName = now.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
        String objectName = String.format("%s/%s/%s/%s/%02d/%s",
            now.getYear(), S3NamingSanitizer.sanitizeOrDefault(fileType), ext,
            S3NamingSanitizer.sanitizeOrDefault(monthName), now.getDayOfMonth(), filename);

        String uploadId = multipartHelper.initiateMultipart(bucketName, objectName);

        FileDocument doc = new FileDocument();
        doc.setOriginalFilename(filename);
        doc.setObjectName(objectName);
        doc.setBucket(bucketName);
        doc.setUploadId(uploadId);
        doc.setUploadStatus("in_progress");
        doc.setPublic(isPublic);
        doc.setUploadedAt(Date.from(now.toInstant()));
        fileDocumentRepository.save(doc);

        return new MultipartInitiateResponseDto(uploadId, objectName, bucketName);
    }

    @Override
    public String uploadPart(String bucket, String objectName, String uploadId,
            int partNumber, InputStream data, long partSize) throws Exception {
        return multipartHelper.uploadMultipartPart(bucket, objectName, data, partSize, uploadId, partNumber);
    }

    @Override
    public FileDocumentDto completeMultipartUpload(MultipartCompleteRequestDto request) throws Exception {
        Part[] parts = request.getParts().stream()
            .map(p -> new Part(p.getPartNumber(), p.getETag()))
            .toArray(Part[]::new);

        multipartHelper.completeMultipart(request.getBucket(), request.getObjectName(),
            request.getUploadId(), parts);

        StatObjectResponse stat = minioClient.statObject(
            StatObjectArgs.builder()
                .bucket(request.getBucket()).object(request.getObjectName()).build());

        ZonedDateTime now = ZonedDateTime.now();
        Optional<FileDocument> existing = fileDocumentRepository
            .findFirstByBucketAndObjectName(request.getBucket(), request.getObjectName());
        FileDocument doc = existing.orElse(new FileDocument());
        boolean isPublic = doc.isPublic();
        String url = getMinioFileUrl(isPublic, request.getBucket(), request.getObjectName(), now, doc);

        doc.setEtag(stat.etag());
        doc.setSize(stat.size());
        doc.setContentType(stat.contentType());
        doc.setLastModified(Date.from(stat.lastModified().toInstant()));
        doc.setUploadStatus("completed");
        doc.setArchived(false);
        fileDocumentRepository.save(doc);

        return new FileDocumentDto(doc.getId(), doc.getEtag(), doc.getOriginalFilename(),
            doc.getLastModified(), doc.getSize(), url);
    }

    @Override
    public void abortMultipartUpload(String bucket, String objectName, String uploadId) throws Exception {
        multipartHelper.abortMultipart(bucket, objectName, uploadId);
        fileDocumentRepository.findFirstByBucketAndObjectName(bucket, objectName)
            .ifPresent(doc -> {
                doc.setUploadStatus("aborted");
                fileDocumentRepository.save(doc);
            });
    }

    @Override
    public PresignedUploadResponseDto generatePresignedUploadUrl(String filename, String fileType,
            String contentType, boolean isPublic) throws Exception {
        String bucketName = isPublic ? minioBucketService.getPublicBucketName()
                                     : minioBucketService.getPrivateBucketName();
        ZonedDateTime now = ZonedDateTime.now();
        String ext = filename.contains(".") ? filename.substring(filename.lastIndexOf('.') + 1).toLowerCase() : "unknown";
        String monthName = now.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
        String objectName = String.format("%s/%s/%s/%s/%02d/%s",
            now.getYear(), S3NamingSanitizer.sanitizeOrDefault(fileType), ext,
            S3NamingSanitizer.sanitizeOrDefault(monthName), now.getDayOfMonth(), filename);

        int presignedPutExpiryMinutes = storageProperties.getPresigned().getPut().getExpiryMinutes();
        String uploadUrl = minioClient.getPresignedObjectUrl(
            GetPresignedObjectUrlArgs.builder()
                .bucket(bucketName).object(objectName)
                .method(Method.PUT)
                .expiry(presignedPutExpiryMinutes, TimeUnit.MINUTES)
                .build());

        Date expiresAt = Date.from(now.plusMinutes(presignedPutExpiryMinutes).toInstant());
        return new PresignedUploadResponseDto(uploadUrl, objectName, bucketName, expiresAt);
    }

    @Override
    public FileDocumentDto confirmPresignedUpload(PresignedUploadConfirmDto confirm) throws Exception {
        StatObjectResponse stat = minioClient.statObject(
            StatObjectArgs.builder()
                .bucket(confirm.getBucket()).object(confirm.getObjectName()).build());

        ZonedDateTime now = ZonedDateTime.now();
        boolean isPublic = Boolean.TRUE.equals(confirm.getIsPublic());
        FileDocument doc = new FileDocument();
        String url = getMinioFileUrl(isPublic, confirm.getBucket(), confirm.getObjectName(), now, doc);

        doc.setOriginalFilename(confirm.getOriginalFilename());
        doc.setObjectName(confirm.getObjectName());
        doc.setBucket(confirm.getBucket());
        doc.setSize(stat.size());
        doc.setContentType(stat.contentType());
        doc.setEtag(stat.etag());
        doc.setLastModified(Date.from(stat.lastModified().toInstant()));
        doc.setUploadedAt(Date.from(now.toInstant()));
        doc.setPublic(isPublic);
        doc.setArchived(false);
        if (storageProperties.getEncryption().getSseS3().isEnabled()) doc.setSseAlgorithm("AES256");
        fileMetadataWorker.persistMetadataAsync(doc);

        return new FileDocumentDto(doc.getEtag(), doc.getEtag(), doc.getOriginalFilename(),
            doc.getLastModified(), doc.getSize(), url);
    }

    @Override
    public RangeDownloadResult downloadFileRange(String etag, long rangeStart, long rangeEnd) throws Exception {
        FileDocument doc = fileDocumentService.findByEtag(etag);
        if (doc == null) return null;

        StatObjectResponse stat = minioClient.statObject(
            StatObjectArgs.builder().bucket(doc.getBucket()).object(doc.getObjectName()).build());
        long totalSize = stat.size();
        long end = rangeEnd < 0 ? totalSize - 1 : Math.min(rangeEnd, totalSize - 1);
        long length = end - rangeStart + 1;

        InputStream stream = minioClient.getObject(
            GetObjectArgs.builder()
                .bucket(doc.getBucket()).object(doc.getObjectName())
                .offset(rangeStart).length(length)
                .build());

        return new RangeDownloadResult(stream, length, rangeStart, end, totalSize);
    }

    @Override
    public InputStream downloadFileEtagWithVersion(String etag, String versionId) throws Exception {
        FileDocument doc = fileDocumentService.findByEtag(etag);
        if (doc == null) return null;
        GetObjectArgs.Builder req = GetObjectArgs.builder()
            .bucket(doc.getBucket()).object(doc.getObjectName());
        if (versionId != null && !versionId.isEmpty()) req.versionId(versionId);
        return minioClient.getObject(req.build());
    }

    @Override
    public List<FileVersionDto> listFileVersions(String etag) throws Exception {
        FileDocument doc = fileDocumentService.findByEtag(etag);
        if (doc == null) return Collections.emptyList();

        List<FileVersionDto> result = new ArrayList<>();
        Iterable<Result<Item>> versions = minioClient.listObjects(
            ListObjectsArgs.builder()
                .bucket(doc.getBucket()).prefix(doc.getObjectName())
                .includeVersions(true).build());
        for (Result<Item> r : versions) {
            Item item = r.get();
            result.add(new FileVersionDto(
                item.versionId(),
                Date.from(item.lastModified().toInstant()),
                item.size(),
                item.etag(),
                item.isLatest()));
        }
        return result;
    }

    @Override
    public ObjectCopyResponseDto copyObject(ObjectCopyRequestDto request) throws Exception {
        FileDocument src = fileDocumentService.findByEtag(request.getSourceEtag());
        if (src == null) throw new RuntimeException("Source file not found");

        String destBucket = request.getDestinationBucket() != null
            ? request.getDestinationBucket() : src.getBucket();
        String destFilename = request.getDestinationFilename() != null
            ? request.getDestinationFilename() : src.getOriginalFilename();
        String destObjectName = src.getObjectName().substring(0, src.getObjectName().lastIndexOf('/') + 1) + destFilename;

        ObjectWriteResponse copyResp = minioClient.copyObject(
            CopyObjectArgs.builder()
                .bucket(destBucket).object(destObjectName)
                .source(CopySource.builder()
                    .bucket(src.getBucket()).object(src.getObjectName()).build())
                .build());

        ZonedDateTime now = ZonedDateTime.now();
        FileDocument newDoc = new FileDocument();
        boolean isPublic = src.isPublic();
        String url = getMinioFileUrl(isPublic, destBucket, destObjectName, now, newDoc);

        newDoc.setOriginalFilename(destFilename);
        newDoc.setObjectName(destObjectName);
        newDoc.setBucket(destBucket);
        newDoc.setSize(src.getSize());
        newDoc.setContentType(src.getContentType());
        newDoc.setEtag(copyResp.etag());
        newDoc.setLastModified(Date.from(now.toInstant()));
        newDoc.setUploadedAt(Date.from(now.toInstant()));
        newDoc.setPublic(isPublic);
        newDoc.setArchived(false);
        fileDocumentRepository.save(newDoc);

        return new ObjectCopyResponseDto(copyResp.etag(), destObjectName, destBucket, url);
    }

    @Override
    public ObjectCopyResponseDto moveObject(ObjectCopyRequestDto request) throws Exception {
        FileDocument src = fileDocumentService.findByEtag(request.getSourceEtag());
        ObjectCopyResponseDto result = copyObject(request);
        minioClient.removeObject(
            RemoveObjectArgs.builder()
                .bucket(src.getBucket()).object(src.getObjectName()).build());
        src.setArchived(true);
        fileDocumentRepository.save(src);
        return result;
    }
}
