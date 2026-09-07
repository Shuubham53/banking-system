package com.banking.customer_service.controller;


import com.banking.customer_service.dto.KycDocumentResponse;
import com.banking.customer_service.dto.KycDocumentUploadRequest;
import com.banking.customer_service.dto.KycReviewRequest;
import com.banking.customer_service.error.ApiResponse;
import com.banking.customer_service.service.KycService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/kyc-documents")
@Slf4j
public class KycController {

    private final KycService kycService;

    @PostMapping
    public ResponseEntity<ApiResponse<KycDocumentResponse>> uploadDocument(
            @Valid @RequestBody KycDocumentUploadRequest request) {
        KycDocumentResponse response = kycService.uploadDocument(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Document uploaded", response));
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<ApiResponse<List<KycDocumentResponse>>> getDocumentsByCustomer(
            @PathVariable Long customerId) {
        List<KycDocumentResponse> documents = kycService.getDocumentsByCustomer(customerId);
        return ResponseEntity.ok(ApiResponse.success("Customer documents", documents));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<KycDocumentResponse>>> getPendingDocuments() {
        List<KycDocumentResponse> documents = kycService.getPendingDocuments();
        return ResponseEntity.ok(ApiResponse.success("Pending documents", documents));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{documentId}/review")
    public ResponseEntity<ApiResponse<KycDocumentResponse>> reviewDocument(
            @PathVariable Long documentId,
            @Valid @RequestBody KycReviewRequest request) {
        KycDocumentResponse response = kycService.reviewDocument(documentId, request);
        return ResponseEntity.ok(ApiResponse.success("Document reviewed", response));
    }
}