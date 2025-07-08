package com.dave.filestorage.storage;

import com.dave.filestorage.db.FileDocument;
import com.dave.filestorage.db.FileDocumentService;
import com.dave.filestorage.dto.FileDocumentDto;
import io.minio.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.InputStream;
import java.time.ZonedDateTime;
import java.time.format.TextStyle;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

// MinioStorageService.java
@Service
public class MinioStorageServiceImpl implements MinioStorageService{

    @Autowired
    private MinioClient minioClient;

    @Autowired
    private FileDocumentService fileDocumentService;

    @Autowired
    private FileMetadataWorker fileMetadataWorker;

    @Value("${minio.bucketName}")
    private String bucketName;

    /**
     * Initializes the MinIO bucket if it does not already exist.
     * This method is called after the bean's properties have been set.
     * It checks for the existence of the specified bucket and creates it if not found.
     */
    @PostConstruct
    public void createBucket() {
        try {
            boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!found) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            }
        } catch (Exception e) {
            // Handle exception
        }
    }

    /**
     * Uploads a file to the MinIO storage and creates a corresponding file document in the database.
     *
     * @param file the file to be uploaded, represented as a MultipartFile.
     * @param fileName the type of the file, used to categorize the file in a virtual folder.
     * @return a FileDocumentDto containing the file's etag, original name, last modified date, and size.
     * @throws Exception if an error occurs during the file upload or database operation.
     */
    public FileDocumentDto uploadFile(MultipartFile file, String fileName) throws Exception  {

        String fileExtension = Objects.requireNonNull(file.getOriginalFilename())
                .substring(file.getOriginalFilename()
                        .lastIndexOf('.')).toLowerCase().replace(".", "");

        String objectName = file.getOriginalFilename();

        //get the year, month, and date from the system date
        String year = String.valueOf(ZonedDateTime.now().getYear());
        String month = String.format("%02d", ZonedDateTime.now().getMonthValue());
        //Convert the month to a month name
        String monthName = ZonedDateTime.now().getMonth().getDisplayName(TextStyle.FULL, Locale.getDefault());
        String convertedMonthName = S3NamingSanitizer.sanitizeOrDefault(monthName);

        String date = String.format("%02d", ZonedDateTime.now().getDayOfMonth());

        // Upload the file to MinIO e.g. year/products/jpeg/month/date/image.jpg
        String newObjectName =
                year + "/" +
                fileName + "/" +
                fileExtension + "/" +
                convertedMonthName + "/" + date + "/" +
                file.getOriginalFilename(); // Virtual folder

        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(newObjectName)
                        .stream(file.getInputStream(), file.getSize(), -1)
                        .contentType(file.getContentType())
                        .build());

        StatObjectResponse stat = minioClient.statObject(
                StatObjectArgs.builder()
                        .bucket(bucketName)
                        .object(newObjectName)
                        .build()
        );

        //Create the file document in your database
        FileDocument doc = new FileDocument();
        doc.setOriginalFilename(objectName);
        doc.setObjectName(newObjectName);
        doc.setBucket(bucketName);
        doc.setSize(stat.size());
        doc.setContentType(stat.contentType());
        doc.setEtag(stat.etag());
        Date lastMoifiedDate = Date.from(stat.lastModified().toInstant());
        doc.setLastModified(lastMoifiedDate);
        doc.setPresignedUrl(null);
        doc.setUploadedAt(Date.from(ZonedDateTime.now().toInstant()));
        doc.setUploadedBy(null);
        fileMetadataWorker.persistMetadataAsync(doc);

        return new FileDocumentDto(
                doc.getEtag(),
                objectName,
                doc.getLastModified(),
                doc.getSize()
        );


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
}