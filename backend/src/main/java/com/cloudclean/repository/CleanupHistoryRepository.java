package com.cloudclean.repository;

import com.cloudclean.cleanup.CleanupHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CleanupHistoryRepository extends MongoRepository<CleanupHistory, String> {
    List<CleanupHistory> findByUserIdOrderByTimestampDesc(String userId);
    Page<CleanupHistory> findByUserIdOrderByTimestampDesc(String userId, Pageable pageable);
    Optional<CleanupHistory> findByIdAndUserId(String id, String userId);
}
