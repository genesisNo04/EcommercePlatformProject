package com.namnguyen.ecommerce_platform.integration.auth;

import com.namnguyen.ecommerce_platform.auth.dto.LoginRequest;
import com.namnguyen.ecommerce_platform.auth.dto.RegisterRequest;
import com.namnguyen.ecommerce_platform.integration.BaseIntegrationTest;
import com.namnguyen.ecommerce_platform.user.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static com.namnguyen.ecommerce_platform.testutil.TestDataFactory.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class AuthIntegrationTest extends BaseIntegrationTest {

    @Test
    void registerUser_withValidRequest_createsUserInDatabase() throws Exception {
        RegisterRequest request = createDefaultRegisterRequest();

        mockMvc.perform(post(REGISTER_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").exists());

        User savedUser = userRepository.findByEmail(request.email()).orElseThrow();

        assertThat(savedUser.getEmail()).isEqualTo(request.email());
        assertThat(savedUser.getFirstName()).isEqualTo(request.firstName());
        assertThat(savedUser.getLastName()).isEqualTo(request.lastName());
        assertThat(savedUser.getPhoneNumber()).isEqualTo(request.phoneNumber());
        assertThat(savedUser.getRole()).isEqualTo(ROLE_CUSTOMER);
        assertThat(passwordEncoder.matches(request.password(), savedUser.getPasswordHash())).isTrue();
        assertThat(userRepository.count()).isEqualTo(1);
    }

    @Test
    void registerUser_withSameEmail_returnsConflict() throws Exception {
        createUser(
                VALID_EMAIL,
                VALID_PASSWORD,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                VALID_PHONE_NUMBER,
                ROLE_CUSTOMER
        );

        RegisterRequest request = createRegisterRequest(
                VALID_EMAIL,
                VALID_PASSWORD,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                "1234567899"
        );

        mockMvc.perform(post(REGISTER_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.token").doesNotExist());

        assertThat(userRepository.count()).isEqualTo(1);
    }

    @Test
    void registerUser_withSamePhoneNumber_returnsConflict() throws Exception {
        createUser(
                VALID_EMAIL,
                VALID_PASSWORD,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                VALID_PHONE_NUMBER,
                ROLE_CUSTOMER
        );

        RegisterRequest request = createRegisterRequest(
                "test1@gmail.com",
                VALID_PASSWORD,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                VALID_PHONE_NUMBER
        );

        mockMvc.perform(post(REGISTER_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.token").doesNotExist());

        assertThat(userRepository.count()).isEqualTo(1);
    }

    @Test
    void loginUser_withValidUser_returnsToken() throws Exception {
        createDefaultCustomer();

        LoginRequest request = createDefaultLoginRequest();

        mockMvc.perform(post(LOGIN_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());
    }

    @Test
    void loginUser_withIncorrectPassword_returnsUnauthorized() throws Exception {
        createDefaultCustomer();

        LoginRequest request = createLoginRequest(
                "customer@gmail.com",
                "test987654321"
        );

        mockMvc.perform(post(LOGIN_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    @Test
    void loginUser_withoutRequestBody_returnsBadRequest() throws Exception {
        mockMvc.perform(post(LOGIN_URI))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    @Test
    void loginUser_withUnknownEmail_returnsUnauthorized() throws Exception {
        LoginRequest request = createLoginRequest(
                "unknown@gmail.com",
                VALID_PASSWORD
        );

        mockMvc.perform(post(LOGIN_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    @Test
    void loginUser_withMissingEmailAndPassword_returnsBadRequest() throws Exception {
        mockMvc.perform(post(LOGIN_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.token").doesNotExist());
    }
}
