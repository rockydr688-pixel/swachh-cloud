package com.cloudclean.config;

import com.cloudclean.exception.BadRequestException;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimiterService {

    private static final int MAX_REQUESTS_PER_MINUTE = 15;
    private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    public void checkRateLimit(String userId, String actionKey) {
        String key = userId + ":" + actionKey;
        TokenBucket bucket = buckets.computeIfAbsent(key, k -> new TokenBucket(MAX_REQUESTS_PER_MINUTE));
        if (!bucket.tryConsume()) {
            throw new BadRequestException("Rate limit exceeded for " + actionKey + ". Please wait before trying again.");
        }
    }

    private static class TokenBucket {
        private final int capacity;
        private int tokens;
        private long lastRefillTimestamp;

        public TokenBucket(int capacity) {
            this.capacity = capacity;
            this.tokens = capacity;
            this.lastRefillTimestamp = System.currentTimeMillis();
        }

        public synchronized boolean tryConsume() {
            refill();
            if (tokens > 0) {
                tokens--;
                return true;
            }
            return false;
        }

        private void refill() {
            long now = System.currentTimeMillis();
            long elapsedSeconds = (now - lastRefillTimestamp) / 1000;
            if (elapsedSeconds >= 60) {
                tokens = capacity;
                lastRefillTimestamp = now;
            }
        }
    }
}
