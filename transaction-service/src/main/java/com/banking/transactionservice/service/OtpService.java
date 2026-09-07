package com.banking.transactionservice.service;


import com.banking.transactionservice.dto.PendingTransaction;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.Random;

@Service
@Slf4j
@RequiredArgsConstructor
public class OtpService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final Duration OTP_TTL = Duration.ofMinutes(5);
    private static final String KEY_PREFIX = "pending-txn:";

    public String generateOtp() {
        Random random = new Random();
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }

    public void storePendingTransaction(PendingTransaction pendingTransaction) {
        try {
            String json = objectMapper.writeValueAsString(pendingTransaction);
            String key = KEY_PREFIX + pendingTransaction.getTransactionId();
            redisTemplate.opsForValue().set(key, json, OTP_TTL);
            log.info("Stored pending transaction {} in Redis with TTL {} minutes",
                    pendingTransaction.getTransactionId(), OTP_TTL.toMinutes());
        } catch (Exception e) {
            log.error("Failed to store pending transaction in Redis: {}", e.getMessage());
            throw new RuntimeException("Failed to initiate OTP verification");
        }
    }

    public Optional<PendingTransaction> getPendingTransaction(String transactionId) {
        String key = KEY_PREFIX + transactionId;
        String json = redisTemplate.opsForValue().get(key);
        if (json == null) {
            return Optional.empty();
        }
        try {
            PendingTransaction pendingTransaction = objectMapper.readValue(json, PendingTransaction.class);
            return Optional.of(pendingTransaction);
        } catch (Exception e) {
            log.error("Failed to parse pending transaction from Redis: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public void deletePendingTransaction(String transactionId) {
        redisTemplate.delete(KEY_PREFIX + transactionId);
    }
}