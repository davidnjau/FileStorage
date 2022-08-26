FROM maven:3.6.3-jdk-11-slim AS build
FROM openjdk:11
EXPOSE 8081
WORKDIR /file_storage
COPY . /file_storage
ADD target/FileStorage.jar FileStorage.jar
ENTRYPOINT ["java","-jar","target/FileStorage.jar"]