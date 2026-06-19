package com.dave.filestorage.db;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataMongoTest
@Testcontainers
class FileDocumentRepositoryTest {

    @Container
    static MongoDBContainer mongo = new MongoDBContainer("mongo:6");

    @DynamicPropertySource
    static void mongoProps(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongo::getReplicaSetUrl);
    }

    @Autowired
    FileDocumentRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void saveAndFindById() {
        FileDocument doc = buildDoc("etag-001", "test-bucket", "images/photo.jpg");
        FileDocument saved = repository.save(doc);

        assertNotNull(saved.getId());
        assertTrue(repository.findById(saved.getId()).isPresent());
    }

    @Test
    void findByEtag_returnsDocument() {
        repository.save(buildDoc("etag-abc", "test-bucket", "docs/file.pdf"));

        Optional<FileDocument> found = repository.findFileDocumentByEtag("etag-abc");
        assertTrue(found.isPresent());
        assertEquals("etag-abc", found.get().getEtag());
    }

    @Test
    void findByEtag_missingEtag_returnsEmpty() {
        Optional<FileDocument> found = repository.findFileDocumentByEtag("nonexistent");
        assertFalse(found.isPresent());
    }

    @Test
    void findFirstByBucketAndObjectName_returnsDocument() {
        repository.save(buildDoc("etag-xyz", "my-bucket", "videos/clip.mp4"));

        Optional<FileDocument> found = repository.findFirstByBucketAndObjectName("my-bucket", "videos/clip.mp4");
        assertTrue(found.isPresent());
        assertEquals("my-bucket", found.get().getBucket());
    }

    @Test
    void findFirstByBucketAndObjectName_wrongBucket_returnsEmpty() {
        repository.save(buildDoc("etag-xyz", "my-bucket", "videos/clip.mp4"));

        Optional<FileDocument> found = repository.findFirstByBucketAndObjectName("other-bucket", "videos/clip.mp4");
        assertFalse(found.isPresent());
    }

    @Test
    void softDelete_archivedDocumentStillExistsInDb() {
        FileDocument doc = buildDoc("etag-del", "test-bucket", "temp/file.txt");
        FileDocument saved = repository.save(doc);

        saved.setArchived(true);
        repository.save(saved);

        Optional<FileDocument> found = repository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertTrue(found.get().isArchived());
    }

    @Test
    void findByObjectName_returnsDocument() {
        repository.save(buildDoc("etag-obj", "test-bucket", "unique/path/file.txt"));

        Optional<FileDocument> found = repository.findByObjectName("unique/path/file.txt");
        assertTrue(found.isPresent());
    }

    private FileDocument buildDoc(String etag, String bucket, String objectName) {
        FileDocument doc = new FileDocument();
        doc.setEtag(etag);
        doc.setBucket(bucket);
        doc.setObjectName(objectName);
        doc.setOriginalFilename("file.txt");
        doc.setSize(1024L);
        doc.setContentType("application/octet-stream");
        doc.setUploadedAt(new Date());
        doc.setArchived(false);
        doc.setPublic(true);
        return doc;
    }
}
