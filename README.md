# File storage

[![Build Status](https://travis-ci.org/codecentric/springboot-sample-app.svg?branch=master)](https://travis-ci.org/codecentric/springboot-sample-app)
[![Coverage Status](https://coveralls.io/repos/github/codecentric/springboot-sample-app/badge.svg?branch=master)](https://coveralls.io/github/codecentric/springboot-sample-app?branch=master)
[![License](http://img.shields.io/:license-apache-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0.html)

SmsManager [Spring Boot](http://projects.spring.io/spring-boot/).

## Requirements

For building and running the application you need:

- [JDK 1.8](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)
- [Maven 3](https://maven.apache.org)
- [Postgresql](https://https://www.postgresql.org/)

## Running the application locally

```shell
mvn spring-boot:run
```
## Accessing Swagger

You can access the swagger UI platform from:

```shell
http://{{host_url}}:8081/swagger-ui.html
```

Make sure to add the open-ui swagger
```shell
    <dependency>
        <groupId>org.springdoc</groupId>
        <artifactId>springdoc-openapi-ui</artifactId>
        <version>1.6.11</version>
    </dependency>
 ```
Create the /var/www/upload/ directory 
```shell
    mkdir /var/www/upload/
```
Make the directory writable
```shell
    chmod -R 777 /var/www/upload/
```
Make the directory readable
```shell
    chmod -R 755 /var/www/upload/
```

## Install Docker
Use the following link to install docker
```shell
https://www.digitalocean.com/community/tutorials/how-to-install-and-use-docker-on-ubuntu-20-04
```

Create Postgres Database
```shell
    docker run --name myPostgresDb -p 5455:5432 -e POSTGRES_USER=postgresUser -e POSTGRES_PASSWORD=postgresPW -e POSTGRES_DB=postgresDB -d postgres
```

In case of this error
```shell
    docker: Error response from daemon: Conflict. The container name "/myPostgresDb" is already in use by container "937c5c6847275dfc5253cd646acbe97611c87621a42a71bb2e54a5f08ad90742". You have to remove (or rename) that container to be able to reuse that name.
```
Prune the docker container
```shell
    docker container prune
```

## Copyright

Released under the Apache License 2.0. See the [LICENSE](https://github.com/codecentric/springboot-sample-app/blob/master/LICENSE) file.
