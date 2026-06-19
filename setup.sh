#!/usr/bin/env bash
set -e

PROPS="src/main/resources/application.properties"
ENV_FILE="configurations/.env"
GARAGE_TOML="configurations/garage.toml"

# ── Colours ───────────────────────────────────────────────────────────
GREEN='\033[0;32m'; YELLOW='\033[1;33m'; CYAN='\033[0;36m'
GRAY='\033[0;90m'; BOLD='\033[1m'; NC='\033[0m'

# ── Helpers ───────────────────────────────────────────────────────────
ask() {
    local prompt="$1" default="$2" result
    read -rp "$(echo -e "${CYAN}${prompt}${default:+ [${GRAY}${default}${CYAN}]}${NC}: ")" result
    echo "${result:-$default}"
}

ask_secret() {
    # Shows a generated default visibly so the user can accept or override.
    local prompt="$1" default="$2" result
    if [[ -n "$default" ]]; then
        read -rp "$(echo -e "${CYAN}${prompt} [${GRAY}${default}${CYAN}]${NC}: ")" result
    else
        read -rsp "$(echo -e "${CYAN}${prompt}${NC}: ")" result
        echo
    fi
    echo "${result:-$default}"
}

yes_no() {
    local prompt="$1" default="${2:-n}" result
    read -rp "$(echo -e "${CYAN}${prompt} [y/N]${NC}: ")" result
    result="${result:-$default}"
    [[ "$result" =~ ^[Yy]$ ]]
}

gen_password() { openssl rand -base64 18 | tr -d '=/+' | cut -c1-20; }
gen_key()      { openssl rand -hex 16; }
gen_secret()   { openssl rand -base64 32 | tr -d '=/+' | cut -c1-40; }
gen_rpc()      { openssl rand -hex 32; }

# ── Banner ────────────────────────────────────────────────────────────
echo -e "\n${BOLD}${GREEN}=== File Storage Setup ===${NC}"
echo -e "${GRAY}Press Enter to accept auto-generated values, or type to override.${NC}\n"

# ── Provider ──────────────────────────────────────────────────────────
echo -e "${YELLOW}Storage provider${NC}"
echo "  1) MinIO  (self-hosted, easiest to get started)"
echo "  2) Garage (lightweight S3-compatible, great for edge)"
PROVIDER_CHOICE=$(ask "Choose provider" "1")
case "$PROVIDER_CHOICE" in
    2|garage|Garage) PROVIDER="garage" ;;
    *)               PROVIDER="minio"  ;;
esac
echo -e "${GREEN}✓ Provider: $PROVIDER${NC}\n"

# ── MongoDB ───────────────────────────────────────────────────────────
echo -e "${YELLOW}MongoDB${NC}"
_MONGO_USER="fsuser"
_MONGO_PASS=$(gen_password)
_MONGO_DB="file_storage_db"

MONGO_USER=$(ask  "Username"  "$_MONGO_USER")
MONGO_PASS=$(ask_secret "Password" "$_MONGO_PASS")
MONGO_DB=$(ask    "Database"  "$_MONGO_DB")
MONGO_HOST=$(ask  "Host:port" "localhost:27017")
MONGO_URI="mongodb://${MONGO_USER}:${MONGO_PASS}@${MONGO_HOST}/${MONGO_DB}?authSource=admin"
echo -e "${GREEN}✓ MongoDB configured${NC}\n"

# ── Provider credentials ──────────────────────────────────────────────
if [[ "$PROVIDER" == "minio" ]]; then
    echo -e "${YELLOW}MinIO${NC}"
    _MINIO_ACCESS=$(gen_key)
    _MINIO_SECRET=$(gen_secret)

    MINIO_URL=$(ask        "URL"                "http://localhost:9000")
    MINIO_ACCESS=$(ask_secret "Access key"      "$_MINIO_ACCESS")
    MINIO_SECRET=$(ask_secret "Secret key"      "$_MINIO_SECRET")
    MINIO_PUB_BUCKET=$(ask "Public bucket"      "ecommerce-public")
    MINIO_PRIV_BUCKET=$(ask "Private bucket"    "ecommerce-private")
    MINIO_EXPIRY=$(ask     "Presigned URL expiry (hours)" "1")
    echo -e "${GREEN}✓ MinIO configured${NC}\n"
