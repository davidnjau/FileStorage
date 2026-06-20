package com.dave.filestorage.exception;

public class FileNotFoundException extends FileStorageException {
    public FileNotFoundException(String identifier) {
        super(ErrorCode.FILE_NOT_FOUND, "File not found: " + identifier);
    }
}
