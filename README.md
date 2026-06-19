# File Manager Microservice

A Spring Boot microservice for secure file upload, storage, and metadata tracking. Supports **MinIO** and **Garage** as interchangeable S3-compatible backends with **MongoDB** for metadata persistence.

## Features

- Upload files with dynamic `fileType` paths and optional SSE-S3 encryption
- Store file metadata (name, size, type, ETag, timestamps) in MongoDB
- Presigned URL generation for secure private file access
- Direct browser upload via presigned PUT URLs
- Multipart upload for large files with per-part tracking
- Byte-range downloads (partial content / video streaming)
- Object versioning — keep and retrieve all versions of a file
- Server-side copy and move between buckets
- Webhook notifications on object created/removed
- Lifecycle policies and auto-expiry per bucket
- Switch providers with a single property — no code changes needed
- Consistent `ApiResponse<T>` envelope on all endpoints
- Paginated list endpoints for webhooks and file versions
- Spring Boot Actuator health checks for storage and MongoDB
- SSRF protection on webhook URL registration
- Structured SLF4J logging throughout

## Tech Stack

- Java 11, Spring Boot 2.7.3, Maven
- **MinIO** via MinIO Java SDK 8.5.2 — or — **Garage** via AWS SDK v2 (`s3:2.20.68`)
- MongoDB via Spring Data
- Docker & Docker Compose (with profiles)
- Testcontainers (integration tests)

## Quick Setup

Run the interactive setup script — it configures your provider, credentials, and optionally starts docker-compose:

```bash
chmod +x setup.sh
./setup.sh
```

The script will ask:
1. Which provider to use (MinIO or Garage)
2. Credentials and endpoint
3. MongoDB connection details
4. Feature flags (versioning, encryption, CORS, lifecycle)
5. Whether to start docker-compose

Then start the app:
```bash
./mvnw spring-boot:run
```

## Manual Configuration

Set `storage.provider` in `src/main/resources/application.properties`:

```properties
# Switch between: minio | garage
storage.provider=minio

# MinIO
minio.url=http://localhost:9000
minio.accessKey=your-access-key
minio.secretKey=your-secret-key

# Garage
garage.url=http://localhost:3900
garage.region=garage
garage.accessKey=your-access-key
garage.secretKey=your-secret-key
```

Set credentials in `configurations/.env`:
```env
MINIO_ROOT_USER=
MINIO_ROOT_PASSWORD=

GARAGE_ACCESS_KEY=
GARAGE_SECRET_KEY=
GARAGE_RPC_SECRET=   # generate with: openssl rand -hex 32

MONGO_INITDB_ROOT_USERNAME=
MONGO_INITDB_ROOT_PASSWORD=
MONGO_INITDB_DATABASE=
```

Set the MongoDB URI via environment variable (never hardcode credentials):
```env
MONGO_URI=mongodb://user:password@localhost:27017/file_storage_db?authSource=admin
```

## Storage Configuration Reference

All tunable values live in `application.properties` under the `storage.*` prefix and are bound via `StorageProperties` (`@ConfigurationProperties`):

| Property | Default | Description |
|----------|---------|-------------|
| `storage.provider` | `minio` | Active backend: `minio` or `garage` |
| `storage.multipart.part-size-bytes` | `5242880` | Part size for multipart uploads (5 MB) |
| `storage.multipart.threshold-bytes` | `10485760` | File size above which multipart is preferred (10 MB) |
| `storage.presigned.put.expiry-minutes` | `15` | Lifetime of presigned PUT URLs |
| `storage.lifecycle.public.expiry-days` | `365` | Auto-expiry for public bucket objects (0 = disabled) |
| `storage.lifecycle.private.expiry-days` | `90` | Auto-expiry for private bucket objects (0 = disabled) |
| `storage.lifecycle.multipart.expiry-days` | `7` | Abort incomplete multipart uploads after N days |
| `storage.versioning.enabled` | `true` | Enable S3 object versioning on buckets |
| `storage.encryption.sse-s3.enabled` | `false` | Enable server-side AES256 encryption |
| `storage.notification.listener.enabled` | `false` | MinIO SSE event listener |
| `storage.notification.polling.enabled` | `false` | Garage change-detection polling |
| `storage.notification.polling.interval-ms` | `30000` | Garage polling interval |

## Docker Compose

Both providers are in a single compose file with profiles. MongoDB always starts.

```bash
# MinIO + MongoDB
docker compose -f configurations/docker-compose.yaml --profile minio --env-file configurations/.env up -d

# Garage + MongoDB
docker compose -f configurations/docker-compose.yaml --profile garage --env-file configurations/.env up -d
```

### First-time Garage node setup
```bash
docker exec garage garage layout assign -z dc1 -c 1G <node-id>
docker exec garage garage layout apply --version 1
docker exec garage garage key create my-key
docker exec garage garage bucket allow --read --write --owner ecommerce-public --key my-key
docker exec garage garage bucket allow --read --write --owner ecommerce-private --key my-key
```

## API Endpoints

All endpoints return a standard `ApiResponse<T>` envelope:

