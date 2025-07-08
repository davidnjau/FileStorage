package com.dave.filestorage.db;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FileDocumentRepository extends MongoRepository<FileDocument, String> {
    /**
     * Retrieves a FileDocument by its object name.
     *
     * @param objectName the name of the object to search for.
     * @return an Optional containing the FileDocument if found, or an empty Optional if not found.
     */
    Optional<FileDocument> findByObjectName(String objectName);

    /**
     * Retrieves a FileDocument by its ETag.
     *
     * @param etag the ETag of the file document to search for.
     * @return an Optional containing the FileDocument if found, or an empty Optional if not found.
     */
    Optional<FileDocument> findFileDocumentByEtag(String etag);
}