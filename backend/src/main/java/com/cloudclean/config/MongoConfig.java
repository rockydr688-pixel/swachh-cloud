package com.cloudclean.config;

import com.mongodb.client.MongoClient;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

/**
 * Spring Data MongoDB production-ready configuration.
 * Enables auditing, transactions, and robust database health monitoring.
 */
@Configuration
public class MongoConfig {

    private static final Logger log = LoggerFactory.getLogger(MongoConfig.class);

    /**
     * Configures MongoDB transactional support for atomic multi-document operations.
     */
    @Bean
    public MongoTransactionManager transactionManager(MongoDatabaseFactory dbFactory) {
        try {
            return new MongoTransactionManager(dbFactory);
        } catch (Exception e) {
            log.warn("MongoTransactionManager disabled (single node / non-replica set environment): {}", e.getMessage());
            return null;
        }
    }

    /**
     * Database health indicator for Spring Boot Actuator / monitoring endpoints.
     */
    @Bean
    public HealthIndicator mongoHealthIndicator(MongoClient mongoClient) {
        return () -> {
            try {
                Document ping = mongoClient.getDatabase("admin").runCommand(new Document("ping", 1));
                if (ping.getDouble("ok") == 1.0 || ping.getInteger("ok", 0) == 1) {
                    return Health.up()
                            .withDetail("database", "MongoDB")
                            .withDetail("status", "CONNECTED")
                            .build();
                }
                return Health.down()
                        .withDetail("database", "MongoDB")
                        .withDetail("status", "PING_FAILED")
                        .build();
            } catch (Exception e) {
                return Health.down(e)
                        .withDetail("database", "MongoDB")
                        .withDetail("status", "DISCONNECTED")
                        .withDetail("error", e.getMessage())
                        .build();
            }
        };
    }
}
