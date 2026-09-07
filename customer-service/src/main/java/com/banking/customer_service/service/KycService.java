package com.banking.customer_service.service;


import com.banking.customer_service.dto.KycDocumentResponse;
import com.banking.customer_service.dto.KycDocumentUploadRequest;
import com.banking.customer_service.dto.KycReviewRequest;
import com.banking.customer_service.entity.Customer;
import com.banking.customer_service.entity.KycAuditing;
import com.banking.customer_service.entity.KycDocument;
import com.banking.customer_service.enums.DocumentType;
import com.banking.customer_service.enums.KycStatus;
import com.banking.customer_service.error.CustomerNotFoundException;
import com.banking.customer_service.error.KycValidationException;
import com.banking.customer_service.repository.CustomerRepository;
import com.banking.customer_service.repository.KycAuditingRepository;
import com.banking.customer_service.repository.KycDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class KycService {

    private final KycDocumentRepository kycDocumentRepository;
    private final KycAuditingRepository kycAuditingRepository;
    private final CustomerRepository customerRepository;

    public KycDocumentResponse uploadDocument(KycDocumentUploadRequest request) {
        log.info("Uploading KYC document for customerId {}", request.getCustomerId());

        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new CustomerNotFoundException(
                        "Customer not found with id: " + request.getCustomerId()));

        validateDocumentFormat(request.getDocumentType(), request.getDocumentNumber());
        validateNameMatch(request.getDocumentHolderName(), customer.getFirstName(), customer.getLastName());

        KycDocument document = KycDocument.builder()
                .customerId(request.getCustomerId())
                .documentType(request.getDocumentType())
                .documentNumber(request.getDocumentNumber())
                .documentHolderName(request.getDocumentHolderName())
                .fileUrl(request.getFileUrl())
                .status(KycStatus.PENDING)
                .uploadedAt(LocalDateTime.now())
                .build();

        kycDocumentRepository.save(document);
        log.info("KYC document uploaded for customerId {}", request.getCustomerId());
        return mapToResponse(document);
    }

    public List<KycDocumentResponse> getDocumentsByCustomer(Long customerId) {
        return kycDocumentRepository.findAllByCustomerId(customerId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<KycDocumentResponse> getPendingDocuments() {
        return kycDocumentRepository.findAllByStatus(KycStatus.PENDING)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public KycDocumentResponse reviewDocument(Long documentId, KycReviewRequest request) {
        log.info("Reviewing KYC document {}", documentId);

        KycDocument document = kycDocumentRepository.findById(documentId)
                .orElseThrow(() -> new CustomerNotFoundException("Document not found: " + documentId));

        KycStatus previousStatus = document.getStatus();

        document.setStatus(request.getStatus());
        document.setRejectionReason(request.getRejectionReason());
        document.setReviewedAt(LocalDateTime.now());

        kycDocumentRepository.save(document);

        String reviewedBy = SecurityContextHolder.getContext().getAuthentication().getName();

        KycAuditing audit = KycAuditing.builder()
                .customerId(document.getCustomerId())
                .previousStatus(previousStatus)
                .newStatus(request.getStatus())
                .changedBy(reviewedBy)
                .reason(request.getRejectionReason())
                .timestamp(LocalDateTime.now())
                .build();
        kycAuditingRepository.save(audit);


        if (request.getStatus() == KycStatus.VERIFIED) {
            Customer customer = customerRepository.findById(document.getCustomerId())
                    .orElseThrow(() -> new CustomerNotFoundException(
                            "Customer not found with id: " + document.getCustomerId()));
            customer.setKycStatus(KycStatus.VERIFIED);
            customer.setUpdatedAt(LocalDateTime.now());
            customerRepository.save(customer);
            log.info("Customer {} KYC status set to VERIFIED", document.getCustomerId());
        }

        log.info("KYC document {} reviewed: {}", documentId, request.getStatus());
        return mapToResponse(document);
    }

    private KycDocumentResponse mapToResponse(KycDocument document) {
        return KycDocumentResponse.builder()
                .id(document.getId())
                .customerId(document.getCustomerId())
                .documentType(document.getDocumentType())
                .documentNumber(document.getDocumentNumber())
                .documentHolderName(document.getDocumentHolderName())
                .fileUrl(document.getFileUrl())
                .status(document.getStatus())
                .rejectionReason(document.getRejectionReason())
                .uploadedAt(document.getUploadedAt())
                .reviewedAt(document.getReviewedAt())
                .build();
    }

    private void validateDocumentFormat(DocumentType documentType, String documentNumber) {
        boolean isValid = switch (documentType) {
            case AADHAR -> documentNumber.matches("\\d{12}");
            case PAN -> documentNumber.matches("[A-Z]{5}\\d{4}[A-Z]");
            case PASSPORT -> documentNumber.matches("[A-Z]\\d{7}");
            case ADDRESS_PROOF -> documentNumber != null && !documentNumber.isBlank();
        };

        if (!isValid) {
            throw new KycValidationException(
                    "Invalid document number format for " + documentType);
        }
    }
    private void validateNameMatch(String documentHolderName, String firstName, String lastName) {
        String normalizedDocName = documentHolderName.toLowerCase().trim();
        String normalizedFirstName = firstName.toLowerCase().trim();
        String normalizedLastName = lastName.toLowerCase().trim();

        boolean firstNameMatches = normalizedDocName.contains(normalizedFirstName);
        boolean lastNameMatches = normalizedDocName.contains(normalizedLastName);

        if (!firstNameMatches || !lastNameMatches) {
            throw new KycValidationException(
                    "Document holder name does not match customer name on file");
        }
    }
}