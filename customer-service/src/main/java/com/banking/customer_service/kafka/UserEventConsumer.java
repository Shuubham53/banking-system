package com.banking.customer_service.kafka;

import com.banking.customer_service.entity.Customer;
import com.banking.customer_service.enums.KycStatus;
import com.banking.customer_service.event.UserCreatedEvent;
import com.banking.customer_service.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserEventConsumer {

    private final CustomerRepository customerRepository;

    @KafkaListener(topics = "user-created", groupId = "customer-service-group")
    public void consumeUserCreatedEvent(UserCreatedEvent event) {
        log.info("Consuming USER_CREATED event for userId {}", event.getUserId());
        Optional<Customer> existingCustomer = customerRepository.findByUserId(event.getUserId());
        if (existingCustomer.isPresent()) {
            log.warn("User already exists with userId {}", event.getUserId());
            return; // stop duplicate insert
        }

        Customer customer = Customer.builder()
                .userId(event.getUserId())
                .kycStatus(KycStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        customerRepository.save(customer);

        log.info("USER_CREATED event consumed successfully");
    }
}