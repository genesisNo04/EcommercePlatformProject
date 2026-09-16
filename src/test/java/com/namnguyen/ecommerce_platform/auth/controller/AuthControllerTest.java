package com.namnguyen.ecommerce_platform.auth.controller;

import com.namnguyen.ecommerce_platform.auth.dto.AuthResponse;
import com.namnguyen.ecommerce_platform.auth.dto.LoginRequest;
import com.namnguyen.ecommerce_platform.auth.dto.RegisterRequest;
import com.namnguyen.ecommerce_platform.auth.service.AuthService;
import com.namnguyen.ecommerce_platform.common.exception.DuplicateResourceException;
import com.namnguyen.ecommerce_platform.common.rate_limit.RateLimitService;
import com.namnguyen.ecommerce_platform.security.jwt.JwtService;
import com.namnguyen.ecommerce_platform.security.user.CustomUserDetailsService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static com.namnguyen.ecommerce_platform.testutil.TestDataFactory.*;
import static com.namnguyen.ecommerce_platform.testutil.messages.AuthTestMessages.*;
import static com.namnguyen.ecommerce_platform.testutil.messages.CommonTestMessages.VALIDATION_FAILED;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.containsInAnyOrder;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private RateLimitService rateLimitService;

    @Test
    void login_whenValidRequest_returnsAuthResponse() throws Exception {
        LoginRequest loginRequest = new LoginRequest(
                VALID_EMAIL,
                VALID_PASSWORD
        );

        AuthResponse authResponse = new AuthResponse(MOCK_JWT_TOKEN);

        when(authService.login(any(LoginRequest.class)))
                .thenReturn(authResponse);

        mockMvc.perform(post(LOGIN_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value(MOCK_JWT_TOKEN));

        ArgumentCaptor<LoginRequest> captor = ArgumentCaptor.forClass(LoginRequest.class);
        verify(authService).login(captor.capture());

        LoginRequest capturedLoginRequest = captor.getValue();

        assertThat(capturedLoginRequest.email()).isEqualTo(loginRequest.email());
        assertThat(capturedLoginRequest.password()).isEqualTo(loginRequest.password());

        verifyNoMoreInteractions(authService);
    }

    @Test
    void login_whenInvalidCredentials_returnsUnauthorized() throws Exception {
        LoginRequest loginRequest = new LoginRequest(
                VALID_EMAIL,
                WRONG_PASSWORD
        );

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new BadCredentialsException(BAD_CREDENTIALS));

        mockMvc.perform(post(LOGIN_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.UNAUTHORIZED.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.UNAUTHORIZED.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(INVALID_CREDENTIALS))
                .andExpect(jsonPath("$.uri").value(LOGIN_URI));

        ArgumentCaptor<LoginRequest> captor = ArgumentCaptor.forClass(LoginRequest.class);
        verify(authService).login(captor.capture());

        LoginRequest capturedLoginRequest = captor.getValue();

        assertThat(capturedLoginRequest.email()).isEqualTo(loginRequest.email());
        assertThat(capturedLoginRequest.password()).isEqualTo(loginRequest.password());

        verifyNoMoreInteractions(authService);
    }

    @Test
    void login_whenInvalidEmail_returnsBadRequest() throws Exception {
        LoginRequest loginRequest = new LoginRequest(
                INVALID_EMAIL,
                VALID_PASSWORD
        );

        mockMvc.perform(post(LOGIN_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                .andExpect(jsonPath("$.uri").value(LOGIN_URI))
                .andExpect(jsonPath("$.fieldErrors.email", containsInAnyOrder(AUTH_EMAIL_IS_INVALID)));

        verifyNoInteractions(authService);
    }

    @Test
    void login_whenEmailIsBlank_returnsBadRequest() throws Exception {
        LoginRequest loginRequest = new LoginRequest(
                "",
                VALID_PASSWORD
        );

        mockMvc.perform(post(LOGIN_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                .andExpect(jsonPath("$.uri").value(LOGIN_URI))
                .andExpect(jsonPath("$.fieldErrors.email", containsInAnyOrder(AUTH_EMAIL_IS_REQUIRED)));

        verifyNoInteractions(authService);
    }

    @Test
    void login_whenEmailIsNull_returnsBadRequest() throws Exception {
        LoginRequest loginRequest = new LoginRequest(
                null,
                VALID_PASSWORD
        );

        mockMvc.perform(post(LOGIN_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                .andExpect(jsonPath("$.uri").value(LOGIN_URI))
                .andExpect(jsonPath("$.fieldErrors.email", containsInAnyOrder(AUTH_EMAIL_IS_REQUIRED)));

        verifyNoInteractions(authService);
    }

    @Test
    void login_whenPasswordIsBlank_returnsBadRequest() throws Exception {
        LoginRequest loginRequest = new LoginRequest(
                VALID_EMAIL,
                ""
        );

        mockMvc.perform(post(LOGIN_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                .andExpect(jsonPath("$.uri").value(LOGIN_URI))
                .andExpect(jsonPath("$.fieldErrors.password").isArray())
                .andExpect(jsonPath("$.fieldErrors.password", containsInAnyOrder(
                        AUTH_PASSWORD_IS_REQUIRED)));

        verifyNoInteractions(authService);
    }

    @Test
    void login_whenPasswordIsNull_returnsBadRequest() throws Exception {
        LoginRequest loginRequest = new LoginRequest(
                VALID_EMAIL,
                null
        );

        mockMvc.perform(post(LOGIN_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                .andExpect(jsonPath("$.uri").value(LOGIN_URI))
                .andExpect(jsonPath("$.fieldErrors.password", containsInAnyOrder(AUTH_PASSWORD_IS_REQUIRED)));

        verifyNoInteractions(authService);
    }

    @Test
    void register_whenValidRequest_returnsAuthResponse() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest(
                VALID_EMAIL,
                VALID_PASSWORD,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                VALID_PHONE_NUMBER
        );

        AuthResponse authResponse = new AuthResponse(MOCK_JWT_TOKEN);

        when(authService.register(any(RegisterRequest.class)))
                .thenReturn(authResponse);

        mockMvc.perform(post(REGISTER_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value(MOCK_JWT_TOKEN));

        ArgumentCaptor<RegisterRequest> captor = ArgumentCaptor.forClass(RegisterRequest.class);
        verify(authService).register(captor.capture());

        RegisterRequest capturedRegisterRequest = captor.getValue();

        assertThat(capturedRegisterRequest.email()).isEqualTo(registerRequest.email());
        assertThat(capturedRegisterRequest.password()).isEqualTo(registerRequest.password());
        assertThat(capturedRegisterRequest.firstName()).isEqualTo(registerRequest.firstName());
        assertThat(capturedRegisterRequest.lastName()).isEqualTo(registerRequest.lastName());
        assertThat(capturedRegisterRequest.phoneNumber()).isEqualTo(registerRequest.phoneNumber());

        verifyNoMoreInteractions(authService);
    }

    @Test
    void register_whenPhoneNumberHasPlus_returnsAuthResponse() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest(
                VALID_EMAIL,
                VALID_PASSWORD,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                VALID_PHONE_NUMBER_WITH_PLUS
        );

        AuthResponse authResponse = new AuthResponse(MOCK_JWT_TOKEN);

        when(authService.register(any(RegisterRequest.class)))
                .thenReturn(authResponse);

        mockMvc.perform(post(REGISTER_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value(MOCK_JWT_TOKEN));

        ArgumentCaptor<RegisterRequest> captor = ArgumentCaptor.forClass(RegisterRequest.class);
        verify(authService).register(captor.capture());

        RegisterRequest capturedRegisterRequest = captor.getValue();

        assertThat(capturedRegisterRequest.email()).isEqualTo(registerRequest.email());
        assertThat(capturedRegisterRequest.password()).isEqualTo(registerRequest.password());
        assertThat(capturedRegisterRequest.firstName()).isEqualTo(registerRequest.firstName());
        assertThat(capturedRegisterRequest.lastName()).isEqualTo(registerRequest.lastName());
        assertThat(capturedRegisterRequest.phoneNumber()).isEqualTo(registerRequest.phoneNumber());

        verifyNoMoreInteractions(authService);
    }

    @Test
    void register_whenEmailIsNull_returnsBadRequest() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest(
                null,
                VALID_PASSWORD,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                VALID_PHONE_NUMBER
        );

        mockMvc.perform(post(REGISTER_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                .andExpect(jsonPath("$.uri").value(REGISTER_URI))
                .andExpect(jsonPath("$.fieldErrors.email", containsInAnyOrder(AUTH_EMAIL_IS_REQUIRED)));

        verifyNoInteractions(authService);
    }

    @Test
    void register_whenEmailIsBlank_returnsBadRequest() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest(
                "",
                VALID_PASSWORD,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                VALID_PHONE_NUMBER
        );

        mockMvc.perform(post(REGISTER_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                .andExpect(jsonPath("$.uri").value(REGISTER_URI))
                .andExpect(jsonPath("$.fieldErrors.email", containsInAnyOrder(AUTH_EMAIL_IS_REQUIRED)));

        verifyNoInteractions(authService);
    }

    @Test
    void register_whenEmailIsInvalid_returnsBadRequest() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest(
                INVALID_EMAIL,
                VALID_PASSWORD,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                VALID_PHONE_NUMBER
        );

        mockMvc.perform(post(REGISTER_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                .andExpect(jsonPath("$.uri").value(REGISTER_URI))
                .andExpect(jsonPath("$.fieldErrors.email", containsInAnyOrder(AUTH_EMAIL_IS_INVALID)));

        verifyNoInteractions(authService);
    }

    @Test
    void register_whenPasswordIsNull_returnsBadRequest() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest(
                VALID_EMAIL,
                null,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                VALID_PHONE_NUMBER
        );

        mockMvc.perform(post(REGISTER_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                .andExpect(jsonPath("$.uri").value(REGISTER_URI))
                .andExpect(jsonPath("$.fieldErrors.password", containsInAnyOrder(AUTH_PASSWORD_IS_REQUIRED)));

        verifyNoInteractions(authService);
    }

    @Test
    void register_whenPasswordIsBlank_returnsBadRequest() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest(
                VALID_EMAIL,
                "",
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                VALID_PHONE_NUMBER
        );

        mockMvc.perform(post(REGISTER_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                .andExpect(jsonPath("$.uri").value(REGISTER_URI))
                .andExpect(jsonPath("$.fieldErrors.password", containsInAnyOrder(
                        AUTH_PASSWORD_IS_REQUIRED,
                        AUTH_PASSWORD_IS_INVALID)));

        verifyNoInteractions(authService);
    }

    @Test
    void register_whenPasswordIsLessThan8_returnsBadRequest() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest(
                VALID_EMAIL,
                INVALID_PASSWORD_LESS_THAN_EIGHT,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                VALID_PHONE_NUMBER
        );

        mockMvc.perform(post(REGISTER_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                .andExpect(jsonPath("$.uri").value(REGISTER_URI))
                .andExpect(jsonPath("$.fieldErrors.password", containsInAnyOrder(AUTH_PASSWORD_IS_INVALID)));

        verifyNoInteractions(authService);
    }

    @Test
    void register_whenPasswordIsMoreThan50_returnsBadRequest() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest(
                VALID_EMAIL,
                INVALID_PASSWORD_MORE_THAN_FIFTY,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                VALID_PHONE_NUMBER
        );

        mockMvc.perform(post(REGISTER_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                .andExpect(jsonPath("$.uri").value(REGISTER_URI))
                .andExpect(jsonPath("$.fieldErrors.password", containsInAnyOrder(AUTH_PASSWORD_IS_INVALID)));

        verifyNoInteractions(authService);
    }

    @Test
    void register_whenFirstNameIsBlank_returnsBadRequest() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest(
                VALID_EMAIL,
                VALID_PASSWORD,
                "",
                VALID_LAST_NAME,
                VALID_PHONE_NUMBER
        );

        mockMvc.perform(post(REGISTER_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                .andExpect(jsonPath("$.uri").value(REGISTER_URI))
                .andExpect(jsonPath("$.fieldErrors.firstName", containsInAnyOrder(AUTH_FIRST_NAME_IS_REQUIRED)));

        verifyNoInteractions(authService);
    }

    @Test
    void register_whenFirstNameIsNull_returnsBadRequest() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest(
                VALID_EMAIL,
                VALID_PASSWORD,
                null,
                VALID_LAST_NAME,
                VALID_PHONE_NUMBER
        );

        mockMvc.perform(post(REGISTER_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                .andExpect(jsonPath("$.uri").value(REGISTER_URI))
                .andExpect(jsonPath("$.fieldErrors.firstName", containsInAnyOrder(AUTH_FIRST_NAME_IS_REQUIRED)));

        verifyNoInteractions(authService);
    }

    @Test
    void register_whenLastNameIsBlank_returnsBadRequest() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest(
                VALID_EMAIL,
                VALID_PASSWORD,
                VALID_FIRST_NAME,
                "",
                VALID_PHONE_NUMBER
        );

        mockMvc.perform(post(REGISTER_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                .andExpect(jsonPath("$.uri").value(REGISTER_URI))
                .andExpect(jsonPath("$.fieldErrors.lastName", containsInAnyOrder(AUTH_LAST_NAME_IS_REQUIRED)));

        verifyNoInteractions(authService);
    }

    @Test
    void register_whenLastNameIsNull_returnsBadRequest() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest(
                VALID_EMAIL,
                VALID_PASSWORD,
                VALID_FIRST_NAME,
                null,
                VALID_PHONE_NUMBER
        );

        mockMvc.perform(post(REGISTER_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                .andExpect(jsonPath("$.uri").value(REGISTER_URI))
                .andExpect(jsonPath("$.fieldErrors.lastName", containsInAnyOrder(AUTH_LAST_NAME_IS_REQUIRED)));

        verifyNoInteractions(authService);
    }

    @Test
    void register_whenPhoneNumberIsBlank_returnsBadRequest() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest(
                VALID_EMAIL,
                VALID_PASSWORD,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                ""
        );

        mockMvc.perform(post(REGISTER_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                .andExpect(jsonPath("$.uri").value(REGISTER_URI))
                .andExpect(jsonPath("$.fieldErrors.phoneNumber", containsInAnyOrder(
                        AUTH_PHONE_NUMBER_IS_REQUIRED,
                        AUTH_PHONE_NUMBER_IS_INVALID)));

        verifyNoInteractions(authService);
    }

    @Test
    void register_whenPhoneNumberIsNull_returnsBadRequest() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest(
                VALID_EMAIL,
                VALID_PASSWORD,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                null
        );

        mockMvc.perform(post(REGISTER_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                .andExpect(jsonPath("$.uri").value(REGISTER_URI))
                .andExpect(jsonPath("$.fieldErrors.phoneNumber", containsInAnyOrder(AUTH_PHONE_NUMBER_IS_REQUIRED)));

        verifyNoInteractions(authService);
    }

    @Test
    void register_whenPhoneNumberHasLessThan10_returnsBadRequest() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest(
                VALID_EMAIL,
                VALID_PASSWORD,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                INVALID_PHONE_NUMBER_LESS_THAN_TEN
        );

        mockMvc.perform(post(REGISTER_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                .andExpect(jsonPath("$.uri").value(REGISTER_URI))
                .andExpect(jsonPath("$.fieldErrors.phoneNumber", containsInAnyOrder(AUTH_PHONE_NUMBER_IS_INVALID)));

        verifyNoInteractions(authService);
    }

    @Test
    void register_whenPhoneNumberHasMoreThan15_returnsBadRequest() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest(
                VALID_EMAIL,
                VALID_PASSWORD,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                INVALID_PHONE_NUMBER_MORE_THAN_FIFTEEN
        );

        mockMvc.perform(post(REGISTER_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                .andExpect(jsonPath("$.uri").value(REGISTER_URI))
                .andExpect(jsonPath("$.fieldErrors.phoneNumber", containsInAnyOrder(AUTH_PHONE_NUMBER_IS_INVALID)));

        verifyNoInteractions(authService);
    }

    @Test
    void register_whenPhoneNumberHasInvalidSymbol_returnsBadRequest() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest(
                VALID_EMAIL,
                VALID_PASSWORD,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                INVALID_PHONE_NUMBER_WITH_MINUS
        );

        mockMvc.perform(post(REGISTER_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                .andExpect(jsonPath("$.uri").value(REGISTER_URI))
                .andExpect(jsonPath("$.fieldErrors.phoneNumber", containsInAnyOrder(AUTH_PHONE_NUMBER_IS_INVALID)));

        verifyNoInteractions(authService);
    }

    @Test
    void register_whenEmailAlreadyExists_returnsConflict() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest(
                VALID_EMAIL,
                VALID_PASSWORD,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                VALID_PHONE_NUMBER
        );

        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new DuplicateResourceException(DUPLICATE_EMAIL));

        mockMvc.perform(post(REGISTER_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.CONFLICT.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.CONFLICT.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(DUPLICATE_EMAIL))
                .andExpect(jsonPath("$.uri").value(REGISTER_URI));

        verify(authService).register(any(RegisterRequest.class));
        verifyNoMoreInteractions(authService);
    }

    @Test
    void register_whenPhoneNumberAlreadyExists_returnsConflict() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest(
                VALID_EMAIL,
                VALID_PASSWORD,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                VALID_PHONE_NUMBER
        );

        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new DuplicateResourceException(DUPLICATE_PHONE));

        mockMvc.perform(post(REGISTER_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.CONFLICT.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.CONFLICT.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(DUPLICATE_PHONE))
                .andExpect(jsonPath("$.uri").value(REGISTER_URI));

        verify(authService).register(any(RegisterRequest.class));
        verifyNoMoreInteractions(authService);
    }
}
