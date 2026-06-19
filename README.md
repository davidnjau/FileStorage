# 📂 File Manager Microservice

A Spring Boot microservice for secure file upload, storage, and metadata tracking using **MinIO** (S3-compatible object store) and **MongoDB** for metadata persistence.

## 🚀 Features
- Upload files to MinIO with dynamic `fileType` paths and SSE-S3 encryption
- Store file metadata (name, size, type, ETag, timestamps, uploader) in MongoDB
- Presigned URL generation for secure private file access
- Direct browser upload via presigned PUT URLs
- Multipart upload for large files with per-part tracking
- Byte-range downloads (partial content / streaming)
- Object versioning — keep and retrieve all versions of a file
- Server-side copy and move between buckets
- Webhook notifications on object created/removed via MinIO SSE stream
- Asynchronous metadata persistence with object tagging
- Lifecycle policies and auto-expiry per bucket
- Health-checked containerized setup via Docker Compose

## 🛠️ Tech Stack
- Java 11, Spring Boot 2.7.3
- MinIO (S3-compatible) via MinIO Java SDK 8.5.2
- MongoDB
- Docker & Docker Compose

## ⚙️ Configuration

Set environment variables in `configurations/.env`:
```env
MINIO_ROOT_USER=
MINIO_ROOT_PASSWORD=
MINIO_BUCKET_NAME=

MONGO_INITDB_ROOT_USERNAME=
MONGO_INITDB_ROOT_PASSWORD=
MONGO_INITDB_DATABASE=
```

Update `src/main/resources/application.properties` with your MinIO credentials and tune feature flags:
```properties
minio.url=http://localhost:9000
minio.accessKey=your-access-key
minio.secretKey=your-secret-key

# Toggle features
storage.versioning.enabled=true
storage.encryption.sse-s3.enabled=true
storage.notification.listener.enabled=true
```

## 🐳 Running with Docker Compose
```bash
docker-compose --env-file configurations/.env -f configurations/docker-compose-combined.yaml up -d
```

## 📤 API Endpoints

### Upload a file
**POST** `/files/upload`

| Parameter  | Type           | Required | Description                                        |
|------------|----------------|----------|----------------------------------------------------|
| `file`     | Multipart file | ✅       | File to upload                                     |
| `fileType` | String         | ❌       | Folder prefix (e.g. `profile-pics`)                |
| `isPublic` | Boolean        | ❌       | `true` = direct URL, `false` = presigned (default: `true`) |

### Download a file
**GET** `/files/download/{eTagId}`

Supports optional `Range: bytes=start-end` header — returns `206 Partial Content`.

### Refresh presigned URL
**GET** `/files/refresh/{id}`

### Multipart upload
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/files/multipart/initiate` | Start a multipart upload, returns `uploadId` |
| POST | `/files/multipart/{uploadId}/part/{partNumber}` | Upload a single part |
| POST | `/files/multipart/complete` | Finalise and assemble all parts |
| DELETE | `/files/multipart/{uploadId}/abort` | Abort and clean up |

### Presigned PUT (direct browser upload)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/files/presigned-upload` | Generate a presigned PUT URL for direct upload |
| POST | `/files/presigned-upload/confirm` | Confirm upload and persist metadata |

### Versioning
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/files/download/{eTagId}/version?versionId=` | Download a specific version |
| GET | `/files/{eTagId}/versions` | List all versions of a file |

### Copy / Move
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/files/copy` | Server-side copy to another bucket or filename |
| POST | `/files/move` | Server-side move (copy + delete source) |

### Webhooks
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/files/webhooks` | Register a webhook for bucket events |
| DELETE | `/files/webhooks/{id}` | Deregister a webhook |
| GET | `/files/webhooks?bucket=` | List registered webhooks |

## 📦 MongoDB Document Structure
```json
{
  "originalFilename": "report.pdf",
  "objectName": "2026/docs/pdf/june/19/report.pdf",
  "bucket": "ecommerce-private",
  "size": 54321,
  "etag": "abc123etag",
  "contentType": "application/pdf",
  "fileUrl": "http://localhost:9000/...",
  "isPublic": false,
  "versionId": "v1",
  "sseAlgorithm": "AES256",
  "expiryHourTime": 1
}
```

## 🧪 Running Tests
```bash
./mvnw test
```

## 📜 License
MIT — © Uptech Organisation
