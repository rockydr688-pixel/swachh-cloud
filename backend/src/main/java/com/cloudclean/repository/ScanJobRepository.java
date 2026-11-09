package com.cloudclean.repository;

import com.cloudclean.scanner.ScanJob;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ScanJobRepository extends MongoRepository<ScanJob, String> {
    List<ScanJob> findByUserIdOrderByCreatedAtDesc(String userId);
    Page<ScanJob> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);
    Optional<ScanJob> findByIdAndUserId(String id, String userId);
    List<ScanJob> findByUserIdAndStatus(String userId, String status);
}
