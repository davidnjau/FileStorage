package com.dave.filestorage;

import com.dave.filestorage.controller.FilesController;
import com.dave.filestorage.config.StorageProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class FileStorageApplicationTests {

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
    FilesController filesController;

    @Autowired
    StorageProperties storageProperties;

    @Test
    void contextLoads() {
        assertThat(filesController).isNotNull();
    }

    @Test
    void storagePropertiesDefaults() {
        assertThat(storageProperties.getMultipart().getPartSizeBytes()).isEqualTo(5_242_880L);
        assertThat(storageProperties.getPresigned().getPut().getExpiryMinutes()).isEqualTo(15);
        assertThat(storageProperties.getLifecycle().getMultipartExpiryDays()).isEqualTo(7);
    }
}
