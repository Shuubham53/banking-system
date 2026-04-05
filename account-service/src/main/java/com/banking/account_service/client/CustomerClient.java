package com.banking.account_service.client;


import com.banking.account_service.dto.CustomerResponse;
import com.banking.account_service.error.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "customer-service")
public interface CustomerClient {

    @GetMapping("/api/customers/{userId}")
    ApiResponse<CustomerResponse> getCustomerByUserId(@PathVariable Long userId);
}