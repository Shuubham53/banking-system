package com.banking.user_service.controller;

import com.banking.user_service.dto.LoginRequest;
import com.banking.user_service.dto.RegisterRequest;
import com.banking.user_service.error.ApiResponse;
import com.banking.user_service.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<String>> register(@Valid @RequestBody RegisterRequest request){
        log.info("Register request received  for username {}",request.getUsername());
        String response = userService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<String>> login(@Valid @RequestBody LoginRequest request){
        log.info("Login request received for username {}",request.getUsername());
        String response = userService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successfully",response));
    }

}