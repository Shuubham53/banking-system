package com.banking.transactionservice.client;

import com.banking.transactionservice.dto.AccountResponse;
import com.banking.transactionservice.error.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

@FeignClient(name = "account-service")
public interface AccountClient {

    @GetMapping("/api/accounts/{accountNumber}")
    ApiResponse<AccountResponse> getAccountByAccountNumber(@PathVariable String accountNumber);

    @PatchMapping("/api/accounts/{accountNumber}/balance")
    ApiResponse<AccountResponse> updateBalance(
            @PathVariable String accountNumber,
            @RequestParam BigDecimal amount
    );

}