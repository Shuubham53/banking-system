package com.banking.customer_service.repository;

import com.banking.customer_service.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


public interface CustomerRepository extends JpaRepository<Customer,Long> {
    Optional<Customer> findByUserId(Long userId);
    Optional<Customer> findByPhone(String phone);
}
