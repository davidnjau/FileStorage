package com.dave.filestorage.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class FilesControllerIntegrationTest {

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
    MockMvc mockMvc;

    @Test
    void uploadAndDownload_httpRoundTrip() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.txt", MediaType.TEXT_PLAIN_VALUE, "hello world".getBytes());

        // Upload
        String uploadResponse = mockMvc.perform(multipart("/files/upload")
                        .file(file).param("fileType", "documents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.fileName").value("test.txt"))
                .andExpect(jsonPath("$.data.eTagId").exists())
                .andReturn().getResponse().getContentAsString();

        // Extract eTagId
        String eTagId = com.fasterxml.jackson.databind.json.JsonMapper.builder().build()
                .readTree(uploadResponse).path("data").path("eTagId").asText();

        // Download
        mockMvc.perform(get("/files/download/" + eTagId))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString(eTagId)));
    }

    @Test
    void registerWebhook_invalidUrl_returns400() throws Exception {
        String body = "{\"bucket\":\"test-bucket\",\"webhookUrl\":\"http://127.0.0.1/hook\",\"events\":[\"s3:ObjectCreated:*\"]}";

        mockMvc.perform(post("/files/webhooks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.error.code").value("INVALID_WEBHOOK_URL"));
    }

    @Test
    void listWebhooks_empty_returnsEmptyPage() throws Exception {
        mockMvc.perform(get("/files/webhooks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.content", hasSize(0)))
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }

    @Test
    void actuatorHealth_returnsUp() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
