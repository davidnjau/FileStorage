package com.dave.filestorage.controller;

import com.dave.filestorage.dto.*;
import com.dave.filestorage.exception.FileNotFoundException;
import com.dave.filestorage.exception.WebhookValidationException;
import com.dave.filestorage.metrics.StorageMetrics;
import com.dave.filestorage.storage.NotificationService;
import com.dave.filestorage.storage.ObjectStorageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FilesController.class)
class FilesControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    ObjectStorageService objectStorageService;

    @MockBean
    NotificationService notificationService;

    @MockBean
    StorageMetrics storageMetrics;

    // ── Upload ──────────────────────────────────────────────────────────────

    @Test
    void uploadFile_success_returnsApiResponseEnvelope() throws Exception {
        FileDocumentDto dto = new FileDocumentDto(
                "id-001", "etag-abc", "photo.jpg", new Date(), 2048L, "http://example.com/photo.jpg");
        when(objectStorageService.uploadFile(any(), anyString(), anyBoolean())).thenReturn(dto);

        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", MediaType.IMAGE_JPEG_VALUE, "fake-image-bytes".getBytes());

        mockMvc.perform(multipart("/files/upload").file(file).param("fileType", "images"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.fileName").value("photo.jpg"))
                .andExpect(jsonPath("$.data.size").value(2048))
                .andExpect(jsonPath("$.error").doesNotExist());
    }

    @Test
    void uploadFile_storageThrowsException_returns500() throws Exception {
        when(objectStorageService.uploadFile(any(), anyString(), anyBoolean()))
                .thenThrow(new RuntimeException("Storage unavailable"));

        MockMultipartFile file = new MockMultipartFile(
                "file", "test.txt", MediaType.TEXT_PLAIN_VALUE, "content".getBytes());

        mockMvc.perform(multipart("/files/upload").file(file))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.error.code").value("INTERNAL_ERROR"));
    }

    // ── Download ────────────────────────────────────────────────────────────

    @Test
    void downloadFile_notFound_returns404() throws Exception {
        when(objectStorageService.downloadFileEtag("missing-etag")).thenReturn(null);

        mockMvc.perform(get("/files/download/missing-etag"))
                .andExpect(status().isNotFound());
    }

    @Test
    void downloadFile_invalidRangeUnit_returns416() throws Exception {
        mockMvc.perform(get("/files/download/some-etag")
                        .header("Range", "kilobytes=0-1023"))
                .andExpect(status().isRequestedRangeNotSatisfiable())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.error.code").value("INVALID_RANGE"));
    }

    @Test
    void downloadFile_malformedRangeNumbers_returns416() throws Exception {
        mockMvc.perform(get("/files/download/some-etag")
                        .header("Range", "bytes=abc-xyz"))
                .andExpect(status().isRequestedRangeNotSatisfiable())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.error.code").value("INVALID_RANGE"));
    }

    @Test
    void downloadFile_startAfterEnd_returns416() throws Exception {
        mockMvc.perform(get("/files/download/some-etag")
                        .header("Range", "bytes=500-100"))
                .andExpect(status().isRequestedRangeNotSatisfiable())
                .andExpect(jsonPath("$.error.code").value("INVALID_RANGE"));
    }

    // ── FileNotFoundException ───────────────────────────────────────────────

    @Test
    void refreshUrl_fileNotFound_returns404WithErrorCode() throws Exception {
        when(objectStorageService.getFileDocumentInformation("bad-id"))
                .thenThrow(new FileNotFoundException("bad-id"));

        mockMvc.perform(get("/files/refresh/bad-id"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.error.code").value("FILE_NOT_FOUND"));
    }

    // ── Webhook SSRF validation ─────────────────────────────────────────────

    @Test
    void registerWebhook_ssrfUrl_returns400() throws Exception {
        when(notificationService.registerWebhook(any()))
                .thenThrow(new WebhookValidationException("Webhook URL must not target a private or loopback address"));

        String body = "{\"bucket\":\"my-bucket\",\"webhookUrl\":\"http://192.168.1.1/hook\",\"events\":[\"s3:ObjectCreated:*\"]}";

        mockMvc.perform(post("/files/webhooks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.error.code").value("INVALID_WEBHOOK_URL"));
    }

    // ── List webhooks pagination ─────────────────────────────────────────────

    @Test
    void listWebhooks_defaultPagination_returnsPaged() throws Exception {
        when(notificationService.listWebhooks(null))
                .thenReturn(Arrays.asList(
                        new WebhookConfigDto("bucket-a", "https://example.com/h1", Collections.emptyList()),
                        new WebhookConfigDto("bucket-b", "https://example.com/h2", Collections.emptyList())
                ));

        mockMvc.perform(get("/files/webhooks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.content", hasSize(2)))
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(20));
    }

    @Test
    void listWebhooks_pageSize1_returnsSingleItem() throws Exception {
        when(notificationService.listWebhooks(null))
                .thenReturn(Arrays.asList(
                        new WebhookConfigDto("bucket-a", "https://example.com/h1", Collections.emptyList()),
                        new WebhookConfigDto("bucket-b", "https://example.com/h2", Collections.emptyList())
                ));

        mockMvc.perform(get("/files/webhooks").param("page", "0").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.totalPages").value(2))
                .andExpect(jsonPath("$.data.hasNext").value(true));
    }

    @Test
    void listWebhooks_page2_returnsSecondItem() throws Exception {
        when(notificationService.listWebhooks(null))
                .thenReturn(Arrays.asList(
                        new WebhookConfigDto("bucket-a", "https://example.com/h1", Collections.emptyList()),
                        new WebhookConfigDto("bucket-b", "https://example.com/h2", Collections.emptyList())
                ));

        mockMvc.perform(get("/files/webhooks").param("page", "1").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.hasNext").value(false));
    }

    // ── List versions pagination ─────────────────────────────────────────────

    @Test
    void listVersions_paginated_returnsCorrectSlice() throws Exception {
        when(objectStorageService.listFileVersions("etag-v", 1, 2)).thenReturn(Arrays.asList(
                new FileVersionDto("v3", new Date(), 300L, "etag-v", true)
        ));

        mockMvc.perform(get("/files/etag-v/versions").param("page", "1").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].versionId").value("v3"))
                .andExpect(jsonPath("$.data.hasNext").value(false));
    }

    // ── Multipart ────────────────────────────────────────────────────────────

    @Test
    void abortMultipart_success_returnsMessage() throws Exception {
        doNothing().when(objectStorageService).abortMultipartUpload(anyString(), anyString(), anyString());

        mockMvc.perform(delete("/files/multipart/upload-id-123/abort")
                        .param("bucket", "test-bucket")
                        .param("objectName", "images/photo.jpg"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data").value("Multipart upload aborted"));
    }
}
