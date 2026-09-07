package com.banking.user_service.controller;

import com.banking.user_service.dto.UserResponse;
import com.banking.user_service.error.ApiResponse;
import com.banking.user_service.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long userId){
        UserResponse response = userService.getUserById(userId);
        return ResponseEntity.ok(ApiResponse.success("User fetched by id",response));
    }
}
