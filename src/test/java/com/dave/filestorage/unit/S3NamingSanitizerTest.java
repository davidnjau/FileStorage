package com.dave.filestorage.unit;

import com.dave.filestorage.storage.S3NamingSanitizer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class S3NamingSanitizerTest {

    @Test
    void nullInput_returnsGeneric() {
        assertEquals("generic", S3NamingSanitizer.sanitizeOrDefault(null));
    }

    @Test
    void emptyInput_returnsGeneric() {
        assertEquals("generic", S3NamingSanitizer.sanitizeOrDefault("  "));
    }

    @ParameterizedTest(name = "sanitize(''{0}'') = ''{1}''")
    @CsvSource({
        "images,           images",
        "MY_IMAGES,        my-images",
        "video files,      video-files",
        "docs/2024,        docs-2024",
        "UPPER_CASE_TYPE,  upper-case-type",
    })
    void validInputs_sanitizedCorrectly(String input, String expected) {
        assertEquals(expected.trim(), S3NamingSanitizer.sanitizeOrDefault(input.trim()));
    }

    @Test
    void ipAddressLike_sanitizesToHyphenated() {
        // Dots in an IP address are replaced with hyphens by the sanitizer;
        // the result passes S3 validation because the IP-format check requires dots.
        assertEquals("192-168-1-1", S3NamingSanitizer.sanitizeOrDefault("192.168.1.1"));
    }

    @Test
    void tooShort_returnsGeneric() {
        assertEquals("generic", S3NamingSanitizer.sanitizeOrDefault("ab"));
    }

    @Test
    void validName_passesThrough() {
        assertEquals("images", S3NamingSanitizer.sanitizeOrDefault("images"));
    }
}
