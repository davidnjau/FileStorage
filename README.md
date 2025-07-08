# 📂 File Manager Microservice

A Spring Boot microservice for secure file upload, storage, and metadata tracking using **MinIO** (S3-compatible object store) and **MongoDB** for metadata persistence.

## 🚀 Features
- Upload files to MinIO with dynamic `fileType` paths
- Store file metadata (name, size, type, ETag, timestamps, uploader) in MongoDB
- Presigned URL generation for secure access
- Asynchronous metadata persistence with retry + MinIO tagging
- Health-checked containerized setup via Docker Compose

## 🛠️ Tech Stack
- Java 17, Spring Boot
- MinIO (S3-compatible)
- MongoDB (with initialization script)
- Docker & Docker Compose
- Lombok, Spring Web, Spring Data MongoDB

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

## 🐳 Running with Docker Compose
```bash
docker-compose --env-file configurations/.env -f configurations/docker-compose-combined.yaml up -d
```

## 📤 File Upload API
**POST** `/upload`

| Parameter   | Type            | Required | Description            |
|-------------|------------------|----------|------------------------|
| `file`      | Multipart file   | ✅       | File to upload         |
| `fileType`  | String           | ❌       | Folder prefix (e.g. `profile-pics`) |

## 📦 MongoDB Document Structure
```json
{
  "originalFilename": "report.pdf",
  "objectName": "docs/report-123.pdf",
  "bucket": "ecommerce-bucket",
  "size": 54321,
  "etag": "abc123etag",
  "contentType": "application/pdf",
  "uploadedBy": "admin",
  "customMetadata": {
    "source": "dashboard"
  }
}
```

## ✅ Health Checks
- MinIO: `curl http://localhost:9000/minio/health/live`
- MongoDB: ping via `mongosh`

## 🧪 Running Tests
```bash
mvn clean test
```

## 📜 License
MIT — © Uptech Organisation
