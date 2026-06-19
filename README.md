# 📂 File Manager Microservice

A Spring Boot microservice for secure file upload, storage, and metadata tracking using **Garage** (S3-compatible object store) and **MongoDB** for metadata persistence.

## 🚀 Features
- Upload files to Garage with dynamic `fileType` paths
- Store file metadata (name, size, type, ETag, timestamps, uploader) in MongoDB
- Presigned URL generation for secure private file access
- Public/private bucket support with S3 bucket policies
- Asynchronous metadata persistence with object tagging via AWS SDK v2

## 🛠️ Tech Stack
- Java 11, Spring Boot 2.7
- Garage (S3-compatible object store) via AWS SDK v2
- MongoDB
- Docker & Docker Compose

## ⚙️ Configuration

### 1. Generate the Garage RPC secret
```bash
openssl rand -hex 32
```

### 2. Set environment variables in `configurations/.env`
```env
GARAGE_ACCESS_KEY=
GARAGE_SECRET_KEY=
GARAGE_RPC_SECRET=

MONGO_INITDB_ROOT_USERNAME=
MONGO_INITDB_ROOT_PASSWORD=
MONGO_INITDB_DATABASE=
```

### 3. Update `configurations/garage.toml`
Replace `rpc_secret` with the value generated above.

### 4. Update `src/main/resources/application.properties`
```properties
garage.accessKey=your-garage-access-key
garage.secretKey=your-garage-secret-key
```

## 🐳 Running with Docker Compose
```bash
docker-compose --env-file configurations/.env -f configurations/docker-compose-combined.yaml up -d
```

### First-time Garage setup
After the container starts, create a layout, key, and allow bucket access:
```bash
# Assign the node to a zone
docker exec garage garage layout assign -z dc1 -c 1G <node-id>
docker exec garage garage layout apply --version 1

# Create an access key
docker exec garage garage key create my-key

# Allow the key to access your buckets
docker exec garage garage bucket allow --read --write --owner ecommerce-public --key my-key
docker exec garage garage bucket allow --read --write --owner ecommerce-private --key my-key
```
The node ID is printed in the container logs on first startup.

## 📤 API Endpoints

### Upload a file
**POST** `/files/upload`

| Parameter  | Type           | Required | Description                              |
|------------|----------------|----------|------------------------------------------|
| `file`     | Multipart file | ✅       | File to upload                           |
| `fileType` | String         | ❌       | Folder prefix (e.g. `profile-pics`)      |
| `isPublic` | Boolean        | ❌       | `true` = direct URL, `false` = presigned (default: `true`) |

### Download a file
**GET** `/files/download/{eTagId}`

### Refresh presigned URL
**GET** `/files/refresh/{id}`

## 📦 MongoDB Document Structure
```json
{
  "originalFilename": "report.pdf",
  "objectName": "2026/docs/pdf/june/19/report.pdf",
  "bucket": "ecommerce-private",
  "size": 54321,
  "etag": "abc123etag",
  "contentType": "application/pdf",
  "fileUrl": "http://localhost:3900/...",
  "isPublic": false,
  "expiryHourTime": 1
}
```

## 🧪 Running Tests
```bash
./mvnw test
```

## 📜 License
MIT — © Uptech Organisation
