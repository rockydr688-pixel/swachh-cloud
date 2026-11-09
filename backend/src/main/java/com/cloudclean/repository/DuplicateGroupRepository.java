package com.cloudclean.repository;

import com.cloudclean.detection.DuplicateGroup;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DuplicateGroupRepository extends MongoRepository<DuplicateGroup, String> {
    List<DuplicateGroup> findByUserId(String userId);
    Optional<DuplicateGroup> findByUserIdAndHash(String userId, String hash);
    void deleteByUserId(String userId);
}
