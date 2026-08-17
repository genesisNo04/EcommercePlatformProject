package com.namnguyen.ecommerce_platform.integration.security;

import com.namnguyen.ecommerce_platform.auth.dto.LoginRequest;
import com.namnguyen.ecommerce_platform.integration.BaseSecurityIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static com.namnguyen.ecommerce_platform.testutil.TestDataFactory.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class JwtSecurityIntegrationTest extends BaseSecurityIntegrationTest {
    @Test
    void login_withValidCredentials_returnsJwtToken() throws Exception {
        createDefaultCustomer();

        LoginRequest request = new LoginRequest (
                "customer@gmail.com",
                "test123456789"
        );

        mockMvc.perform(post(LOGIN_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());
    }

    @Test
    void login_withInvalidPassword_returnsUnauthorized() throws Exception {
        createDefaultAdmin();

        LoginRequest request = new LoginRequest(
                "admin@gmail.com",
                "wrongpassword"
        );

        mockMvc.perform(post(LOGIN_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createProduct_whenCustomerJwt_returnsForbidden() throws Exception {
        createDefaultCustomer();

        String token = loginAndGetToken("customer@gmail.com", "test123456789");

        mockMvc.perform(post(PRODUCT_URI)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDefaultProductCreateRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    void createProduct_whenAdminJwt_returnsCreated() throws Exception {
        createDefaultAdmin();

        String token = loginAndGetToken("admin@gmail.com", "test123456789");

        mockMvc.perform(post(PRODUCT_URI)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDefaultProductCreateRequest())))
                .andExpect(status().isCreated());
    }

    @Test
    void createProduct_whenInvalidToken_returnsUnauthorized() throws Exception {
        String rawPassword = "test123456789";

        createDefaultAdmin();

        String validToken = loginAndGetToken("admin@gmail.com", rawPassword);

        String invalidToken = validToken + "abc";

        mockMvc.perform(post(PRODUCT_URI)
                        .header("Authorization", "Bearer " + invalidToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDefaultProductCreateRequest())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createProduct_whenMissingToken_returnsUnauthorized() throws Exception {
        mockMvc.perform(post(PRODUCT_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDefaultProductCreateRequest())))
                .andExpect(status().isUnauthorized());
    }

}
