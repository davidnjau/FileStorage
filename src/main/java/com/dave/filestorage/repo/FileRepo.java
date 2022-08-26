package com.dave.filestorage.repo;

import com.dave.filestorage.model.Files;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FileRepo extends JpaRepository<Files, String> {
}
