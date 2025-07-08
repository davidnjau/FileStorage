package com.dave.filestorage.db;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class FileDocumentServiceImpl implements FileDocumentService{

    @Autowired
    private FileDocumentRepository fileDocumentRepository;

    /**
     * Saves a FileDocument entity to the repository.
     *
     * @param fileDocument the FileDocument entity to be saved
     * @return the saved FileDocument entity
     */
    @Override
    public FileDocument save(FileDocument fileDocument) {
        return fileDocumentRepository.save(fileDocument);
    }

    /**
     * Retrieves a FileDocument entity by its unique identifier.
     *
     * @param id the unique identifier of the FileDocument entity to be retrieved
     * @return the FileDocument entity if found, otherwise null
     */
    @Override
    public FileDocument findById(String id) {

        Optional<FileDocument> optionalFileDocument = fileDocumentRepository.findById(id);
        return optionalFileDocument.orElse(null);

    }

    /**
     * Retrieves a FileDocument entity by its object name.
     *
     * @param objectName the object name of the FileDocument entity to be retrieved
     * @return the FileDocument entity if found, otherwise null
     */
    @Override
    public FileDocument findByObjectName(String objectName) {

        Optional<FileDocument> optionalFileDocument = fileDocumentRepository.findByObjectName(objectName);
        return optionalFileDocument.orElse(null);

    }

    /**
     * Retrieves a FileDocument entity by its ETag.
     *
     * @param etag the ETag of the FileDocument entity to be retrieved
     * @return the FileDocument entity if found, otherwise null
     */
    @Override
    public FileDocument findByEtag(String etag) {

        Optional<FileDocument> optionalFileDocument = fileDocumentRepository.findFileDocumentByEtag(etag);
        return optionalFileDocument.orElse(null);

    }
}