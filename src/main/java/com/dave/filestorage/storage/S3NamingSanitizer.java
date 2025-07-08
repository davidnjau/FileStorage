package com.dave.filestorage.storage;

public class S3NamingSanitizer {

    /**
     * Sanitizes a given raw name to ensure it complies with S3/MinIO bucket naming rules.
     * The function normalizes the input by converting it to lowercase, replacing invalid
     * characters with hyphens, and removing leading or trailing hyphens. If the sanitized
     * name does not meet the S3 bucket naming criteria, a default value "generic" is returned.
     *
     * @param rawName the original name to be sanitized. It can be null or empty.
     * @return a sanitized name that complies with S3 bucket naming rules, or "generic" if
     *         the name cannot be sanitized into a valid format.
     */
    public static String sanitizeOrDefault(String rawName) {
        if (rawName == null || rawName.trim().isEmpty()) {
            return "generic";
        }

        // Step 1: Normalize
        String cleaned = rawName.trim().toLowerCase();

        // Replace all invalid characters with hyphens
        cleaned = cleaned.replaceAll("[^a-z0-9-]", "-");

        // Replace consecutive hyphens with a single hyphen
        cleaned = cleaned.replaceAll("-{2,}", "-");

        // Remove leading/trailing hyphens
        cleaned = cleaned.replaceAll("^-+", "").replaceAll("-+$", "");

        // Step 2: Final validation
        if (isValidS3BucketName(cleaned)) {
            return cleaned;
        } else {
            return "generic";
        }
    }

    /**
     * Validates whether a given string is a valid S3 bucket name.
     * 
     * The validation checks include:
     * - The name must be between 3 and 63 characters long.
     * - The name can only contain lowercase letters, numbers, and hyphens.
     * - The name cannot start or end with a hyphen.
     * - The name must not be formatted as an IP address.
     *
     * @param name the string to be validated as an S3 bucket name. It can be null.
     * @return true if the name is a valid S3 bucket name, false otherwise.
     */
    private static boolean isValidS3BucketName(String name) {
        if (name == null) return false;

        // Length
        if (name.length() < 3 || name.length() > 63) return false;

        // Allowed pattern
        if (!name.matches("^[a-z0-9-]+$")) return false;

        // No leading/trailing hyphen
        if (name.startsWith("-") || name.endsWith("-")) return false;

        // Not an IP address
        if (name.matches("^(\\d{1,3}\\.){3}\\d{1,3}$")) return false;

        return true;
    }
}