package com.cloudclean.repository;

import com.cloudclean.storage.StorageConnection;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StorageConnectionRepository extends MongoRepository<StorageConnection, String> {
    List<StorageConnection> findByUserId(String userId);
    Optional<StorageConnection> findByIdAndUserId(String id, String userId);
    Optional<StorageConnection> findByUserIdAndType(String userId, String type);
}
