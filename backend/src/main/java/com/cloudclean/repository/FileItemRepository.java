package com.cloudclean.repository;

import com.cloudclean.file.FileItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface FileItemRepository extends MongoRepository<FileItem, String> {

    Optional<FileItem> findByIdAndUserId(String id, String userId);

    Page<FileItem> findByUserIdAndIsDeletedFalse(String userId, Pageable pageable);

    Page<FileItem> findByUserIdAndCategoryAndIsDeletedFalse(String userId, String category, Pageable pageable);

    List<FileItem> findByUserIdAndCategoryAndIsDeletedFalse(String userId, String category);

    List<FileItem> findByUserIdAndIsDeletedFalse(String userId);

    List<FileItem> findByIdInAndUserId(List<String> ids, String userId);

    long countByUserIdAndIsDeletedFalse(String userId);

    long countByUserIdAndCategoryAndIsDeletedFalse(String userId, String category);

    @Query("{ 'userId': ?0, 'isDeleted': false, $or: [ { 'name': { $regex: ?1, $options: 'i' } }, { 'storageLocation': { $regex: ?1, $options: 'i' } }, { 'detectionReason': { $regex: ?1, $options: 'i' } } ] }")
    Page<FileItem> searchFiles(String userId, String query, Pageable pageable);

    List<FileItem> findByUserIdAndLastAccessedBeforeAndIsDeletedFalse(String userId, Instant cutoffDate);

    List<FileItem> findByUserIdAndSizeBytesGreaterThanEqualAndIsDeletedFalse(String userId, long minSize);
}
