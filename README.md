# Smsrevamp

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




## Copyright

Released under the Apache License 2.0. See the [LICENSE](https://github.com/codecentric/springboot-sample-app/blob/master/LICENSE) file.