else
    echo -e "${YELLOW}Garage${NC}"
    _GARAGE_ACCESS=$(gen_key)
    _GARAGE_SECRET=$(gen_secret)
    _GARAGE_RPC=$(gen_rpc)

    GARAGE_URL=$(ask         "URL"               "http://localhost:3900")
    GARAGE_REGION=$(ask      "Region"            "garage")
    GARAGE_ACCESS=$(ask_secret "Access key"      "$_GARAGE_ACCESS")
    GARAGE_SECRET=$(ask_secret "Secret key"      "$_GARAGE_SECRET")
    GARAGE_PUB_BUCKET=$(ask  "Public bucket"     "ecommerce-public")
    GARAGE_PRIV_BUCKET=$(ask "Private bucket"    "ecommerce-private")
    GARAGE_EXPIRY=$(ask      "Presigned URL expiry (hours)" "1")
    GARAGE_RPC=$(ask_secret  "RPC secret"        "$_GARAGE_RPC")
    echo -e "${GREEN}✓ Garage configured${NC}\n"
fi

# ── Feature flags ─────────────────────────────────────────────────────
echo -e "${YELLOW}Feature flags${NC}"
if yes_no "Enable object versioning?"; then VERSIONING=true; else VERSIONING=false; fi
if yes_no "Enable SSE-S3 server-side encryption?"; then SSE=true; else SSE=false; fi
CORS_ORIGINS=$(ask "CORS allowed origins" "*")
PUB_EXPIRY=$(ask   "Public bucket lifecycle expiry days  (0 = never)" "365")
PRIV_EXPIRY=$(ask  "Private bucket lifecycle expiry days (0 = never)" "90")
echo -e "${GREEN}✓ Feature flags set${NC}\n"

# ── Write application.properties ──────────────────────────────────────
echo -e "${GREEN}Writing $PROPS ...${NC}"
cat > "$PROPS" <<EOF
spring.application.name=file_manager
server.port=8008

spring.servlet.multipart.max-file-size=100MB
spring.servlet.multipart.max-request-size=100MB

# ── Provider: minio | garage ──────────────────────────────────────────
storage.provider=${PROVIDER}

