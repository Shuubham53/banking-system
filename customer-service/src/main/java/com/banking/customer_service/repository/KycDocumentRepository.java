package com.banking.customer_service.repository;

import com.banking.customer_service.entity.KycDocument;
import com.banking.customer_service.enums.KycStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface KycDocumentRepository extends JpaRepository<KycDocument, Long> {
    List<KycDocument> findAllByCustomerId(Long customerId);
    List<KycDocument> findAllByStatus(KycStatus status);

}