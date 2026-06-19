package com.dave.filestorage;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import com.dave.filestorage.config.StorageProperties;

@OpenAPIDefinition(
    info = @Info(
        title = "File Storage API",
        version = "1.0.0",
        description = "S3-compatible file storage microservice supporting MinIO and Garage backends. " +
            "Provides upload, download, multipart, versioning, copy/move, presigned URLs, and webhook notifications.",
        contact = @Contact(name = "Dave", email = "davidnjau21@gmail.com")
    ),
    servers = {
        @Server(url = "http://localhost:8008", description = "Local development")
    }
)
@EnableAsync
@EnableScheduling
@EnableConfigurationProperties(StorageProperties.class)
@SpringBootApplication
public class FileStorageApplication {

    public static void main(String[] args) {
        SpringApplication.run(FileStorageApplication.class, args);
    }

}
