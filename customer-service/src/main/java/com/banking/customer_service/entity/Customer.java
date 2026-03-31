package com.banking.customer_service.entity;

import com.banking.customer_service.enums.KycStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "customers")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private Long userId;

    private String firstName;

    private String lastName;

    private String phone;

    private String address;

    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    private KycStatus kycStatus;

    private String kycDocument;

    private LocalDateTime createdAt;
}

