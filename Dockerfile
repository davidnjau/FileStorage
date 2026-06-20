# Stage 1: build
FROM maven:3-eclipse-temurin-11 AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests -q

# Stage 2: run
FROM eclipse-temurin:11-jre
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && addgroup --system filestorage \
    && adduser --system --ingroup filestorage filestorage
ARG STORAGE_PROVIDER=minio
ENV STORAGE_PROVIDER=${STORAGE_PROVIDER}

WORKDIR /app
COPY --from=builder /app/target/FileStorage.jar app.jar
RUN chown filestorage:filestorage app.jar
USER filestorage
EXPOSE 8008
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
    CMD curl -sf http://localhost:8008/actuator/health || exit 1
ENTRYPOINT ["java", \
    "-Xmx512m", "-Xms256m", \
    "-XX:+UseG1GC", "-XX:MaxGCPauseMillis=200", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]
