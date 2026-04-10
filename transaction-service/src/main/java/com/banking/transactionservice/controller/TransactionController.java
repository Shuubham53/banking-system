package com.banking.transactionservice.controller;

import com.banking.transactionservice.dto.TransactionRequest;
import com.banking.transactionservice.dto.TransactionResponse;
import com.banking.transactionservice.error.ApiResponse;
import com.banking.transactionservice.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/transactions")
@Slf4j
public class TransactionController {
    private final TransactionService transactionService;

    @PostMapping("/deposit")
    public ResponseEntity<ApiResponse<TransactionResponse>> deposit(
            @Valid @RequestBody TransactionRequest request){
        log.info("Deposit request for account: {}", request.getToAccountNumber());
        TransactionResponse response = transactionService.deposit(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Money deposited", response));
    }

    @PostMapping("/withdraw")
    public ResponseEntity<ApiResponse<TransactionResponse>> withdraw(
            @Valid @RequestBody TransactionRequest request){
        log.info("Withdraw request for account: {}", request.getFromAccountNumber());
        TransactionResponse response = transactionService.withdraw(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Money withdrawn", response));
    }

    @PostMapping("/transfer")
    public ResponseEntity<ApiResponse<TransactionResponse>> transfer(
            @Valid @RequestBody TransactionRequest request){
        log.info("Transfer request from: {}", request.getFromAccountNumber());
        TransactionResponse response = transactionService.transfer(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Money transferred", response));
    }

    @GetMapping("/history/{accountNumber}")
    public ResponseEntity<ApiResponse<List<TransactionResponse>>> getTransactionHistory(
            @PathVariable String accountNumber){
        log.info("Transaction history for account: {}", accountNumber);
        List<TransactionResponse> response = transactionService.getTransactionHistory(accountNumber);
        return ResponseEntity.ok(ApiResponse.success("Transaction history", response));
    }
}