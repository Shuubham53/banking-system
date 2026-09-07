package com.banking.customer_service.entity;

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
@Table(name = "kyc_audit_logs")
public class KycAuditing {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long customerId;

    @Enumerated(EnumType.STRING)
    private KycStatus previousStatus;

    @Enumerated(EnumType.STRING)
    private KycStatus newStatus;

    private String changedBy;

    private String reason;

    private LocalDateTime timestamp;
}
