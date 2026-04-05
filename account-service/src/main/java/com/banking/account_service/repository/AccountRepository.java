package com.banking.account_service.repository;

import com.banking.account_service.entity.Account;
import com.banking.account_service.enums.AccountType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account,Long> {
    List<Account> findByUserId(Long userId);
    Optional<Account> findByAccountNumber(String accountNumber);
    Optional<Account> findByCustomerIdAndAccountType(Long customerId, AccountType accountType);

}
