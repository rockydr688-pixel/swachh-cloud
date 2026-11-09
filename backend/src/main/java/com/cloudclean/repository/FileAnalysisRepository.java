package com.cloudclean.repository;

import com.cloudclean.analysis.FileAnalysis;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FileAnalysisRepository extends MongoRepository<FileAnalysis, String> {
    List<FileAnalysis> findByUserIdAndFileId(String userId, String fileId);
    List<FileAnalysis> findByUserIdAndAnalysisType(String userId, String analysisType);
    Optional<FileAnalysis> findByUserIdAndFileIdAndAnalysisType(String userId, String fileId, String analysisType);
    void deleteByFileId(String fileId);
}
