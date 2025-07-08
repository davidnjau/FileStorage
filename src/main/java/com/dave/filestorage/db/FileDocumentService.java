package com.dave.filestorage.db;

public interface FileDocumentService {

    /**
     * Saves the given file document to the database.
     *
     * @param fileDocument the file document to be saved
     * @return the saved file document with any updates made during the save process
     */
    FileDocument save(FileDocument fileDocument);

    /**
     * Finds a file document by its unique identifier.
     *
     * @param id the unique identifier of the file document
     * @return the file document with the specified id, or null if not found
     */
    FileDocument findById(String id);

    /**
     * Finds a file document by its object name.
     *
     * @param objectName the object name of the file document
     * @return the file document with the specified object name, or null if not found
     */
    FileDocument findByObjectName(String objectName);

    /**
     * Finds a file document by its ETag.
     *
     * @param etag the ETag of the file document
     * @return the file document with the specified ETag, or null if not found
     */
    FileDocument findByEtag(String etag);
}