package com.dave.filestorage.service;

import com.dave.filestorage.exception.FileNotFoundException;
import com.dave.filestorage.exception.FileStorageException;
import com.dave.filestorage.model.Files;
import com.dave.filestorage.repo.FileRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class FileStorageService {

    @Autowired
    private FileRepo fileRepo;

    public Files storeFile(MultipartFile file) {
        String fileName = StringUtils.cleanPath(file.getOriginalFilename());

        try {
            if(fileName.contains("..")) {
                throw new FileStorageException("Sorry! Filename contains invalid path sequenth " + fileName);
            }

            Files dbFile = new Files(fileName, file.getContentType(), file.getBytes());

            return fileRepo.save(dbFile);

        } catch (IOException ex) {
            throw new FileStorageException("Could not store file " + fileName + ". Please try again!", ex);
        }
    }

    public Files getFile(String fileId) {
        return fileRepo.findById(fileId)
                .orElseThrow(() -> new FileNotFoundException("File you requested not found with id " + fileId));
    }
}
