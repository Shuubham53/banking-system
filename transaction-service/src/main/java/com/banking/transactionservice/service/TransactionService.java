package com.banking.transactionservice.service;

import com.banking.transactionservice.client.AccountClient;
import com.banking.transactionservice.client.UserClient;
import com.banking.transactionservice.dto.*;
import com.banking.transactionservice.entity.Transaction;
import com.banking.transactionservice.enums.AccountStatus;
import com.banking.transactionservice.enums.TransactionStatus;
import com.banking.transactionservice.enums.TransactionType;
import com.banking.transactionservice.error.AccountNotFoundException;
import com.banking.transactionservice.error.InsufficientBalanceException;
import com.banking.transactionservice.error.InvalidTransactionException;
import com.banking.transactionservice.event.FraudAlertEvent;
import com.banking.transactionservice.kafka.FraudEventProducer;
import com.banking.transactionservice.repository.TransactionRepository;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountClient accountClient;
    private final FraudEventProducer fraudEventProducer;
    private final UserClient userClient;
    private final EmailService emailService;
    private final OtpService otpService;
    private final Clock clock;

     public FraudCheckResult checkForFraud(String accountNumber, BigDecimal amount,
                                           String transactionType, String transactionId) {
        int riskScore = 0;
        List<String> reasons = new ArrayList<>();

        /// Rule 1: large amount
        BigDecimal largeAmountThreshold = BigDecimal.valueOf(50000);
        if (amount.compareTo(largeAmountThreshold) > 0) {
            riskScore += 55;
            reasons.add("Large transaction amount");
        }

        /// Rule 2: velocity - too many transactions involving this account recently
        LocalDateTime windowStart = LocalDateTime.now().minusMinutes(2);
        long fromCount = transactionRepository
                .countByFromAccountNumberAndCreatedAtAfter(accountNumber, windowStart);
        long toCount = transactionRepository
                .countByToAccountNumberAndCreatedAtAfter(accountNumber, windowStart);
        long recentCount = fromCount + toCount;
        if (recentCount >= 10) {
            riskScore += 70;
            reasons.add("Severe transaction velocity (" + recentCount + " in last 2 minutes)");
        } else if (recentCount >= 5) {
            riskScore += 50;
            reasons.add("High transaction velocity (" + recentCount + " in last 2 minutes)");
        } else if (recentCount >= 3) {
            riskScore += 30;
            reasons.add("Elevated transaction velocity (" + recentCount + " in last 2 minutes)");
        }

        /// Rule 3: odd-hour large transaction
         int currentHour = LocalDateTime.now(clock).getHour();
        BigDecimal oddHourThreshold = BigDecimal.valueOf(20000);
        boolean isOddHour = currentHour >= 0 && currentHour < 5;
        if (isOddHour && amount.compareTo(oddHourThreshold) > 0) {
            riskScore += 30;
            reasons.add("Large transaction during odd hours");
        }

        String flagReason = reasons.isEmpty() ? null : String.join("; ", reasons);

        if (riskScore >= 50) {
            log.warn("FRAUD ALERT: account {} flagged with score {} - {}",
                    accountNumber, riskScore, flagReason);
            FraudAlertEvent event = FraudAlertEvent.builder()
                    .accountNumber(accountNumber)
                    .amount(amount)
                    .transactionType(transactionType)
                    .transactionId(transactionId)
                    .build();
            fraudEventProducer.publishFraudAlert(event);
        }

        return new FraudCheckResult(riskScore, flagReason);
    }

    private AccountResponse getAccountOrThrow(String accountNumber) {
        try {
            return accountClient.getAccountByAccountNumber(accountNumber).getData();
        } catch (FeignException.NotFound e) {
            throw new AccountNotFoundException("Account not found");
        }
    }
    @Transactional
    public TransactionResponse deposit(TransactionRequest request) {
        log.info("depositing money to account number {}",request.getToAccountNumber());

        AccountResponse account = getAccountOrThrow(request.getToAccountNumber());

        if(!account.getAccountStatus().equals(AccountStatus.ACTIVE)){
            throw new InvalidTransactionException("Account is not active");
        }
        String transactionId = generateTransactionId();
        Transaction transaction = Transaction.builder()
                .transactionId(transactionId)
                .transactionStatus(TransactionStatus.PENDING)
                .transactionType(request.getTransactionType())
                .amount(request.getAmount())
                .toAccountNumber(request.getToAccountNumber())
                .fromAccountNumber(request.getFromAccountNumber())
                .description(request.getDescription())
                .createdAt(LocalDateTime.now())
                .build();
        AccountResponse updatedAccount;
        try{
            updatedAccount = accountClient.updateBalance(account.getAccountNumber(),request.getAmount()).getData();
        } catch (Exception e) {
            log.error("Deposit failed for transaction {}, account {}", transactionId, account.getAccountNumber());
            transaction.setTransactionStatus(TransactionStatus.FAILED);
            transaction.setUpdatedAt(LocalDateTime.now());
            transactionRepository.save(transaction);
            throw new InvalidTransactionException("Deposit failed: " + e.getMessage());
        }

        transaction.setTransactionStatus(TransactionStatus.SUCCESS);
        transaction.setUpdatedAt(LocalDateTime.now());
        transaction.setAvailableBalance(updatedAccount.getBalance());

        FraudCheckResult fraudResult = checkForFraud(request.getToAccountNumber(), request.getAmount(),
                "DEPOSIT", transactionId);
        transaction.setRiskScore(fraudResult.getRiskScore());
        transaction.setFlagReason(fraudResult.getFlagReason());

        if (fraudResult.getRiskScore() >= 50) {
            try {
                UserResponse user = userClient.getUserById(account.getUserId()).getData();
                emailService.sendFraudAlertEmail(user.getEmail(), "DEPOSIT", request.getAmount(),
                        fraudResult.getRiskScore(), fraudResult.getFlagReason());
            } catch (Exception e) {
                log.error("Failed to send fraud alert email: {}", e.getMessage());
            }
        }

        transactionRepository.save(transaction);
        log.info("money deposited from account number {}",request.getFromAccountNumber());
        try {
            UserResponse user = userClient.getUserById(account.getUserId()).getData();
            emailService.sendTransactionNotification(
                    user.getEmail(),
                    "DEPOSIT",
                    request.getAmount(),
                    updatedAccount.getBalance(),
                    request.getToAccountNumber(),
                    null,
                    transaction.getCreatedAt()
            );
        } catch (Exception e) {
            log.error("Failed to send transaction email for transaction {}: {}", transactionId, e.getMessage());
        }
        return mapToResponse(transaction);
    }

    @Transactional
    public TransactionResponse withdraw(TransactionRequest request) {

        log.info("withdrawing  money from account {}",request.getFromAccountNumber());

        AccountResponse account = getAccountOrThrow(request.getFromAccountNumber());

        if(!account.getAccountStatus().equals(AccountStatus.ACTIVE)){
            throw new InvalidTransactionException("Account is not active");
        }

        BigDecimal minimumBalance = BigDecimal.valueOf(500);
        BigDecimal balanceAfterWithdrawal = account.getBalance().subtract(request.getAmount());

        if(balanceAfterWithdrawal.compareTo(minimumBalance) < 0){
            throw new InsufficientBalanceException(
                    "Cannot withdraw. Minimum balance of ₹500 must be maintained.");
        }

        String transactionId = generateTransactionId();

        // Run fraud check BEFORE touching any balance
        FraudCheckResult fraudResult = checkForFraud(request.getFromAccountNumber(), request.getAmount(),
                "WITHDRAWAL", transactionId);

        if (fraudResult.getRiskScore() >= 50) {
            return initiateOtpFlow(request, transactionId, fraudResult, account.getUserId());
        }

        // Not risky — proceed exactly as before
        Transaction transaction = Transaction.builder()
                .transactionId(transactionId)
                .transactionStatus(TransactionStatus.PENDING)
                .transactionType(request.getTransactionType())
                .amount(request.getAmount())
                .toAccountNumber(null)
                .fromAccountNumber(request.getFromAccountNumber())
                .description(request.getDescription())
                .createdAt(LocalDateTime.now())
                .build();

        AccountResponse updatedAccount;
        try {
            updatedAccount = accountClient.updateBalance(account.getAccountNumber(), request.getAmount().negate()).getData();
        } catch (Exception e) {
            log.error("Withdrawal failed for transaction {}, account {}", transactionId, account.getAccountNumber());
            transaction.setTransactionStatus(TransactionStatus.FAILED);
            transaction.setUpdatedAt(LocalDateTime.now());
            transactionRepository.save(transaction);
            throw new InvalidTransactionException("Withdrawal failed: " + e.getMessage());
        }

        transaction.setTransactionStatus(TransactionStatus.SUCCESS);
        transaction.setUpdatedAt(LocalDateTime.now());
        transaction.setAvailableBalance(updatedAccount.getBalance());
        transaction.setRiskScore(fraudResult.getRiskScore());
        transaction.setFlagReason(fraudResult.getFlagReason());

        transactionRepository.save(transaction);
        log.info("money is withdraw  from account number {}",request.getFromAccountNumber());

        try {
            UserResponse user = userClient.getUserById(account.getUserId()).getData();
            emailService.sendTransactionNotification(
                    user.getEmail(),
                    "WITHDRAWAL",
                    request.getAmount(),
                    updatedAccount.getBalance(),
                    request.getFromAccountNumber(),
                    null,
                    transaction.getCreatedAt()
            );
        } catch (Exception e) {
            log.error("Failed to send transaction email for transaction {}: {}", transactionId, e.getMessage());
        }

        return mapToResponse(transaction);
    }


    @Transactional
    public TransactionResponse transfer(TransactionRequest request) {
        AccountResponse senderAccount = getAccountOrThrow(request.getFromAccountNumber());
        AccountResponse receiverAccount = getAccountOrThrow(request.getToAccountNumber());

        if(!senderAccount.getAccountStatus().equals(AccountStatus.ACTIVE)){
            throw new InvalidTransactionException("Sender Account is not active");
        }
        if(!receiverAccount.getAccountStatus().equals(AccountStatus.ACTIVE)){
            throw new InvalidTransactionException("Receiver Account is not active");
        }
        if(senderAccount.getBalance().compareTo(request.getAmount()) < 0){
            log.info("cannot transfer money , insufficient balance");
            throw new InsufficientBalanceException("cannot transfer , insufficient balance");
        }

        String transactionId = generateTransactionId();

        // Run fraud check BEFORE touching any balance
        FraudCheckResult fraudResult = checkForFraud(request.getFromAccountNumber(), request.getAmount(),
                "TRANSFER", transactionId);

        if (fraudResult.getRiskScore() >= 50) {
            return initiateOtpFlow(request, transactionId, fraudResult, senderAccount.getUserId());
        }

        // Not risky — proceed exactly as before
        Transaction transaction = Transaction.builder()
                .transactionId(transactionId)
                .transactionStatus(TransactionStatus.PENDING)
                .transactionType(request.getTransactionType())
                .amount(request.getAmount())
                .toAccountNumber(request.getToAccountNumber())
                .fromAccountNumber(request.getFromAccountNumber())
                .description(request.getDescription())
                .createdAt(LocalDateTime.now())
                .build();

        AccountResponse debitedAccount =
                accountClient.updateBalance(request.getFromAccountNumber(), request.getAmount().negate()).getData();

        try {
            accountClient.updateBalance(request.getToAccountNumber(), request.getAmount());
        } catch (Exception e) {
            log.error("Credit failed for transaction {}, reversing debit on account {}",
                    transactionId, request.getFromAccountNumber());
            try {
                accountClient.updateBalance(request.getFromAccountNumber(), request.getAmount());
            } catch (Exception reversalError) {
                log.error("CRITICAL: reversal FAILED for transaction {}. Manual reconciliation needed. Account: {}",
                        transactionId, request.getFromAccountNumber(), reversalError);
            }
            transaction.setTransactionStatus(TransactionStatus.FAILED);
            transaction.setUpdatedAt(LocalDateTime.now());
            transactionRepository.save(transaction);
            throw new InvalidTransactionException("Transfer failed, amount reversed: " + e.getMessage());
        }

        transaction.setTransactionStatus(TransactionStatus.SUCCESS);
        transaction.setUpdatedAt(LocalDateTime.now());
        transaction.setAvailableBalance(debitedAccount.getBalance());
        transaction.setRiskScore(fraudResult.getRiskScore());
        transaction.setFlagReason(fraudResult.getFlagReason());

        transactionRepository.save(transaction);
        log.info("money transferred from account {} to account {}",
                request.getFromAccountNumber(), request.getToAccountNumber());

        try {
            UserResponse user = userClient.getUserById(senderAccount.getUserId()).getData();
            emailService.sendTransactionNotification(
                    user.getEmail(),
                    "TRANSFER",
                    request.getAmount(),
                    debitedAccount.getBalance(),
                    request.getFromAccountNumber(),
                    request.getToAccountNumber(),
                    transaction.getCreatedAt()
            );
        } catch (Exception e) {
            log.error("Failed to send transaction email for transaction {}: {}", transactionId, e.getMessage());
        }

        return mapToResponse(transaction);
    }

    private TransactionResponse initiateOtpFlow(TransactionRequest request, String transactionId,
                                                FraudCheckResult fraudResult, Long userId) {
        Transaction transaction = Transaction.builder()
                .transactionId(transactionId)
                .transactionStatus(TransactionStatus.PENDING)
                .transactionType(request.getTransactionType())
                .amount(request.getAmount())
                .toAccountNumber(request.getToAccountNumber())
                .fromAccountNumber(request.getFromAccountNumber())
                .description(request.getDescription())
                .createdAt(LocalDateTime.now())
                .riskScore(fraudResult.getRiskScore())
                .flagReason(fraudResult.getFlagReason())
                .build();

        transactionRepository.save(transaction);

        String otp = otpService.generateOtp();
        PendingTransaction pendingTransaction = new PendingTransaction(
                otp,
                transactionId,
                request.getTransactionType(),
                request.getFromAccountNumber(),
                request.getToAccountNumber(),
                request.getAmount(),
                request.getDescription()
        );
        otpService.storePendingTransaction(pendingTransaction);

        try {
            UserResponse user = userClient.getUserById(userId).getData();
            emailService.sendOtpEmail(user.getEmail(), otp, request.getAmount());
        } catch (Exception e) {
            log.error("Failed to send OTP email for transaction {}: {}", transactionId, e.getMessage());
        }

        log.warn("Transaction {} flagged as risky (score {}), OTP sent, awaiting confirmation",
                transactionId, fraudResult.getRiskScore());

        return mapToResponse(transaction);
    }

    public List<TransactionResponse> getTransactionHistory(String accountNumber) {
        List<Transaction> send = transactionRepository.findAllByFromAccountNumber(accountNumber);
        List<Transaction> received = transactionRepository.findAllByToAccountNumber(accountNumber);

        List<Transaction> history = new ArrayList<>(send);
        history.addAll(received);
        return history.stream()
                .map(this::mapToResponse)
                .toList();
    }

    private TransactionResponse mapToResponse(Transaction transaction) {
        return TransactionResponse.builder()
                .id(transaction.getId())
                .transactionId(transaction.getTransactionId())
                .amount(transaction.getAmount())
                .description(transaction.getDescription())
                .fromAccountNumber(transaction.getFromAccountNumber())
                .toAccountNumber(transaction.getToAccountNumber())
                .transactionStatus(transaction.getTransactionStatus())
                .transactionType(transaction.getTransactionType())
                .createdAt(transaction.getCreatedAt())
                .availableBalance(transaction.getAvailableBalance())
                .updatedAt(transaction.getUpdatedAt())
                .riskScore(transaction.getRiskScore())
                .flagReason(transaction.getFlagReason())
                .build();
    }


    @Transactional
    public TransactionResponse confirmOtp(String transactionId, String submittedOtp) {
        PendingTransaction pending = otpService.getPendingTransaction(transactionId)
                .orElseThrow(() -> new InvalidTransactionException("OTP expired or transaction not found"));

        if (!pending.getOtp().equals(submittedOtp)) {
            throw new InvalidTransactionException("Invalid OTP");
        }

        Transaction transaction = transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new InvalidTransactionException("Transaction not found: " + transactionId));

        if (pending.getTransactionType() == TransactionType.WITHDRAWAL) {
            return confirmWithdrawal(pending, transaction);
        } else {
            return confirmTransfer(pending, transaction);
        }
    }

    private TransactionResponse confirmWithdrawal(PendingTransaction pending, Transaction transaction) {
        AccountResponse account = getAccountOrThrow(pending.getFromAccountNumber());

        AccountResponse updatedAccount;
        try {
            updatedAccount = accountClient.updateBalance(pending.getFromAccountNumber(), pending.getAmount().negate()).getData();
        } catch (Exception e) {
            log.error("Withdrawal failed during OTP confirmation for transaction {}", pending.getTransactionId());
            transaction.setTransactionStatus(TransactionStatus.FAILED);
            transaction.setUpdatedAt(LocalDateTime.now());
            transactionRepository.save(transaction);
            otpService.deletePendingTransaction(pending.getTransactionId());
            throw new InvalidTransactionException("Withdrawal failed after OTP confirmation: " + e.getMessage());
        }

        transaction.setTransactionStatus(TransactionStatus.SUCCESS);
        transaction.setUpdatedAt(LocalDateTime.now());
        transaction.setAvailableBalance(updatedAccount.getBalance());
        transactionRepository.save(transaction);

        otpService.deletePendingTransaction(pending.getTransactionId());

        try {
            UserResponse user = userClient.getUserById(account.getUserId()).getData();
            emailService.sendTransactionNotification(
                    user.getEmail(),
                    "WITHDRAWAL",
                    pending.getAmount(),
                    updatedAccount.getBalance(),
                    pending.getFromAccountNumber(),
                    null,
                    transaction.getCreatedAt()
            );
        } catch (Exception e) {
            log.error("Failed to send confirmation email: {}", e.getMessage());
        }

        log.info("Withdrawal {} confirmed via OTP and completed", pending.getTransactionId());
        return mapToResponse(transaction);
    }

    private TransactionResponse confirmTransfer(PendingTransaction pending, Transaction transaction) {
        AccountResponse senderAccount = getAccountOrThrow(pending.getFromAccountNumber());

        AccountResponse debitedAccount =
                accountClient.updateBalance(pending.getFromAccountNumber(), pending.getAmount().negate()).getData();

        try {
            accountClient.updateBalance(pending.getToAccountNumber(), pending.getAmount());
        } catch (Exception e) {
            log.error("Credit failed during OTP confirmation for transaction {}, reversing debit", pending.getTransactionId());
            try {
                accountClient.updateBalance(pending.getFromAccountNumber(), pending.getAmount());
            } catch (Exception reversalError) {
                log.error("CRITICAL: reversal FAILED for transaction {}. Manual reconciliation needed.",
                        pending.getTransactionId(), reversalError);
            }
            transaction.setTransactionStatus(TransactionStatus.FAILED);
            transaction.setUpdatedAt(LocalDateTime.now());
            transactionRepository.save(transaction);
            otpService.deletePendingTransaction(pending.getTransactionId());
            throw new InvalidTransactionException("Transaction failed after OTP confirmation, amount reversed: " + e.getMessage());
        }

        transaction.setTransactionStatus(TransactionStatus.SUCCESS);
        transaction.setUpdatedAt(LocalDateTime.now());
        transaction.setAvailableBalance(debitedAccount.getBalance());
        transactionRepository.save(transaction);

        otpService.deletePendingTransaction(pending.getTransactionId());

        try {
            UserResponse user = userClient.getUserById(senderAccount.getUserId()).getData();
            emailService.sendTransactionNotification(
                    user.getEmail(),
                    "TRANSFER",
                    pending.getAmount(),
                    debitedAccount.getBalance(),
                    pending.getFromAccountNumber(),
                    pending.getToAccountNumber(),
                    transaction.getCreatedAt()
            );
        } catch (Exception e) {
            log.error("Failed to send confirmation email: {}", e.getMessage());
        }

        log.info("Transfer {} confirmed via OTP and completed", pending.getTransactionId());
        return mapToResponse(transaction);
    }

    private String generateTransactionId() {
        return UUID.randomUUID().toString();
    }
}