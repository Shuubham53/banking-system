package com.banking.customer_service.service;

import com.banking.customer_service.dto.CustomerRequest;
import com.banking.customer_service.dto.CustomerResponse;
import com.banking.customer_service.entity.Customer;
import com.banking.customer_service.enums.KycStatus;
import com.banking.customer_service.error.CustomerAlreadyExistsException;
import com.banking.customer_service.error.CustomerNotFoundException;
import com.banking.customer_service.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;

    @Transactional
    public CustomerResponse createCustomer(Long userId, CustomerRequest request) {
        log.info("Creating profile for usedId {}",userId);
        customerRepository.findByUserId(userId)
                .ifPresent(c -> {
                    throw new CustomerAlreadyExistsException("Customer already exists with ID: " + userId);
                });
        Customer customer = Customer.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phone(request.getPhone())
                .address(request.getAddress())
                .userId(userId)
                .kycStatus(KycStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
        customerRepository.save(customer);
        log.info("Customer profile created for userId: {}", userId);
        return mapToResponse(customer);
    }

    public CustomerResponse getCustomerByUserId(Long userId) {
        log.info("Fetching customer profile");
        Customer customer = customerRepository.findByUserId(userId).orElseThrow(() ->
                new CustomerNotFoundException("Customer not found with userId "+userId));
        return mapToResponse(customer);
    }


    @Transactional
    public CustomerResponse updateKycStatus(Long userId, KycStatus status) {
        log.info("Updating Customer KycStatus");
        Customer customer = customerRepository.findByUserId(userId).orElseThrow(() ->
                new CustomerNotFoundException("Customer not found with userId "+userId));
        customer.setKycStatus(status);
        customerRepository.save(customer);
        log.info("Customer KycStatus updated successfully");
        return mapToResponse(customer);
    }

    public CustomerResponse mapToResponse(Customer customer){
        return CustomerResponse.builder()
                .id(customer.getId())
                .firstName(customer.getFirstName())
                .lastName(customer.getLastName())
                .phone(customer.getPhone())
                .address(customer.getAddress())
                .kycStatus(customer.getKycStatus())
                .dateOfBirth(customer.getDateOfBirth())
                .userId(customer.getUserId())
                .createdAt(customer.getCreatedAt())
                .build();
    }
}