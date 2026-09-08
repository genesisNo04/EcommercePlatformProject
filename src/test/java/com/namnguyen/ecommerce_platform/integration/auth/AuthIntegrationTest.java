package com.namnguyen.ecommerce_platform.integration.auth;

import com.namnguyen.ecommerce_platform.auth.dto.LoginRequest;
import com.namnguyen.ecommerce_platform.auth.dto.RegisterRequest;
import com.namnguyen.ecommerce_platform.integration.BaseIntegrationTest;
import com.namnguyen.ecommerce_platform.user.entity.User;
import com.namnguyen.ecommerce_platform.user.enums.Role;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static com.namnguyen.ecommerce_platform.testutil.TestDataFactory.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class AuthIntegrationTest extends BaseIntegrationTest {

    @Test
    void registerUser_whenRequestIsValid_createsUserInDatabase() throws Exception {
        RegisterRequest registerRequest = createDefaultRegisterRequest();

        mockMvc.perform(post(REGISTER_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").exists());

        User savedUser = userRepository.findByEmail(registerRequest.email()).orElseThrow();

        assertThat(savedUser.getEmail()).isEqualTo(registerRequest.email());
        assertThat(savedUser.getFirstName()).isEqualTo(registerRequest.firstName());
        assertThat(savedUser.getLastName()).isEqualTo(registerRequest.lastName());
        assertThat(savedUser.getPhoneNumber()).isEqualTo(registerRequest.phoneNumber());
        assertThat(savedUser.getRole()).isEqualTo(Role.CUSTOMER);
        assertThat(passwordEncoder.matches(registerRequest.password(), savedUser.getPasswordHash())).isTrue();
        assertThat(userRepository.count()).isEqualTo(1);
    }

    @Test
    void registerUser_whenEmailAlreadyExists_returnsConflict() throws Exception {
        persistDefaultCustomer();

        RegisterRequest registerRequest = createRegisterRequest(
                VALID_EMAIL,
                VALID_PASSWORD,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                "1234567899"
        );

        mockMvc.perform(post(REGISTER_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.token").doesNotExist());

        assertThat(userRepository.count()).isEqualTo(1);
    }

    @Test
    void registerUser_whenPhoneNumberAlreadyExists_returnsConflict() throws Exception {
        persistDefaultCustomer();

        RegisterRequest registerRequest = createRegisterRequest(
                "secondemail@gmail.com",
                VALID_PASSWORD,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                VALID_PHONE_NUMBER
        );

        mockMvc.perform(post(REGISTER_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.token").doesNotExist());

        assertThat(userRepository.count()).isEqualTo(1);
    }

    @Test
    void loginUser_whenCredentialsAreValid_returnsToken() throws Exception {
        persistDefaultCustomer();

        LoginRequest loginRequest = createDefaultLoginRequest();

        mockMvc.perform(post(LOGIN_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());
    }

    @Test
    void loginUser_whenPasswordIsIncorrect_returnsUnauthorized() throws Exception {
        persistDefaultCustomer();

        LoginRequest loginRequest = createLoginRequest(
                VALID_EMAIL,
                WRONG_PASSWORD
        );

        mockMvc.perform(post(LOGIN_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    @Test
    void loginUser_whenRequestBodyIsMissing_returnsBadRequest() throws Exception {
        mockMvc.perform(post(LOGIN_URI))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    @Test
    void loginUser_whenEmailDoesNotExist_returnsUnauthorized() throws Exception {
        LoginRequest loginRequest = createLoginRequest(
                "unknown@gmail.com",
                VALID_PASSWORD
        );

        mockMvc.perform(post(LOGIN_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    @Test
    void loginUser_whenCredentialsAreMissing_returnsBadRequest() throws Exception {
        mockMvc.perform(post(LOGIN_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.token").doesNotExist());
    }
}
