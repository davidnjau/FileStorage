#!/usr/bin/env bash
set -e

PROPS="src/main/resources/application.properties"
ENV_FILE="configurations/.env"
GARAGE_TOML="configurations/garage.toml"

# ── Colours ──────────────────────────────────────────────────────────
GREEN='\033[0;32m'; YELLOW='\033[1;33m'; CYAN='\033[0;36m'; NC='\033[0m'

ask() {
    local prompt="$1" default="$2" result
    read -rp "$(echo -e "${CYAN}${prompt}${default:+ [$default]}${NC}: ")" result
    echo "${result:-$default}"
}

ask_secret() {
    local prompt="$1" result
    read -rsp "$(echo -e "${CYAN}${prompt}${NC}: ")" result
    echo
    echo "$result"
}

yes_no() {
    local prompt="$1" default="${2:-n}" result
    read -rp "$(echo -e "${CYAN}${prompt} [y/N]${NC}: ")" result
    result="${result:-$default}"
    [[ "$result" =~ ^[Yy]$ ]]
}

echo -e "\n${GREEN}=== File Storage Setup ===${NC}\n"

# ── Provider ─────────────────────────────────────────────────────────
echo "Available providers:"
echo "  1) MinIO"
echo "  2) Garage"
PROVIDER_CHOICE=$(ask "Choose provider" "1")
case "$PROVIDER_CHOICE" in
    2|garage|Garage) PROVIDER="garage" ;;
    *)               PROVIDER="minio"  ;;
esac
echo -e "${GREEN}Provider: $PROVIDER${NC}\n"

# ── MongoDB ───────────────────────────────────────────────────────────
echo -e "${YELLOW}MongoDB configuration${NC}"
MONGO_USER=$(ask "Username" "civaLENs")
MONGO_PASS=$(ask_secret "Password")
MONGO_DB=$(ask "Database" "file_storage_db")
MONGO_HOST=$(ask "Host:port" "localhost:27017")
MONGO_URI="mongodb://${MONGO_USER}:${MONGO_PASS}@${MONGO_HOST}/${MONGO_DB}?authSource=admin"

# ── Provider credentials ──────────────────────────────────────────────
if [[ "$PROVIDER" == "minio" ]]; then
    echo -e "\n${YELLOW}MinIO configuration${NC}"
    MINIO_URL=$(ask "MinIO URL" "http://localhost:9000")
    MINIO_ACCESS=$(ask "Access key")
    MINIO_SECRET=$(ask_secret "Secret key")
    MINIO_PUB_BUCKET=$(ask "Public bucket name" "ecommerce-public")
    MINIO_PRIV_BUCKET=$(ask "Private bucket name" "ecommerce-private")
    MINIO_EXPIRY=$(ask "Presigned URL expiry hours" "1")
else
    echo -e "\n${YELLOW}Garage configuration${NC}"
    GARAGE_URL=$(ask "Garage URL" "http://localhost:3900")
    GARAGE_REGION=$(ask "Region" "garage")
    GARAGE_ACCESS=$(ask "Access key")
    GARAGE_SECRET=$(ask_secret "Secret key")
    GARAGE_PUB_BUCKET=$(ask "Public bucket name" "ecommerce-public")
    GARAGE_PRIV_BUCKET=$(ask "Private bucket name" "ecommerce-private")
    GARAGE_EXPIRY=$(ask "Presigned URL expiry hours" "1")
    GARAGE_RPC=$(ask_secret "RPC secret (leave blank to auto-generate)")
    if [[ -z "$GARAGE_RPC" ]]; then
        GARAGE_RPC=$(openssl rand -hex 32)
        echo -e "${GREEN}Generated RPC secret.${NC}"
    fi
fi

# ── Shared options ────────────────────────────────────────────────────
echo -e "\n${YELLOW}Feature flags${NC}"
if yes_no "Enable versioning?"; then VERSIONING=true; else VERSIONING=false; fi
if yes_no "Enable SSE-S3 encryption?"; then SSE=true; else SSE=false; fi
CORS_ORIGINS=$(ask "CORS allowed origins" "*")
PUB_EXPIRY=$(ask "Public bucket lifecycle expiry days (0=never)" "365")
PRIV_EXPIRY=$(ask "Private bucket lifecycle expiry days (0=never)" "90")

# ── Write application.properties ─────────────────────────────────────
echo -e "\n${GREEN}Writing $PROPS ...${NC}"
cat > "$PROPS" <<EOF
spring.application.name=file_manager
server.port=8008

spring.servlet.multipart.max-file-size=100MB
spring.servlet.multipart.max-request-size=100MB
spring.rateLimitCapacity=rateLimitCapacity

# Provider: minio | garage
storage.provider=${PROVIDER}

