package com.banking.account_service.controller;

import com.banking.account_service.dto.AccountRequest;
import com.banking.account_service.dto.AccountResponse;
import com.banking.account_service.enums.AccountStatus;
import com.banking.account_service.error.ApiResponse;
import com.banking.account_service.service.AccountService;
import jakarta.validation.Valid;
import jakarta.ws.rs.Path;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/accounts")
@Slf4j
public class AccountController {
    private final AccountService accountService;
    @PostMapping
    public ResponseEntity<ApiResponse<AccountResponse>> createAccount(@Valid @RequestBody AccountRequest request){
        log.info("Creating account for customerId {}",request.getCustomerId());
        AccountResponse response = accountService.createAccount(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Account Created",response));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<AccountResponse>>> getAccountsByUserId(@PathVariable Long userId){
        log.info("Fetching accounts for userId: {}", userId);
        List<AccountResponse> response = accountService.getAccountsByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success("Accounts of User by userId",response));
    }

    @GetMapping("/{accountNumber}")
    public ResponseEntity<ApiResponse<AccountResponse>> getAccountByAccountNumber(@PathVariable String accountNumber){
        log.info("Fetching account: {}", accountNumber);
        AccountResponse response = accountService.getAccountByAccountNumber(accountNumber);
        return ResponseEntity.ok(ApiResponse.success("Account of user",response));
    }
    @PatchMapping("/{accountNumber}/status")
    public ResponseEntity<ApiResponse<AccountResponse>> updateAccountStatus(@PathVariable String accountNumber,
        @RequestParam AccountStatus status){
        log.info("Updating status for account: {}", accountNumber);
        AccountResponse response = accountService.updateAccountStatus(accountNumber,status);
        return ResponseEntity.ok(ApiResponse.success("Account status updated successfully",response));
    }
    @PatchMapping("/{accountNumber}/balance")
    public ResponseEntity<ApiResponse<AccountResponse>> updateBalance(
            @PathVariable String accountNumber,
            @RequestParam BigDecimal amount) {
        log.info("Updating balance for account: {}", accountNumber);
        AccountResponse response = accountService.updateBalance(accountNumber, amount);
        return ResponseEntity.ok(ApiResponse.success("Balance updated", response));
    }

}
