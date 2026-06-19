package com.dave.filestorage.util;

import com.dave.filestorage.exception.WebhookValidationException;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;

/**
 * Validates webhook URLs to prevent SSRF (Server-Side Request Forgery).
 * Rejects private IP ranges, loopback addresses, and non-HTTP/HTTPS schemes.
 */
public final class WebhookUrlValidator {

    private static final List<String> ALLOWED_SCHEMES = Arrays.asList("http", "https");

    private WebhookUrlValidator() {}

    public static void validate(String urlString) {
        if (urlString == null || urlString.isBlank()) {
            throw new WebhookValidationException("Webhook URL must not be blank");
        }
        URI uri;
        try {
            uri = URI.create(urlString);
        } catch (IllegalArgumentException ex) {
            throw new WebhookValidationException("Malformed webhook URL: " + urlString);
        }
        if (uri.getScheme() == null || !ALLOWED_SCHEMES.contains(uri.getScheme().toLowerCase())) {
            throw new WebhookValidationException("Webhook URL must use http or https scheme");
        }
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new WebhookValidationException("Webhook URL must have a valid host");
        }
        try {
            InetAddress address = InetAddress.getByName(host);
            if (address.isLoopbackAddress() || address.isSiteLocalAddress()
                    || address.isAnyLocalAddress() || address.isLinkLocalAddress()) {
                throw new WebhookValidationException("Webhook URL must not target a private or loopback address");
            }
        } catch (UnknownHostException ex) {
            throw new WebhookValidationException("Cannot resolve webhook host: " + host);
        }
    }
}
