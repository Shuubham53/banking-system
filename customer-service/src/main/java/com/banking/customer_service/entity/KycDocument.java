package com.banking.customer_service.entity;

import com.banking.customer_service.enums.DocumentType;
import com.banking.customer_service.enums.KycStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "kyc_documents")
public class KycDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private Long customerId;

    @Enumerated(EnumType.STRING)
    private DocumentType documentType;

    private String fileUrl;

    @Enumerated(EnumType.STRING)
    private KycStatus status;

    private String documentNumber;

    private String documentHolderName;

    private String rejectionReason;

    private LocalDateTime uploadedAt;

    private LocalDateTime reviewedAt;
}
