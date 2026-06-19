package com.dave.filestorage.unit;

import com.dave.filestorage.exception.WebhookValidationException;
import com.dave.filestorage.util.WebhookUrlValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class WebhookUrlValidatorTest {

    @Test
    void nullUrl_throwsException() {
        assertThrows(WebhookValidationException.class,
                () -> WebhookUrlValidator.validate(null));
    }

    @Test
    void blankUrl_throwsException() {
        assertThrows(WebhookValidationException.class,
                () -> WebhookUrlValidator.validate("   "));
    }

    @ParameterizedTest(name = "private URL ''{0}'' should be rejected")
    @ValueSource(strings = {
        "http://localhost/hook",
        "http://127.0.0.1/hook",
        "http://10.0.0.1/hook",
        "http://192.168.1.100/hook",
        "http://172.16.0.1/hook",
    })
    void privateOrLoopbackAddress_throwsException(String url) {
        assertThrows(WebhookValidationException.class,
                () -> WebhookUrlValidator.validate(url));
    }

    @ParameterizedTest(name = "bad scheme ''{0}'' should be rejected")
    @ValueSource(strings = {
        "ftp://example.com/hook",
        "file:///etc/passwd",
        "javascript://example.com/hook",
    })
    void nonHttpScheme_throwsException(String url) {
        assertThrows(WebhookValidationException.class,
                () -> WebhookUrlValidator.validate(url));
    }

    @Test
    void malformedUrl_throwsException() {
        assertThrows(WebhookValidationException.class,
                () -> WebhookUrlValidator.validate("not-a-url"));
    }
}
