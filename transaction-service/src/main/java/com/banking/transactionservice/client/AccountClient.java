package com.banking.transactionservice.client;

import com.banking.transactionservice.dto.AccountResponse;
import com.banking.transactionservice.error.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@FeignClient(name = "account-service")
public interface AccountClient {

    @GetMapping("/api/accounts/{accountNumber}")
    ApiResponse<AccountResponse> getAccountByAccountNumber(@PathVariable String accountNumber);

    @PostMapping("/api/accounts/{accountNumber}/balance")
    ApiResponse<AccountResponse> updateBalance(
            @PathVariable String accountNumber,
            @RequestParam BigDecimal amount
    );

}