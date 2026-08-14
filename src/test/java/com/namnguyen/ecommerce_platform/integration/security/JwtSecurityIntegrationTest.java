package com.namnguyen.ecommerce_platform.integration.security;

import com.namnguyen.ecommerce_platform.auth.dto.LoginRequest;
import com.namnguyen.ecommerce_platform.integration.BaseSecurityIntegrationTest;
import com.namnguyen.ecommerce_platform.product.dto.ProductCreateRequest;
import com.namnguyen.ecommerce_platform.user.entity.User;
import com.namnguyen.ecommerce_platform.user.enums.Role;
import com.namnguyen.ecommerce_platform.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static com.namnguyen.ecommerce_platform.testutil.TestDataFactory.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class JwtSecurityIntegrationTest extends BaseSecurityIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private record TokenResponse(String token) {}

    private String loginAndGetToken(String email, String password) throws Exception {
        LoginRequest request = new LoginRequest (
                email,
                password
        );

        MvcResult result = mockMvc.perform(post(LOGIN_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();

        TokenResponse response = objectMapper.readValue(responseBody, TokenResponse.class);

        return response.token();
    }

    private void saveUser(String email, String rawPassword, Role role) {
        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .firstName("test")
                .lastName("user")
                .phoneNumber("1234567891")
                .role(role)
                .build();

        userRepository.save(user);
    }

    private ProductCreateRequest validProductCreateRequest() {
        return new ProductCreateRequest(
                "PS5",
                "Playstation",
                BigDecimal.valueOf(399.99),
                12
        );
    }

    @Test
    void login_withValidCredentials_returnsJwtToken() throws Exception {
        saveUser("test@gmail.com", "test123456789", Role.CUSTOMER);

        LoginRequest request = new LoginRequest (
                "test@gmail.com",
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
        String password = "test123456789";

        saveUser("admin@gmail.com", password, Role.ADMIN);

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
        saveUser("test@gmail.com", "test123456789", Role.CUSTOMER);

        String token = loginAndGetToken("test@gmail.com", "test123456789");

        mockMvc.perform(post(PRODUCT_URI)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validProductCreateRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    void createProduct_whenAdminJwt_returnsCreated() throws Exception {
        saveUser("test@gmail.com", "test123456789", Role.ADMIN);

        String token = loginAndGetToken("test@gmail.com", "test123456789");

        mockMvc.perform(post(PRODUCT_URI)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validProductCreateRequest())))
                .andExpect(status().isCreated());
    }

    @Test
    void createProduct_whenInvalidToken_returnsUnauthorized() throws Exception {
        String rawPassword = "test123456789";

        saveUser("admin@gmail.com", rawPassword, Role.ADMIN);

        String validToken = loginAndGetToken("admin@gmail.com", rawPassword);

        String invalidToken = validToken + "abc";

        mockMvc.perform(post(PRODUCT_URI)
                        .header("Authorization", "Bearer " + invalidToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validProductCreateRequest())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createProduct_whenMissingToken_returnsUnauthorized() throws Exception {
        mockMvc.perform(post(PRODUCT_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validProductCreateRequest())))
                .andExpect(status().isUnauthorized());
    }

}
