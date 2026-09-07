package com.banking.customer_service.dto;

import com.banking.customer_service.enums.KycStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class KycReviewRequest {
    @NotNull(message = "status is required")
    private KycStatus status;

    private String rejectionReason;
}
