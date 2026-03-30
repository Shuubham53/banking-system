//package com.banking.user_service;
//
//import com.banking.user_service.dto.LoginRequest;
//import com.banking.user_service.dto.RegisterRequest;
//import com.banking.user_service.entity.User;
//import com.banking.user_service.error.UserAlreadyExistsException;
//import com.banking.user_service.repository.UserRepository;
//import com.banking.user_service.security.JwtUtil;
//import com.banking.user_service.service.UserService;
//import org.junit.jupiter.api.Disabled;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.junit.jupiter.api.Assertions.*;
//import org.springframework.security.authentication.AuthenticationManager;
//import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
//import org.springframework.security.crypto.password.PasswordEncoder;
//
//import java.util.Optional;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.junit.jupiter.api.Assertions.assertThrows;
//import static org.mockito.Mockito.when;
//
//@ExtendWith(MockitoExtension.class)
//public class UserServiceTest {
//    @Mock
//    private UserRepository userRepository;
//
//    @InjectMocks
//    private UserService userService;
//    @Mock
//    private PasswordEncoder passwordEncoder;
//
//
//
//    @Test
//    @Disabled
//    void shouldThrowUserAlreadyExistException(){
//        RegisterRequest request =
//                new RegisterRequest("shubham","shubhamnishadd@gmail.com","123456");
//
//        when(userRepository.findByUsername("shubham"))
//                .thenReturn(Optional.of(new User()));
//
//
//        assertThrows(UserAlreadyExistsException.class, () -> {
//            userService.register(request);
//        });
//    }
//
//    @Mock
//    private JwtUtil jwtUtil;
//
//    @Mock
//    private AuthenticationManager authenticationManager;
//
//    @Test
//    void shouldLoginSuccessfully(){
//        LoginRequest request = new LoginRequest("shubham","123456");
//        when(authenticationManager.authenticate(
//                new UsernamePasswordAuthenticationToken(request.getUsername(),request.getPassword()))).thenReturn(null);
//
//        when(jwtUtil.generateToken("shubham")).thenReturn("fakeToken");
//        String actual = jwtUtil.generateToken(request.getUsername());
//
//
//        assertEquals("fakeToken",actual);
//
//    }
//
//}
