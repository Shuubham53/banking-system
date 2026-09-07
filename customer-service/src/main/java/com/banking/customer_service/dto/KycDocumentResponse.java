package com.banking.customer_service.dto;

import com.banking.customer_service.enums.DocumentType;
import com.banking.customer_service.enums.KycStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KycDocumentResponse {
    private Long id;
    private Long customerId;
    private DocumentType documentType;
    private String documentNumber;
    private String documentHolderName;
    private String fileUrl;
    private KycStatus status;
    private String rejectionReason;
    private LocalDateTime uploadedAt;
    private LocalDateTime reviewedAt;
}