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
    void createProduct_whenAdminJwt_returnsCreated() throws Exception {
        persistDefaultAdmin();

        String token = loginAndGetToken(
                VALID_ADMIN_EMAIL,
                VALID_PASSWORD
        );

        mockMvc.perform(post(PRODUCT_URI)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                createDefaultProductCreateRequest()
                        )))
                .andExpect(status().isCreated());
    }

    @Test
    void createProduct_whenInvalidToken_returnsUnauthorized() throws Exception {
        persistDefaultAdmin();

        String validToken = loginAndGetToken(
                VALID_ADMIN_EMAIL,
                VALID_PASSWORD
        );

        String tamperedToken = validToken + "abc";

        mockMvc.perform(post(PRODUCT_URI)
                        .header("Authorization", "Bearer " + tamperedToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                createDefaultProductCreateRequest()
                        )))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createProduct_whenMissingToken_returnsUnauthorized() throws Exception {
        mockMvc.perform(post(PRODUCT_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                createDefaultProductCreateRequest()
                        )))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createProduct_whenCustomerJwt_returnsForbidden() throws Exception {
        persistDefaultCustomer();

        String token = loginAndGetToken(
                VALID_EMAIL,
                VALID_PASSWORD
        );

        mockMvc.perform(post(PRODUCT_URI)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                createDefaultProductCreateRequest()
                        )))
                .andExpect(status().isForbidden());
    }
}
