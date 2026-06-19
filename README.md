# 📂 File Manager Microservice

A Spring Boot microservice for secure file upload, storage, and metadata tracking supporting **MinIO** and **Garage** as interchangeable S3-compatible backends, with **MongoDB** for metadata persistence.

## 🚀 Features
- Upload files with dynamic `fileType` paths and SSE-S3 encryption
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

## 🛠️ Tech Stack
- Java 11, Spring Boot 2.7.3
- **MinIO** via MinIO Java SDK 8.5.2 — or — **Garage** via AWS SDK v2
- MongoDB
- Docker & Docker Compose (with profiles)

## ⚡ Quick Setup

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

## ⚙️ Manual Configuration

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

## 🐳 Docker Compose

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
The node ID is printed in the container logs on first startup.

## 📤 API Endpoints

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
| GET | `/files/{eTagId}/versions` | List all versions |

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
| GET | `/files/webhooks?bucket=` | List webhooks |

API docs: `http://localhost:8008/swagger-ui.html`

## 🧪 Running Tests
```bash
./mvnw test
```

## 📜 License
MIT — © Uptech Organisation
