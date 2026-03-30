package com.banking.user_service;
import static org.junit.jupiter.api.Assertions.*;
import com.banking.user_service.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;


public class JwtsUtilTest {
    private JwtUtil jwtUtil;
    private String testUsername = "shubham";
    private String token = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJzaHViaGFtIiwiaWF0IjoxNzc0Njg1OTA5LCJleHAiOjE3NzQ3NzIzMDl9.G72gnXjd3DQt18C2EuVHSwOghKp_UtJgXQoCybWFsZ0";

    @BeforeEach
    void setup() {
        jwtUtil = new JwtUtil();
        // inject values manually since no Spring context
        ReflectionTestUtils.setField(jwtUtil, "secretKey", "test-secret-key-for-junit-testing-256-bits");
        ReflectionTestUtils.setField(jwtUtil, "expirationTime", 86400000L);
    }


    @Test
    void shouldGenerateToken(){
        String token = jwtUtil.generateToken(testUsername);
        System.out.println(token);
        assertNotNull(token);
    }
    

    @Test
    void shouldExtractUsernameFromToken(){
        String username = jwtUtil.extractUsername(token);
        System.out.println(username);
        assertEquals(testUsername,username);
    }
    @Test
    void ShouldValidateToken(){
        assertTrue(jwtUtil.validateToken(token));
    }

    @Test
    void shouldRejectInvalidToken(){
        assertFalse(jwtUtil.validateToken("Invalid_token"));//   invalid token
    }
}
