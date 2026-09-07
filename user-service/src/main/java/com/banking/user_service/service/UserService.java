package com.banking.user_service.service;

import com.banking.user_service.dto.LoginRequest;
import com.banking.user_service.dto.LoginResponse;
import com.banking.user_service.dto.RegisterRequest;
import com.banking.user_service.dto.UserResponse;
import com.banking.user_service.entity.User;
import com.banking.user_service.enums.Role;
import com.banking.user_service.error.UserAlreadyExistsException;
import com.banking.user_service.event.UserCreatedEvent;
import com.banking.user_service.kafka.UserEventProducer;
import com.banking.user_service.repository.UserRepository;
import com.banking.user_service.security.JwtUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final UserEventProducer userEventProducer;

    @Transactional
    public String register(RegisterRequest request){
        log.info("User trying to Register");
        userRepository.findByEmail(request.getEmail())
                .ifPresent(u -> {
                    throw new UserAlreadyExistsException("Username already exists");});
        User user = User.builder()
               .email(request.getEmail())
               .createdAt(LocalDateTime.now())
               .password(passwordEncoder.encode(request.getPassword()))
               .role(Role.CUSTOMER)
                .enabled(true)
               .build();
        userRepository.save(user);
        log.info("User registered successfully");
        UserCreatedEvent event = UserCreatedEvent.builder()
                .userId(user.getId())

                .email(user.getEmail())
                .build();
        try {
            userEventProducer.publishUserCreatedEvent(event);
            log.info("Kafka event published for userId: {}", user.getId());
        } catch (Exception e) {
            log.error("CRITICAL: Kafka event failed for userId: {}. Manual intervention required!",
                    user.getId());
        }
        return "User registered successfully";
    }

    public LoginResponse login(LoginRequest request){

        log.info("Login attempt for email: {}", request.getEmail());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(),request.getPassword()));

        String role = authentication.getAuthorities().stream()
                .findFirst()
                .map(GrantedAuthority::getAuthority)
                .orElse("ROLE_CUSTOMER")
                .replace("ROLE_", "");

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        String token = jwtUtil.generateToken(user.getEmail(), role);

        log.info("JWT token generated successfully for email: {}", request.getEmail());
        return LoginResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .build();
    }

    public UserResponse getUserById(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() ->
                new UsernameNotFoundException("User not found with Id: " + userId));

        return UserResponse.builder()
                .id(userId)
                .email(user.getEmail())
                .build();
    }
}
