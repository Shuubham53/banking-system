package com.banking.transactionservice.service;

import com.banking.transactionservice.client.AccountClient;
import com.banking.transactionservice.dto.AccountResponse;
import com.banking.transactionservice.dto.TransactionRequest;
import com.banking.transactionservice.dto.TransactionResponse;
import com.banking.transactionservice.entity.Transaction;
import com.banking.transactionservice.enums.AccountStatus;
import com.banking.transactionservice.enums.TransactionStatus;
import com.banking.transactionservice.enums.TransactionType;
import com.banking.transactionservice.error.InsufficientBalanceException;
import com.banking.transactionservice.error.InvalidTransactionException;
import com.banking.transactionservice.event.FraudAlertEvent;
import com.banking.transactionservice.kafka.FraudEventProducer;
import com.banking.transactionservice.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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

    private void checkForFraud(String accountNumber, BigDecimal amount,
                               String transactionType, String transactionId) {
        BigDecimal fraudThreshold = BigDecimal.valueOf(50000);
        if (amount.compareTo(fraudThreshold) > 0) {
            log.warn("FRAUD ALERT: Large transaction detected for account: {}", accountNumber);
            FraudAlertEvent event = FraudAlertEvent.builder()
                    .accountNumber(accountNumber)
                    .amount(amount)
                    .transactionType(transactionType)
                    .transactionId(transactionId)
                    .build();
            fraudEventProducer.publishFraudAlert(event);
        }
    }
    @Transactional
    public TransactionResponse deposit(TransactionRequest request) {
        log.info("depositing money to account number {}",request.getToAccountNumber());
        AccountResponse account = accountClient
            .getAccountByAccountNumber(request.getToAccountNumber()).getData();
        if(!account.getAccountStatus().equals(AccountStatus.ACTIVE)){
            throw new InvalidTransactionException("Account is not active");
        }
        String transactionId = generateTransactionId();
        Transaction transaction = Transaction.builder()
                        .transactionId(transactionId)
                        .transactionStatus(TransactionStatus.SUCCESS)
                        .transactionType(request.getTransactionType())
                        .amount(request.getAmount())
                        .toAccountNumber(request.getToAccountNumber())
                        .fromAccountNumber(request.getFromAccountNumber())
                        .description(request.getDescription())
                        .createdAt(LocalDateTime.now())
                        .build();
        accountClient.updateBalance(account.getAccountNumber(),request.getAmount());
        transactionRepository.save(transaction);
        checkForFraud(request.getToAccountNumber(), request.getAmount(),
                "DEPOSIT", transactionId);
        log.info("money deposited from account number {}",request.getFromAccountNumber());
        return mapToResponse(transaction);

    }

    @Transactional
    public TransactionResponse withdraw(TransactionRequest request) {

        log.info("withdrawing  money from account {}",request.getFromAccountNumber());
        AccountResponse account = accountClient
                .getAccountByAccountNumber(request.getFromAccountNumber()).getData();
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
        Transaction transaction = Transaction.builder()
                .transactionId(transactionId)
                .transactionStatus(TransactionStatus.SUCCESS)
                .transactionType(request.getTransactionType())
                .amount(request.getAmount())
                .toAccountNumber(null)
                .fromAccountNumber(request.getFromAccountNumber())
                .description(request.getDescription())
                .createdAt(LocalDateTime.now())
                .build();
        accountClient.updateBalance(account.getAccountNumber(),request.getAmount().negate());
        transactionRepository.save(transaction);
        checkForFraud(request.getFromAccountNumber(), request.getAmount(),
                "WITHDRAWAL", transactionId);
        log.info("money is withdraw  from account number {}",request.getFromAccountNumber());
        return mapToResponse(transaction);
    }

    @Transactional
    public TransactionResponse transfer(TransactionRequest request) {
        AccountResponse senderAccount = accountClient
                .getAccountByAccountNumber(request.getFromAccountNumber()).getData();
        AccountResponse receiverAccount = accountClient
                .getAccountByAccountNumber(request.getToAccountNumber()).getData();
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
        Transaction transaction = Transaction.builder()
                .transactionId(transactionId)
                .transactionStatus(TransactionStatus.SUCCESS)
                .transactionType(request.getTransactionType())
                .amount(request.getAmount())
                .toAccountNumber(request.getToAccountNumber())
                .fromAccountNumber(request.getFromAccountNumber())
                .description(request.getDescription())
                .createdAt(LocalDateTime.now())
                .build();
        accountClient.updateBalance(request.getFromAccountNumber(),request.getAmount().negate());
        accountClient.updateBalance(request.getToAccountNumber(),request.getAmount());
        transactionRepository.save(transaction);
        checkForFraud(request.getFromAccountNumber(), request.getAmount(),
                "TRANSFER", transactionId);
        log.info("money transferred from account {} to account {}",
                request.getFromAccountNumber(),
                request.getToAccountNumber());
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
                .build();

    }

    private String generateTransactionId() {
        return UUID.randomUUID().toString();
    }
}