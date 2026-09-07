package com.banking.transactionservice.service;


import com.banking.transactionservice.dto.TransactionResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class IdempotencyService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final Duration TTL = Duration.ofHours(24);
    private static final String KEY_PREFIX = "idempotency:";

    public Optional<TransactionResponse> checkIfProcessed(String idempotencyKey) {
        String key = KEY_PREFIX + idempotencyKey;
        String json = redisTemplate.opsForValue().get(key);
        if (json == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(json, TransactionResponse.class));
        } catch (Exception e) {
            log.error("Failed to parse cached idempotent response: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public void storeResult(String idempotencyKey, TransactionResponse response) {
        try {
            String key = KEY_PREFIX + idempotencyKey;
            String json = objectMapper.writeValueAsString(response);
            redisTemplate.opsForValue().set(key, json, TTL);
        } catch (Exception e) {
            log.error("Failed to store idempotent response: {}", e.getMessage());
        }
    }
}
