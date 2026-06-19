package com.dave.filestorage.unit;

import com.dave.filestorage.dto.ApiResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ApiResponseTest {

    @Test
    void success_setsStatusAndData() {
        ApiResponse<String> response = ApiResponse.success("hello");
        assertEquals("success", response.getStatus());
        assertEquals("hello", response.getData());
        assertNull(response.getError());
        assertTrue(response.getTimestamp() > 0);
    }

    @Test
    void error_setsStatusAndError() {
        ApiResponse<Void> response = ApiResponse.error("FILE_NOT_FOUND", "File missing");
        assertEquals("error", response.getStatus());
        assertNull(response.getData());
        assertNotNull(response.getError());
        assertEquals("FILE_NOT_FOUND", response.getError().getCode());
        assertEquals("File missing", response.getError().getMessage());
    }
}
