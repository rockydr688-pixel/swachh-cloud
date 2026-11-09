package com.cloudclean.repository;

import com.cloudclean.recommendation.Recommendation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RecommendationRepository extends MongoRepository<Recommendation, String> {
    List<Recommendation> findByUserIdOrderByCreatedAtDesc(String userId);
    List<Recommendation> findByUserIdAndStatusOrderByCreatedAtDesc(String userId, String status);
    Optional<Recommendation> findByIdAndUserId(String id, String userId);
}
