package com.cloudclean.repository;

import com.cloudclean.user.UserPreferences;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserPreferencesRepository extends MongoRepository<UserPreferences, String> {

    Optional<UserPreferences> findByUserId(String userId);

    boolean existsByUserId(String userId);

    void deleteByUserId(String userId);
}
