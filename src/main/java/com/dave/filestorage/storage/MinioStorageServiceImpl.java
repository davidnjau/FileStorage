package com.dave.filestorage.storage;

import com.dave.filestorage.db.FileDocument;
import com.dave.filestorage.db.FileDocumentRepository;
import com.dave.filestorage.db.FileDocumentService;
import com.dave.filestorage.dto.FileDocumentDto;
import com.dave.filestorage.minio.MinioBucketService;
import io.minio.*;
import io.minio.errors.*;
import io.minio.http.Method;
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
public class MinioStorageServiceImpl implements MinioStorageService{

    @Autowired
    private MinioClient minioClient;

    @Autowired
    private FileDocumentService fileDocumentService;

    @Autowired
    private FileMetadataWorker fileMetadataWorker;

    @Autowired
    private MinioBucketService minioBucketService;

    @Value("${minio.url}")
    private String minioUrl;

    @Value("${minio.expiry.hours}")
    private String expiryHours;

    @Autowired
    private FileDocumentRepository fileDocumentRepository;


    /**
     * Initializes the MinIO bucket if it does not already exist.
     * This method is called after the bean's properties have been set.
     * It checks for the existence of the specified bucket and creates it if not found.
     */
    @PostConstruct
    public void initBuckets() {
        minioBucketService.createBuckets();
    }

    /**
     * Uploads a file to MinIO and persists metadata.
     *
     * @param file     the file to be uploaded
     * @param fileType the logical folder/category (e.g. "products", "invoices")
     * @param isPublic whether the file should be accessible publicly
     * @return a FileDocumentDto containing metadata and either a presigned URL (private) or direct URL (public)
     */
    public FileDocumentDto uploadFile(MultipartFile file, String fileType, boolean isPublic) {
        Objects.requireNonNull(file, "File cannot be null");
        Objects.requireNonNull(fileType, "File type cannot be null");

        try (InputStream inputStream = file.getInputStream()) {

            // Choose bucket based on visibility
            String bucketName = isPublic ?
                    minioBucketService.getPublicBucketName() :
                    minioBucketService.getPrivateBucketName();

            // Extract extension
            String fileExtension = Optional.ofNullable(file.getOriginalFilename())
                    .filter(name -> name.contains("."))
                    .map(name -> name.substring(name.lastIndexOf('.') + 1).toLowerCase())
                    .orElse("unknown");

            // Build structured object key: year/type/ext/month/day/originalName
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

            // Upload to MinIO
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(inputStream, file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );

            // Fetch stored metadata
            StatObjectResponse stat = minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );

            // Persist metadata asynchronously
            FileDocument doc = new FileDocument();

            String url = getMinioFileUrl(isPublic, bucketName, objectName, now, doc);

            doc.setUploadedAt(Date.from(now.toInstant()));
            doc.setOriginalFilename(file.getOriginalFilename());
            doc.setObjectName(objectName);
            doc.setBucket(bucketName);
            doc.setSize(stat.size());
            doc.setContentType(stat.contentType());
            doc.setEtag(stat.etag());
            doc.setLastModified(Date.from(stat.lastModified().toInstant()));
            doc.setUploadedBy(null); // TODO: wire user context
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
        // Return DTO with correct URL depending on visibility
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
                    .from(now.plusHours(Long
                                    .parseLong(expiryHours))
                            .toInstant());
            doc.setExpiryDateTime(oneHourLaterDate);
            doc.setExpiryHourTime(Integer.parseInt(expiryHours));

        }
        doc.setFileUrl(url);

        return url;
    }

    private String buildPublicUrl(String endpoint, String bucketName, String objectName) {
        // Ensure endpoint doesn’t end with slash
        String baseUrl = endpoint.endsWith("/") ? endpoint.substring(0, endpoint.length() - 1) : endpoint;
        return String.format("%s/%s/%s", baseUrl, bucketName, objectName);
    }


    /**
     * Downloads a file from MinIO storage using its unique ETag.
     *
     * @param etag the unique identifier of the file to be downloaded, used to locate the file in the database and storage.
     * @return an InputStream of the file if found, or null if the file does not exist in the database.
     * @throws Exception if an error occurs during the file retrieval from storage.
     */
    @Override
    public InputStream downloadFileEtag(String etag) throws Exception {

        FileDocument fileDocument = fileDocumentService.findByEtag(etag); // Check if file exists in the database
        if (fileDocument != null){

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
                // Generate new signed URL if file is private
                String url = getMinioFileUrl(fileDocument.isPublic(), fileDocument.getBucket(), fileDocument.getObjectName(), ZonedDateTime.now(), fileDocument);
                fileDocumentRepository.save(fileDocument); // Update the document with the new URL


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
}