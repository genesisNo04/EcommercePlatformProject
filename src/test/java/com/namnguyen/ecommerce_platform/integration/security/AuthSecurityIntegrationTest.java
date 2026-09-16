package com.namnguyen.ecommerce_platform.integration.security;

import com.namnguyen.ecommerce_platform.auth.dto.LoginRequest;
import com.namnguyen.ecommerce_platform.auth.dto.RegisterRequest;
import com.namnguyen.ecommerce_platform.integration.BaseSecurityIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static com.namnguyen.ecommerce_platform.testutil.TestDataFactory.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


public class AuthSecurityIntegrationTest extends BaseSecurityIntegrationTest {

    @Test
    void registerUser_withoutToken_returnsCreated() throws Exception {
        RegisterRequest registerRequest = createDefaultRegisterRequest();

        mockMvc.perform(post(REGISTER_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").exists());
    }

    @Test
    void loginUser_withoutToken_returnsToken() throws Exception {
        persistDefaultCustomer();

        LoginRequest loginRequest = createDefaultLoginRequest();

        mockMvc.perform(post(LOGIN_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());
    }

    @Test
    void loginUser_withInvalidToken_onPublicEndpoint_returnsOk() throws Exception {
        persistDefaultCustomer();

        LoginRequest loginRequest = createDefaultLoginRequest();

        mockMvc.perform(post(LOGIN_URI)
                        .header("Authorization", "Bearer invalid.token.value")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());
    }
}
