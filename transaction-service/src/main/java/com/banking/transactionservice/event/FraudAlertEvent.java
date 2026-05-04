package com.banking.transactionservice.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FraudAlertEvent {
    private String accountNumber;
    private BigDecimal amount;
    private String transactionType;
    private String transactionId;
}
