package com.dave.filestorage.exception;

public class MultipartUploadException extends FileStorageException {
    public MultipartUploadException(String message, Throwable cause) {
        super("MULTIPART_ERROR", message, cause);
    }
}
