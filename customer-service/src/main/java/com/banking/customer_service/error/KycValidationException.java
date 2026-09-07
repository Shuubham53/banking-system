package com.banking.customer_service.error;


public class KycValidationException extends RuntimeException {
    public KycValidationException(String message) {
        super(message);
    }
}