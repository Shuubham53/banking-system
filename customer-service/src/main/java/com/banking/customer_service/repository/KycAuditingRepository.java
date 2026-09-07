package com.banking.customer_service.repository;


import com.banking.customer_service.entity.KycAuditing;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KycAuditingRepository extends JpaRepository<KycAuditing, Long> {
    List<KycAuditing> findAllByCustomerId(Long customerId);
}