package com.banking.account_service.kafka;

import com.banking.account_service.event.FraudAlertEvent;
import com.banking.account_service.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class FraudAlertConsumer {
    private final AccountRepository accountRepository;
    @KafkaListener(topics = "fraud-alerts", groupId = "account-service-group")
    public void consumeFraudAlert(FraudAlertEvent event) {
        log.warn("Received FRAUD ALERT for account: {}", event.getAccountNumber());

        accountRepository.findByAccountNumber(event.getAccountNumber())
                .ifPresent(account -> {
                    account.setAccountStatus(com.banking.account_service.enums.AccountStatus.FROZEN);
                    accountRepository.save(account);
                    log.warn("Account FROZEN due to fraud: {}", event.getAccountNumber());
                });
    }

}
