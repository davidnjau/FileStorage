package com.dave.filestorage.db;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Date;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DataMongoTest
@Testcontainers
class GaragePollStateRepositoryTest {

    @Container
    static MongoDBContainer mongo = new MongoDBContainer("mongo:6");

    @DynamicPropertySource
    static void mongoProps(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongo::getReplicaSetUrl);
    }

    @Autowired
    GaragePollStateRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void saveAndFindByBucket() {
        Set<String> keys = Set.of("2024/images/photo.jpg", "2024/docs/report.pdf");
        repository.save(new GaragePollState("my-bucket", keys, new Date()));

        Optional<GaragePollState> found = repository.findByBucket("my-bucket");
        assertTrue(found.isPresent());
        assertEquals(2, found.get().getObjectKeys().size());
    }

    @Test
    void findByBucket_missingBucket_returnsEmpty() {
        Optional<GaragePollState> found = repository.findByBucket("nonexistent");
        assertFalse(found.isPresent());
    }

    @Test
    void updateState_replacesObjectKeys() {
        GaragePollState state = repository.save(
                new GaragePollState("bucket-x", Set.of("file1.txt"), new Date()));

        state.setObjectKeys(Set.of("file1.txt", "file2.txt"));
        repository.save(state);

        Optional<GaragePollState> found = repository.findByBucket("bucket-x");
        assertTrue(found.isPresent());
        assertEquals(2, found.get().getObjectKeys().size());
    }
}
