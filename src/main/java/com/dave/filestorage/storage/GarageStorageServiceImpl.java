package com.dave.filestorage.storage;

import com.dave.filestorage.db.FileDocument;
import com.dave.filestorage.db.FileDocumentRepository;
import com.dave.filestorage.db.FileDocumentService;
import com.dave.filestorage.dto.FileDocumentDto;
import com.dave.filestorage.garage.GarageBucketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import javax.annotation.PostConstruct;
import java.io.InputStream;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.TextStyle;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

@Service
public class GarageStorageServiceImpl implements GarageStorageService {

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

            garageS3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(objectName)
                            .contentType(file.getContentType())
                            .build(),
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
