# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Run locally
mvn spring-boot:run

# Build JAR
mvn clean package

# Run tests
mvn test

# Run a single test class
mvn test -Dtest=FileStorageApplicationTests

# Start PostgreSQL via Docker
docker run --name myPostgresDb -p 5455:5432 \
  -e POSTGRES_USER=postgresUser \
  -e POSTGRES_PASSWORD=postgresPW \
  -e POSTGRES_DB=files -d postgres

# Build and run Docker image
docker build -t file-storage .
docker run -p 8081:8081 file-storage
```

API docs available at `http://localhost:8081/swagger-ui.html` once running.

## Architecture

Spring Boot REST API (Java 11, Maven) with a standard layered structure:

```
FilesController  →  FileStorageService  →  FileRepo (JPA)  →  PostgreSQL
```

Files are stored as BLOBs in the database — there is no filesystem storage despite the `file.upload-dir` property in `application.properties` (that property is currently unused by the service).

**Key flows:**
- `POST /upload-file` — multipart upload; service validates against path traversal, stores binary content + metadata (`id`, `name`, `type`) as a `Files` entity, returns a `FileUploadResponse` with the download URI.
- `GET /download-file/{fileId}` — looks up by UUID, streams bytes back with correct `Content-Type`.

**Exception handling:** `FileStorageException` (500) and `FileNotFoundException` (404) are the two custom exceptions. They are thrown from the service layer and should bubble up to Spring's default error handling.

## Configuration

`src/main/resources/application.properties` — the database URL points to `0.0.0.0:5455` (the Docker-mapped port). Credentials match the `docker run` command above. DDL is set to `update`, so Hibernate manages schema migrations automatically.

Max upload size: 100 MB per file / 125 MB per request.
