package com.banking.account_service.dto;

import com.banking.account_service.enums.AccountStatus;
import com.banking.account_service.enums.AccountType;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
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
