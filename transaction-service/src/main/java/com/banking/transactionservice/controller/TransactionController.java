package com.banking.transactionservice.controller;

import com.banking.transactionservice.dto.OtpConfirmRequest;
import com.banking.transactionservice.dto.TransactionRequest;
import com.banking.transactionservice.dto.TransactionResponse;

import com.banking.transactionservice.enums.TransactionStatus;
import com.banking.transactionservice.error.ApiResponse;
import com.banking.transactionservice.service.IdempotencyService;
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
    private final IdempotencyService idempotencyService;

    @PostMapping("/deposit")
    public ResponseEntity<ApiResponse<TransactionResponse>> deposit(
            @Valid @RequestBody TransactionRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey){
        log.info("Deposit request for account: {}", request.getToAccountNumber());

        if (idempotencyKey != null) {
            var cached = idempotencyService.checkIfProcessed(idempotencyKey);
            if (cached.isPresent()) {
                log.info("Returning cached response for idempotency key: {}", idempotencyKey);
                return ResponseEntity.status(HttpStatus.CREATED)
                        .body(ApiResponse.success("Money deposited", cached.get()));
            }
        }

        TransactionResponse response = transactionService.deposit(request);

        if (idempotencyKey != null) {
            idempotencyService.storeResult(idempotencyKey, response);
        }
        return ResponseEntity.status(HttpStatus.CREATED).
                body(ApiResponse.success("Money deposited", response));
    }

    @PostMapping("/withdraw")
    public ResponseEntity<ApiResponse<TransactionResponse>> withdraw(
            @Valid @RequestBody TransactionRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey){
        log.info("Withdraw request for account: {}", request.getFromAccountNumber());

        if (idempotencyKey != null) {
            var cached = idempotencyService.checkIfProcessed(idempotencyKey);
            if (cached.isPresent()) {
                log.info("Returning cached response for idempotency key: {}", idempotencyKey);
                return ResponseEntity.status(HttpStatus.CREATED)
                        .body(ApiResponse.success("Money withdrawn", cached.get()));
            }
        }

        TransactionResponse response = transactionService.withdraw(request);

        if (idempotencyKey != null) {
            idempotencyService.storeResult(idempotencyKey, response);
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Money withdrawn", response));
    }

    @PostMapping("/transfer")
    public ResponseEntity<ApiResponse<TransactionResponse>> transfer(
            @Valid @RequestBody TransactionRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey){
        log.info("Transfer request from: {}", request.getFromAccountNumber());

        if (idempotencyKey != null) {
            var cached = idempotencyService.checkIfProcessed(idempotencyKey);
            if (cached.isPresent()) {
                log.info("Returning cached response for idempotency key: {}", idempotencyKey);
                return ResponseEntity.status(HttpStatus.CREATED)
                        .body(ApiResponse.success("Money transferred", cached.get()));
            }
        }

        TransactionResponse response = transactionService.transfer(request);

        String message = response.getTransactionStatus() == TransactionStatus.PENDING
                ? "OTP required to complete transfer"
                : "Money transferred";

        if (idempotencyKey != null) {
            idempotencyService.storeResult(idempotencyKey, response);
        }

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(message, response));
    }

    @GetMapping("/history/{accountNumber}")
    public ResponseEntity<ApiResponse<List<TransactionResponse>>> getTransactionHistory(
            @PathVariable String accountNumber){
        log.info("Transaction history for account: {}", accountNumber);
        List<TransactionResponse> response = transactionService.getTransactionHistory(accountNumber);
        return ResponseEntity.ok(ApiResponse.success("Transaction history", response));
    }
    @PostMapping("/confirm-otp/{transactionId}")
    public ResponseEntity<ApiResponse<TransactionResponse>> confirmOtp(@PathVariable String transactionId,
                                                                       @RequestBody OtpConfirmRequest request) {
        TransactionResponse response = transactionService.confirmOtp(transactionId, request.getOtp());
        return ResponseEntity.ok(ApiResponse.success("OTP verified", response));
    }

}



