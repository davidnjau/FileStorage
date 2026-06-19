package com.dave.filestorage.integration;

import com.dave.filestorage.dto.FileDocumentDto;
import com.dave.filestorage.storage.ObjectStorageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class MinioStorageIntegrationTest {

    @Container
    static MongoDBContainer mongo = new MongoDBContainer("mongo:6");

    @SuppressWarnings("resource")
    @Container
    static GenericContainer<?> minio = new GenericContainer<>(DockerImageName.parse("minio/minio:latest"))
            .withCommand("server", "/data", "--console-address", ":9001")
            .withEnv("MINIO_ROOT_USER", "minioadmin")
            .withEnv("MINIO_ROOT_PASSWORD", "minioadmin")
            .withExposedPorts(9000)
            .waitingFor(Wait.forHttp("/minio/health/live").forPort(9000).withStartupTimeout(
                    java.time.Duration.ofSeconds(60)));

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongo::getReplicaSetUrl);
        registry.add("minio.url", () -> "http://localhost:" + minio.getMappedPort(9000));
        registry.add("minio.accessKey", () -> "minioadmin");
        registry.add("minio.secretKey", () -> "minioadmin");
        registry.add("storage.provider", () -> "minio");
        registry.add("storage.versioning.enabled", () -> "false");
        registry.add("storage.notification.listener.enabled", () -> "false");
    }

    @Autowired
    ObjectStorageService objectStorageService;

    @Test
    void uploadAndDownload_endToEnd() throws Exception {
        byte[] content = "Hello, integration test!".getBytes();
        MockMultipartFile file = new MockMultipartFile(
                "file", "hello.txt", "text/plain", content);

        FileDocumentDto uploaded = objectStorageService.uploadFile(file, "test-docs", true);

        assertNotNull(uploaded);
        assertNotNull(uploaded.geteTagId());
        assertEquals("hello.txt", uploaded.getFileName());
        assertEquals(content.length, uploaded.getSize());
        assertNotNull(uploaded.getUrl());

        // Download and verify content
        InputStream downloaded = objectStorageService.downloadFileEtag(uploaded.geteTagId());
        assertNotNull(downloaded);
        byte[] downloadedBytes = downloaded.readAllBytes();
        assertArrayEquals(content, downloadedBytes);
    }

    @Test
    void uploadTwice_differentEtags() throws Exception {
        MockMultipartFile file1 = new MockMultipartFile(
                "file", "a.txt", "text/plain", "content-a".getBytes());
        MockMultipartFile file2 = new MockMultipartFile(
                "file", "b.txt", "text/plain", "content-b".getBytes());

        FileDocumentDto dto1 = objectStorageService.uploadFile(file1, "docs", true);
        FileDocumentDto dto2 = objectStorageService.uploadFile(file2, "docs", true);

        assertNotEquals(dto1.geteTagId(), dto2.geteTagId());
    }

    @Test
    void downloadNonExistentEtag_returnsNull() throws Exception {
        InputStream result = objectStorageService.downloadFileEtag("nonexistent-etag-xyz");
        assertNull(result);
    }

    @Test
    void uploadPrivateFile_returnsPresignedUrl() throws Exception {
        byte[] content = "private content".getBytes();
        MockMultipartFile file = new MockMultipartFile(
                "file", "secret.txt", "text/plain", content);

        FileDocumentDto uploaded = objectStorageService.uploadFile(file, "secure-docs", false);

        assertNotNull(uploaded.getUrl());
        // Presigned URLs contain query parameters
        assertTrue(uploaded.getUrl().contains("?") || uploaded.getUrl().contains("X-Amz"),
                "Expected a presigned URL, got: " + uploaded.getUrl());
    }
}