```json
{
  "status": "success",
  "data": { ... },
  "timestamp": 1718831234567
}
```

Errors follow the same shape:
```json
{
  "status": "error",
  "error": { "code": "FILE_NOT_FOUND", "message": "File not found: abc123" },
  "timestamp": 1718831234567
}
```

### Core
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/files/upload` | Upload a file (`file`, `fileType`, `isPublic`) |
| GET | `/files/download/{eTagId}` | Download — supports `Range` header for partial content |
| GET | `/files/refresh/{id}` | Refresh presigned URL |

### Multipart
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/files/multipart/initiate` | Start a multipart upload |
| POST | `/files/multipart/{uploadId}/part/{partNumber}` | Upload a part |
| POST | `/files/multipart/complete` | Finalise upload |
| DELETE | `/files/multipart/{uploadId}/abort` | Abort upload |

### Presigned PUT (direct browser upload)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/files/presigned-upload` | Get a presigned PUT URL |
| POST | `/files/presigned-upload/confirm` | Confirm and persist metadata |

### Versioning
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/files/download/{eTagId}/version?versionId=` | Download a specific version |
| GET | `/files/{eTagId}/versions?page=0&size=50` | List all versions (paginated) |

### Copy / Move
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/files/copy` | Server-side copy |
| POST | `/files/move` | Server-side move |

### Webhooks
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/files/webhooks` | Register a webhook |
| DELETE | `/files/webhooks/{id}` | Deregister a webhook |
| GET | `/files/webhooks?bucket=&page=0&size=20` | List webhooks (paginated) |

Webhook URLs are validated on registration — private IPs, loopback addresses, and non-HTTP(S) schemes are rejected.

## Health & Observability

Spring Boot Actuator is enabled:

```bash
# Overall health (includes storage + MongoDB)
curl http://localhost:8008/actuator/health

# App info
curl http://localhost:8008/actuator/info
```

The health response includes a custom `minioStorage` or `garageStorage` indicator that actively pings the backend:

```json
{
  "status": "UP",
  "components": {
    "minioStorage": { "status": "UP", "details": { "provider": "minio" } },
    "mongo":        { "status": "UP" }
  }
}
```

## API Docs

Swagger UI: `http://localhost:8008/swagger-ui.html`

All endpoints, request bodies, and response schemas are fully annotated with OpenAPI 3 descriptions.

## Running Tests

### Unit + controller slice tests (no Docker required)
```bash
./mvnw test -Dtest="S3NamingSanitizerTest,WebhookUrlValidatorTest,ApiResponseTest,PagedResultTest,FilesControllerTest"
```

### Repository tests (requires Docker for MongoDB container)
```bash
./mvnw test -Dtest="FileDocumentRepositoryTest,NotificationWebhookConfigRepositoryTest,GaragePollStateRepositoryTest"
```

### Full integration tests (requires Docker for MinIO + MongoDB containers)
```bash
./mvnw test
```

Testcontainers pulls and starts MinIO and MongoDB automatically — no manual setup needed.

### Run a single test
```bash
./mvnw test -Dtest=FilesControllerTest#uploadFile_success_returnsApiResponseEnvelope
```

## Project Structure

```
src/main/java/com/dave/filestorage/
├── FileStorageApplication.java          # Entry point, @EnableAsync, @EnableScheduling
├── config/
│   └── StorageProperties.java           # @ConfigurationProperties for all storage tunables
├── controller/
│   └── FilesController.java             # REST endpoints, ApiResponse envelope
├── db/
│   ├── FileDocument.java                # MongoDB document + compound indexes
│   ├── FileDocumentRepository.java
│   ├── FileDocumentService.java
│   ├── GaragePollState.java             # Garage change-detection state per bucket
│   └── NotificationWebhookConfig.java
├── dto/
│   ├── ApiResponse.java                 # Standard response envelope
│   ├── PagedResult.java                 # Pagination wrapper
│   └── ...                              # Feature-specific DTOs
├── exception/
│   ├── FileStorageException.java        # Base exception with error code
│   ├── FileNotFoundException.java
│   ├── MultipartUploadException.java
│   ├── WebhookValidationException.java
│   └── GlobalExceptionHandler.java      # @RestControllerAdvice
├── health/
│   ├── MinioHealthIndicator.java        # Actuator health for MinIO
│   └── GarageHealthIndicator.java       # Actuator health for Garage
├── storage/
│   ├── ObjectStorageService.java        # Shared interface
│   ├── NotificationService.java         # Shared interface
│   ├── S3NamingSanitizer.java
│   ├── minio/                           # MinIO implementation
│   └── garage/                          # Garage implementation
└── util/
    └── WebhookUrlValidator.java         # SSRF protection

src/test/java/com/dave/filestorage/
├── FileStorageApplicationTests.java     # Context load test (Testcontainers)
├── unit/                                # Pure unit tests, no containers
├── controller/                          # @WebMvcTest slice tests
├── db/                                  # @DataMongoTest + MongoDB container
└── integration/                         # Full stack with MinIO + MongoDB containers
```

## License

MIT — © Uptech Organisation
