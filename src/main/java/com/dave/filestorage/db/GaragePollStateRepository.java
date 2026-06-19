package com.dave.filestorage.db;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface GaragePollStateRepository extends MongoRepository<GaragePollState, String> {
    Optional<GaragePollState> findByBucket(String bucket);
}
