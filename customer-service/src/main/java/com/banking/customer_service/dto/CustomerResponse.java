package com.banking.customer_service.dto;

import com.banking.customer_service.enums.KycStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor

public class CustomerResponse {
    private Long id;
    private Long userId;
    private String firstName;
    private String lastName;
    private String phone;
    private String address;
    private LocalDate dateOfBirth;
    private KycStatus kycStatus;
    private LocalDateTime createdAt;
}