# MinIO
minio.url=${MINIO_URL:-http://localhost:9000}
minio.accessKey=${MINIO_ACCESS:-}
minio.secretKey=${MINIO_SECRET:-}
minio.publicBucketName=${MINIO_PUB_BUCKET:-ecommerce-public}
minio.privateBucketName=${MINIO_PRIV_BUCKET:-ecommerce-private}
minio.expiry.hours=${MINIO_EXPIRY:-1}

# Garage
garage.url=${GARAGE_URL:-http://localhost:3900}
garage.region=${GARAGE_REGION:-garage}
garage.accessKey=${GARAGE_ACCESS:-}
garage.secretKey=${GARAGE_SECRET:-}
garage.publicBucketName=${GARAGE_PUB_BUCKET:-ecommerce-public}
garage.privateBucketName=${GARAGE_PRIV_BUCKET:-ecommerce-private}
garage.expiry.hours=${GARAGE_EXPIRY:-1}

# MongoDB
spring.data.mongodb.uri=${MONGO_URI}
spring.data.mongodb.auto-index-creation=true
spring.data.mongodb.socket-timeout=3000
spring.data.mongodb.connect-timeout=3000

# Shared storage config
storage.multipart.threshold-bytes=10485760
storage.multipart.part-size-bytes=5242880
storage.lifecycle.public.expiry-days=${PUB_EXPIRY}
storage.lifecycle.private.expiry-days=${PRIV_EXPIRY}
storage.lifecycle.multipart.expiry-days=7
storage.presigned.put.expiry-minutes=15
storage.versioning.enabled=${VERSIONING}
storage.encryption.sse-s3.enabled=${SSE}
storage.cors.allowed-origins=${CORS_ORIGINS}
storage.cors.max-age-seconds=3600

# Notifications
storage.notification.listener.enabled=false
storage.notification.polling.enabled=false
storage.notification.polling.interval-ms=30000

# Logging
logging.level.org.springframework.data.mongodb.core.MongoTemplate=INFO

# Swagger
springdoc.swagger-ui.disable-swagger-default-url=true
api.response-codes.ok.desc=OK
api.response-codes.badRequest.desc=BAD_REQUEST
api.response-codes.notFound.desc=NOT_FOUND
EOF

# ── Write .env ────────────────────────────────────────────────────────
echo -e "${GREEN}Writing $ENV_FILE ...${NC}"
cat > "$ENV_FILE" <<EOF
# MongoDB
MONGO_INITDB_ROOT_USERNAME=${MONGO_USER}
MONGO_INITDB_ROOT_PASSWORD=${MONGO_PASS}
MONGO_INITDB_DATABASE=${MONGO_DB}

# MinIO
MINIO_ROOT_USER=${MINIO_ACCESS:-}
MINIO_ROOT_PASSWORD=${MINIO_SECRET:-}

# Garage
GARAGE_ACCESS_KEY=${GARAGE_ACCESS:-}
GARAGE_SECRET_KEY=${GARAGE_SECRET:-}
GARAGE_RPC_SECRET=${GARAGE_RPC:-}
EOF

# ── Patch garage.toml ─────────────────────────────────────────────────
if [[ "$PROVIDER" == "garage" && -n "$GARAGE_RPC" && -f "$GARAGE_TOML" ]]; then
    sed -i.bak "s|^rpc_secret = .*|rpc_secret = \"${GARAGE_RPC}\"|" "$GARAGE_TOML"
    rm -f "${GARAGE_TOML}.bak"
    echo -e "${GREEN}Patched $GARAGE_TOML with RPC secret.${NC}"
fi

# ── Docker Compose ────────────────────────────────────────────────────
echo
if yes_no "Start docker-compose now?"; then
    echo -e "${GREEN}Starting services (profile: $PROVIDER) ...${NC}"
    docker compose \
        -f configurations/docker-compose.yaml \
        --profile "$PROVIDER" \
        --env-file "$ENV_FILE" \
        up -d
    echo -e "${GREEN}Done. MongoDB + ${PROVIDER} are starting up.${NC}"

    if [[ "$PROVIDER" == "garage" ]]; then
        echo -e "\n${YELLOW}Reminder: Garage requires first-time node setup after startup:${NC}"
        echo "  docker exec garage garage layout assign -z dc1 -c 1G <node-id>"
        echo "  docker exec garage garage layout apply --version 1"
        echo "  docker exec garage garage key create my-key"
        echo "  docker exec garage garage bucket allow --read --write --owner ecommerce-public --key my-key"
        echo "  docker exec garage garage bucket allow --read --write --owner ecommerce-private --key my-key"
    fi
fi

echo -e "\n${GREEN}Setup complete. Run:  ./mvnw spring-boot:run${NC}\n"
