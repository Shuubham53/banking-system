package com.banking.transactionservice;


import com.banking.transactionservice.dto.FraudCheckResult;
import com.banking.transactionservice.kafka.FraudEventProducer;
import com.banking.transactionservice.repository.TransactionRepository;
import com.banking.transactionservice.service.EmailService;
import com.banking.transactionservice.service.OtpService;
import com.banking.transactionservice.service.TransactionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


import com.banking.transactionservice.client.AccountClient;
import com.banking.transactionservice.client.UserClient;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountClient accountClient;

    @Mock
    private FraudEventProducer fraudEventProducer;

    @Mock
    private UserClient userClient;

    @Mock
    private EmailService emailService;

    @Mock
    private OtpService otpService;

    @InjectMocks
    private TransactionService transactionService;


    @Test
    void checkForFraud_noSignals_returnsZeroScore() {
        when(transactionRepository.countByFromAccountNumberAndCreatedAtAfter(
                eq("ACC123"), any(LocalDateTime.class))).thenReturn(0L);
        when(transactionRepository.countByToAccountNumberAndCreatedAtAfter(
                eq("ACC123"), any(LocalDateTime.class))).thenReturn(0L);

        FraudCheckResult result = transactionService.checkForFraud(
                "ACC123", BigDecimal.valueOf(1000), "DEPOSIT", "txn-1");

        assertEquals(0, result.getRiskScore());
        assertNull(result.getFlagReason());
    }

    @Test
    void checkForFraud_largeAmountAlone_returns55AndFlags() {
        when(transactionRepository.countByFromAccountNumberAndCreatedAtAfter(
                eq("ACC123"), any(LocalDateTime.class))).thenReturn(0L);
        when(transactionRepository.countByToAccountNumberAndCreatedAtAfter(
                eq("ACC123"), any(LocalDateTime.class))).thenReturn(0L);

        FraudCheckResult result = transactionService.checkForFraud(
                "ACC123", BigDecimal.valueOf(60000), "DEPOSIT", "txn-2");

        assertEquals(55, result.getRiskScore());
        assertEquals("Large transaction amount", result.getFlagReason());
    }
    @Test
    void checkForFraud_velocityTier_5to9_returns50() {
        when(transactionRepository.countByFromAccountNumberAndCreatedAtAfter(
                eq("ACC123"), any(LocalDateTime.class))).thenReturn(5L);
        when(transactionRepository.countByToAccountNumberAndCreatedAtAfter(
                eq("ACC123"), any(LocalDateTime.class))).thenReturn(0L);

        FraudCheckResult result = transactionService.checkForFraud(
                "ACC123", BigDecimal.valueOf(1000), "DEPOSIT", "txn-3");

        assertEquals(50, result.getRiskScore());
    }

    @Test
    void checkForFraud_velocityTier_10Plus_returns70() {
        when(transactionRepository.countByFromAccountNumberAndCreatedAtAfter(
                eq("ACC123"), any(LocalDateTime.class))).thenReturn(10L);
        when(transactionRepository.countByToAccountNumberAndCreatedAtAfter(
                eq("ACC123"), any(LocalDateTime.class))).thenReturn(0L);

        FraudCheckResult result = transactionService.checkForFraud(
                "ACC123", BigDecimal.valueOf(1000), "DEPOSIT", "txn-4");

        assertEquals(70, result.getRiskScore());
    }

    @Test
    void checkForFraud_largeAmountPlusVelocity_scoresAdditively() {
        when(transactionRepository.countByFromAccountNumberAndCreatedAtAfter(
                eq("ACC123"), any(LocalDateTime.class))).thenReturn(4L);
        when(transactionRepository.countByToAccountNumberAndCreatedAtAfter(
                eq("ACC123"), any(LocalDateTime.class))).thenReturn(0L);

        FraudCheckResult result = transactionService.checkForFraud(
                "ACC123", BigDecimal.valueOf(60000), "DEPOSIT", "txn-5");

        assertEquals(85, result.getRiskScore()); // 55 (large amount) + 30 (velocity 3-4 tier)
        assertEquals("Large transaction amount; Elevated transaction velocity (4 in last 2 minutes)",
                result.getFlagReason());
    }

    @Test
    void checkForFraud_oddHourLargeAmount_returns30() {
        when(transactionRepository.countByFromAccountNumberAndCreatedAtAfter(
                eq("ACC123"), any(LocalDateTime.class))).thenReturn(0L);
        when(transactionRepository.countByToAccountNumberAndCreatedAtAfter(
                eq("ACC123"), any(LocalDateTime.class))).thenReturn(0L);

        Instant fixedInstant = LocalDateTime.of(2026, 1, 1, 2, 0)
                .atZone(ZoneId.systemDefault()).toInstant();
        Clock fixedClock = Clock.fixed(fixedInstant, ZoneId.systemDefault());

        TransactionService serviceWithFixedClock = new TransactionService(
                transactionRepository, accountClient, fraudEventProducer,
                userClient, emailService, otpService, fixedClock);

        FraudCheckResult result = serviceWithFixedClock.checkForFraud(
                "ACC123", BigDecimal.valueOf(25000), "DEPOSIT", "txn-6");

        assertEquals(30, result.getRiskScore());
        assertEquals("Large transaction during odd hours", result.getFlagReason());
    }
}