# MinIO (active when storage.provider=minio)
minio.url=${MINIO_URL:-http://localhost:9000}
minio.accessKey=${MINIO_ACCESS:-}
minio.secretKey=${MINIO_SECRET:-}
minio.publicBucketName=${MINIO_PUB_BUCKET:-ecommerce-public}
minio.privateBucketName=${MINIO_PRIV_BUCKET:-ecommerce-private}
minio.expiry.hours=${MINIO_EXPIRY:-1}

# Garage (active when storage.provider=garage)
garage.url=${GARAGE_URL:-http://localhost:3900}
garage.region=${GARAGE_REGION:-garage}
garage.accessKey=${GARAGE_ACCESS:-}
garage.secretKey=${GARAGE_SECRET:-}
garage.publicBucketName=${GARAGE_PUB_BUCKET:-ecommerce-public}
garage.privateBucketName=${GARAGE_PRIV_BUCKET:-ecommerce-private}
garage.expiry.hours=${GARAGE_EXPIRY:-1}

# ── MongoDB (override via MONGO_URI env var in production) ────────────
spring.data.mongodb.uri=\${MONGO_URI:${MONGO_URI}}
spring.data.mongodb.auto-index-creation=true
spring.data.mongodb.socket-timeout=3000
spring.data.mongodb.connect-timeout=3000

# ── Shared storage config ─────────────────────────────────────────────
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

# ── Notifications ─────────────────────────────────────────────────────
storage.notification.listener.enabled=false
storage.notification.polling.enabled=false
storage.notification.polling.interval-ms=30000

# ── Logging ───────────────────────────────────────────────────────────
logging.level.org.springframework.data.mongodb.core.MongoTemplate=INFO

# ── Actuator ──────────────────────────────────────────────────────────
management.endpoints.web.exposure.include=health,info
management.endpoint.health.show-details=always
management.endpoint.health.probes.enabled=true

# ── Swagger ───────────────────────────────────────────────────────────
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
MONGO_URI=mongodb://${MONGO_USER}:${MONGO_PASS}@mongodb:27017/${MONGO_DB}?authSource=admin

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
    echo -e "${GREEN}✓ Patched $GARAGE_TOML with RPC secret${NC}"
fi

# ── Summary ───────────────────────────────────────────────────────────
echo -e "\n${BOLD}${YELLOW}── Generated credentials ──────────────────────────────${NC}"
echo -e "  MongoDB user:     ${BOLD}${MONGO_USER}${NC}"
echo -e "  MongoDB password: ${BOLD}${MONGO_PASS}${NC}"
echo -e "  MongoDB database: ${BOLD}${MONGO_DB}${NC}"
if [[ "$PROVIDER" == "minio" ]]; then
    echo -e "  MinIO access key: ${BOLD}${MINIO_ACCESS}${NC}"
    echo -e "  MinIO secret key: ${BOLD}${MINIO_SECRET}${NC}"
else
    echo -e "  Garage access key: ${BOLD}${GARAGE_ACCESS}${NC}"
    echo -e "  Garage secret key: ${BOLD}${GARAGE_SECRET}${NC}"
    echo -e "  Garage RPC secret: ${BOLD}${GARAGE_RPC}${NC}"
fi
echo -e "${GRAY}These are also saved in $ENV_FILE${NC}\n"

# ── Docker Compose ────────────────────────────────────────────────────
COMPOSE_STARTED=false
if yes_no "Start docker-compose now?"; then
    echo -e "${GREEN}Starting services (profile: $PROVIDER) ...${NC}"
    docker compose \
        -f configurations/docker-compose.yaml \
        --profile "$PROVIDER" \
        --env-file "$ENV_FILE" \
        up -d
    COMPOSE_STARTED=true
    echo -e "${GREEN}✓ Containers started${NC}"

    if [[ "$PROVIDER" == "garage" ]]; then
        echo -e "\n${YELLOW}Reminder — first-time Garage node setup (run after containers start):${NC}"
        echo -e "  ${GRAY}docker exec garage garage layout assign -z dc1 -c 1G \$(docker exec garage garage node id | head -1)"
        echo -e "  docker exec garage garage layout apply --version 1"
        echo -e "  docker exec garage garage key import --key-id ${GARAGE_ACCESS} --secret-key ${GARAGE_SECRET} my-key"
        echo -e "  docker exec garage garage bucket allow --read --write --owner ecommerce-public --key my-key"
        echo -e "  docker exec garage garage bucket allow --read --write --owner ecommerce-private --key my-key${NC}"
    fi
fi

# ── Wait for dependencies ─────────────────────────────────────────────
wait_for_port() {
    local host="${1}" port="${2}" label="${3}" retries=30 delay=2
    echo -e "${CYAN}Waiting for ${label} on ${host}:${port} ...${NC}"
    for ((i=1; i<=retries; i++)); do
        if nc -z "$host" "$port" 2>/dev/null; then
            echo -e "${GREEN}✓ ${label} is ready${NC}"
            return 0
        fi
        echo -e "${GRAY}  attempt $i/$retries — retrying in ${delay}s ...${NC}"
        sleep "$delay"
    done
    echo -e "${YELLOW}⚠ ${label} not reachable after $((retries * delay))s — continuing anyway${NC}"
    return 1
}

if [[ "$COMPOSE_STARTED" == true ]]; then
    MONGO_PORT="${MONGO_HOST##*:}"
    MONGO_HOST_ONLY="${MONGO_HOST%%:*}"
    wait_for_port "$MONGO_HOST_ONLY" "${MONGO_PORT:-27017}" "MongoDB"

    if [[ "$PROVIDER" == "minio" ]]; then
        MINIO_HOST=$(echo "$MINIO_URL" | sed -E 's|https?://([^:/]+).*|\1|')
        MINIO_PORT=$(echo "$MINIO_URL" | sed -E 's|.*:([0-9]+).*|\1|')
        wait_for_port "$MINIO_HOST" "${MINIO_PORT:-9000}" "MinIO"
    else
        GARAGE_HOST=$(echo "$GARAGE_URL" | sed -E 's|https?://([^:/]+).*|\1|')
        GARAGE_PORT=$(echo "$GARAGE_URL" | sed -E 's|.*:([0-9]+).*|\1|')
        wait_for_port "$GARAGE_HOST" "${GARAGE_PORT:-3900}" "Garage"
    fi
fi

# ── Start backend ─────────────────────────────────────────────────────
echo -e "\n${BOLD}${GREEN}Setup complete.${NC}"
echo -e "API docs:     ${CYAN}http://localhost:8008/swagger-ui.html${NC}"
echo -e "Health check: ${CYAN}http://localhost:8008/actuator/health${NC}\n"

if yes_no "Start the Spring Boot backend now?"; then
    echo -e "${GREEN}Building and starting the backend ...${NC}\n"
    export MONGO_URI="mongodb://${MONGO_USER}:${MONGO_PASS}@${MONGO_HOST}/${MONGO_DB}?authSource=admin"
    ./mvnw spring-boot:run
else
    echo -e "When ready, run:  ${CYAN}./mvnw spring-boot:run${NC}\n"
fi
