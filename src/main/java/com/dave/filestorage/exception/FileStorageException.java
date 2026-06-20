package com.dave.filestorage.exception;

public class FileStorageException extends RuntimeException {
    private final ErrorCode errorCode;

    public FileStorageException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public FileStorageException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() { return errorCode; }
}
