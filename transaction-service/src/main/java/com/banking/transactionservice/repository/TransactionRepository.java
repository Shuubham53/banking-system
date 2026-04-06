package com.banking.transactionservice.repository;

import com.banking.transactionservice.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction,Long> {
    Optional<Transaction> findByTransactionId(String transactionId);
    List<Transaction> findAllByFromAccountNumber(String fromAccountNumber);
    List<Transaction> findAllByToAccountNumber(String toAccountNumber);

}
