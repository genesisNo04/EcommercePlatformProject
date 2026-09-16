package com.namnguyen.ecommerce_platform.jwt.service;

import com.namnguyen.ecommerce_platform.security.jwt.JwtService;
import com.namnguyen.ecommerce_platform.user.enums.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;

import java.util.Date;
import java.util.List;

import static com.namnguyen.ecommerce_platform.testutil.TestDataFactory.*;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

public class JwtServiceTest {

    private JwtService jwtService;

    private static final String TEST_SECRET_KEY =
            "MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=";

    private static final long TEST_EXPIRATION_MS = 60 * 60 * 1000L;
    private static final long TEST_EXPIRED_MS = -1000L;

    private SecretKey getTestSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(TEST_SECRET_KEY);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private UserDetails createCustomerUserDetails(String email) {
        return User
                .withUsername(email)
                .password(ENCODED_PASSWORD)
                .roles(Role.CUSTOMER.name())
                .build();
    }

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();

        ReflectionTestUtils.setField(jwtService, "secretKey", TEST_SECRET_KEY);
        ReflectionTestUtils.setField(jwtService, "jwtExpirationMs", TEST_EXPIRATION_MS);
    }

    @Test
    void generateToken_whenUserIsValid_returnToken() {
        UserDetails userDetails = createCustomerUserDetails(VALID_EMAIL);

        String token = jwtService.generateToken(userDetails);


        Claims claims = Jwts.parser()
                            .verifyWith(getTestSigningKey())
                            .build()
                            .parseSignedClaims(token)
                            .getPayload();

        assertThat(token).isNotBlank();
        assertThat(claims.getSubject()).isEqualTo(userDetails.getUsername());
        assertThat(claims.get("roles", List.class)).containsExactly("ROLE_CUSTOMER");
        assertThat(claims.getIssuedAt()).isNotNull();
        assertThat(claims.getExpiration()).isAfter(new Date());
    }

    @Test
    void extractUsername_whenTokenIsValid_returnUsername() {
        UserDetails userDetails = createCustomerUserDetails(VALID_EMAIL);

        String token = jwtService.generateToken(userDetails);

        String username = jwtService.extractUsername(token);

        assertThat(username).isEqualTo(userDetails.getUsername());
    }

    @Test
    void extractExpiration_whenTokenIsValid_returnExpirationDate() {
        UserDetails userDetails = createCustomerUserDetails(VALID_EMAIL);

        String token = jwtService.generateToken(userDetails);

        Date expiration = jwtService.extractExpiration(token);

        assertThat(expiration).isAfter(new Date());
    }

    @Test
    void isTokenValid_whenTokenBelongsToUser_returnsTrue() {
        UserDetails userDetails = createCustomerUserDetails(VALID_EMAIL);

        String token = jwtService.generateToken(userDetails);

        assertTrue(jwtService.isTokenValid(token, userDetails));
    }

    @Test
    void isTokenValid_whenTokenBelongsToDifferentUser_returnsFalse() {
        UserDetails userDetails = createCustomerUserDetails(VALID_EMAIL);

        UserDetails differentUserDetails = createCustomerUserDetails("test1@gmail.com");

        String token = jwtService.generateToken(userDetails);

        assertFalse(jwtService.isTokenValid(token, differentUserDetails));
    }

    @Test
    void isTokenValid_whenTokenExpired_returnsFalse() {
        ReflectionTestUtils.setField(jwtService, "jwtExpirationMs", TEST_EXPIRED_MS);

        UserDetails userDetails = createCustomerUserDetails(VALID_EMAIL);

        String token = jwtService.generateToken(userDetails);

        assertFalse(jwtService.isTokenValid(token, userDetails));
    }
}
