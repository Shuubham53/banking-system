package com.banking.account_service.dto;

import com.banking.account_service.enums.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AccountRequest {
    @NotNull(message = "customer id is required")
    private Long customerId;

    @NotNull(message = "user id is required")
    private Long userId;

    @NotNull(message = "account type is required")
    private AccountType accountType;
}
