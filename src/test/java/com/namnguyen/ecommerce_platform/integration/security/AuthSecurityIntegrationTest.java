package com.namnguyen.ecommerce_platform.integration.security;

import com.namnguyen.ecommerce_platform.auth.dto.LoginRequest;
import com.namnguyen.ecommerce_platform.auth.dto.RegisterRequest;
import com.namnguyen.ecommerce_platform.integration.BaseSecurityIntegrationTest;
import com.namnguyen.ecommerce_platform.user.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static com.namnguyen.ecommerce_platform.testutil.TestDataFactory.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


public class AuthSecurityIntegrationTest extends BaseSecurityIntegrationTest {

    @Test
    void registerUser_withoutToken_returnsCreated() throws Exception {
        RegisterRequest request = createDefaultRegisterRequest();

        mockMvc.perform(post(REGISTER_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").exists());
    }

    @Test
    void loginUser_withoutToken_returnsToken() throws Exception {
        createDefaultCustomer();

        LoginRequest request = createDefaultLoginRequest();

        mockMvc.perform(post(LOGIN_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());
    }

    @Test
    void loginUser_withInvalidTokenStillUsesPublicEndpoint_returnsOk() throws Exception {
        createDefaultCustomer();

        LoginRequest request = createDefaultLoginRequest();

        mockMvc.perform(post(LOGIN_URI)
                        .header("Authorization", "Bearer invalid.token.value")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());
    }
}
