# Stage 1: build
FROM maven:3.6.3-jdk-11-slim AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests -q

# Stage 2: run
FROM openjdk:11-jre-slim
RUN addgroup --system filestorage && adduser --system --ingroup filestorage filestorage
WORKDIR /app
COPY --from=builder /app/target/FileStorage.jar app.jar
RUN chown filestorage:filestorage app.jar
USER filestorage
EXPOSE 8008
HEALTHCHECK --interval=30s --timeout=5s --start-period=15s --retries=3 \
    CMD curl -f http://localhost:8008/actuator/health || exit 1
ENTRYPOINT ["java", \
    "-Xmx512m", "-Xms256m", \
    "-XX:+UseG1GC", "-XX:MaxGCPauseMillis=200", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]
