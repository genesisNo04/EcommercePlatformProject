package com.namnguyen.ecommerce_platform.jwt;

import com.namnguyen.ecommerce_platform.security.jwt.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import static com.namnguyen.ecommerce_platform.testutil.TestMessages.*;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class JwtServiceImplTest {

    @InjectMocks
    private JwtService jwtService;

    @Test
    void generateToken_whenUserIsValid_returnToken() {
        UserDetails userDetails = User
                .withUsername("test@gmail.com")
                .password("test")
                .roles("CUSTOMER")
                .build();

        String token = jwtService.generateToken(userDetails);

        assertThat(token).isNotNull();
    }
}
