package com.banking.customer_service.controller;


import com.banking.customer_service.dto.CustomerRequest;
import com.banking.customer_service.dto.CustomerResponse;
import com.banking.customer_service.enums.KycStatus;
import com.banking.customer_service.error.ApiResponse;
import com.banking.customer_service.service.CustomerService;
import jakarta.validation.Valid;
import jakarta.ws.rs.Path;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/customers")
@Slf4j
public class CustomerController {
    private final CustomerService customerService;

    @PostMapping("/{userId}")
    public ResponseEntity<ApiResponse<CustomerResponse>> createCustomer(
            @Valid @RequestBody CustomerRequest request, @PathVariable Long userId){
        log.info("Creating customer profile for userId: {}", userId);
        CustomerResponse response = customerService.createCustomer(userId,request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("customer profile created",response));
    }
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<CustomerResponse>> getCustomerByUserId(@PathVariable Long userId){
        log.info("Fetching customer profile for userId: {}", userId);
        CustomerResponse response = customerService.getCustomerByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success("Fetching Customer profile",response));
    }
    @PatchMapping("/{userId}/kyc")
    public ResponseEntity<ApiResponse<CustomerResponse>> updateKycStatus(@PathVariable Long userId, @RequestParam KycStatus status){
        log.info("Updating KYC status for userId: {}", userId);
        CustomerResponse response = customerService.updateKycStatus(userId,status);
        return ResponseEntity.ok(ApiResponse.success("Customer Kyc updated",response));
    }
}
