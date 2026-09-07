package com.banking.transactionservice.client;


import com.banking.transactionservice.dto.UserResponse;
import com.banking.transactionservice.error.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service")
public interface UserClient {

    @GetMapping("/api/users/{userId}")
    ApiResponse<UserResponse> getUserById(@PathVariable Long userId);
}