package com.banking.transactionservice.dto;

import com.banking.transactionservice.enums.AccountStatus;
import com.banking.transactionservice.enums.AccountType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountResponse {
    private Long id;
    private String accountNumber;
    private Long customerId;
    private Long userId;
    private AccountType accountType;
    private AccountStatus accountStatus;
    private BigDecimal balance;
    private LocalDateTime createdAt;
}