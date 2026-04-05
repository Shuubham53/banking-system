package com.banking.account_service.service;

import com.banking.account_service.client.CustomerClient;
import com.banking.account_service.dto.AccountRequest;
import com.banking.account_service.dto.AccountResponse;
import com.banking.account_service.dto.CustomerResponse;
import com.banking.account_service.entity.Account;
import com.banking.account_service.enums.AccountStatus;
import com.banking.account_service.error.AccountAlreadyExistsException;
import com.banking.account_service.error.AccountNotFoundException;
import com.banking.account_service.error.ApiResponse;
import com.banking.account_service.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final CustomerClient customerClient;

    @Transactional
    public AccountResponse createAccount(AccountRequest request) {
        log.info("Creating account for customerId: {}", request.getCustomerId());
        try{
            ApiResponse<CustomerResponse> customerResponse =
                    customerClient.getCustomerByUserId(request.getUserId());

            if(customerResponse == null || !customerResponse.isSuccess()){
                throw new RuntimeException("Customer Not Found");
            }
        } catch (Exception e) {
            log.error("Customer validation failed {}",e.getMessage());
            throw new RuntimeException("Customer not found with userId: " + request.getUserId());
        }

        accountRepository.findByCustomerIdAndAccountType(
                        request.getCustomerId(), request.getAccountType())
                .ifPresent(a -> {
                    throw new AccountAlreadyExistsException(
                            "Account already exists for this type");
                });
        String accountNumber = generateAccountNumber();
        Account account = Account.builder()
                .userId(request.getUserId())
                .customerId(request.getCustomerId())
                .accountNumber(accountNumber)
                .accountStatus(AccountStatus.ACTIVE)
                .accountType(request.getAccountType())
                .createdAt(LocalDateTime.now())
                .balance(BigDecimal.valueOf(0))
                .build();
        accountRepository.save(account);
        log.info("Account Created for Customer with id {}",request.getCustomerId());
        return mapToResponse(account);
    }
    private AccountResponse mapToResponse(Account account) {
        return AccountResponse.builder()
                .id(account.getId())
                .userId(account.getUserId())
                .customerId(account.getCustomerId())
                .accountStatus(account.getAccountStatus())
                .accountType(account.getAccountType())
                .accountNumber(account.getAccountNumber())
                .balance(account.getBalance())
                .createdAt(account.getCreatedAt())
                .build();

    }

    public List<AccountResponse> getAccountsByUserId(Long userId) {
        List<Account> accounts = accountRepository.findByUserId(userId);
        return accounts.stream().map(this::mapToResponse).toList();
    }

    public AccountResponse getAccountByAccountNumber(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(()-> new AccountNotFoundException("Account not found"));
        return mapToResponse(account);
    }

    public AccountResponse updateAccountStatus(String accountNumber, AccountStatus status) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(()-> new AccountNotFoundException("Account not found"));

        account.setAccountStatus(status);
        accountRepository.save(account);
        log.info("Account status updated");
        return mapToResponse(account);
    }



    private String generateAccountNumber() {
        return "ACC" + System.currentTimeMillis();
    }
}