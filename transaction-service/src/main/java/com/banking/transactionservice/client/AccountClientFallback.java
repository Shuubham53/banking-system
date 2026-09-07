package com.banking.transactionservice.client;


import com.banking.transactionservice.dto.AccountResponse;
import com.banking.transactionservice.error.ApiResponse;
import com.banking.transactionservice.error.ServiceUnavailableException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class AccountClientFallback implements AccountClient {

    @Override
    public ApiResponse<AccountResponse> getAccountByAccountNumber(String accountNumber) {
        throw new ServiceUnavailableException("Account service is temporarily unavailable, please try again shortly");
    }

    @Override
    public ApiResponse<AccountResponse> updateBalance(String accountNumber, BigDecimal amount) {
        throw new ServiceUnavailableException("Account service is temporarily unavailable, please try again shortly");
    }
}
