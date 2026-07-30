package com.namnguyen.ecommerce_platform.auth.controller;

import com.namnguyen.ecommerce_platform.auth.dto.AuthResponse;
import com.namnguyen.ecommerce_platform.auth.dto.LoginRequest;
import com.namnguyen.ecommerce_platform.auth.dto.RegisterRequest;
import com.namnguyen.ecommerce_platform.auth.service.AuthService;
import com.namnguyen.ecommerce_platform.common.exception.DuplicateResourceException;
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

import static com.namnguyen.ecommerce_platform.testutil.TestMessages.*;
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

    private final static String LOGIN_URI = "/api/auth/login";
    private final static String REGISTER_URI = "/api/auth/register";
    private final static String VALID_EMAIL = "test@gmail.com";
    private final static String INVALID_EMAIL = "testgmail.com";
    private final static String VALID_PASSWORD = "test1237";
    private final static String VALID_FIRST_NAME = "test";
    private final static String VALID_LAST_NAME = "user";
    private final static String VALID_PHONE_NUMBER = "1234567891";

    @Test
    void login_whenValidRequest_returnsAuthResponse() throws Exception{
        LoginRequest request = new LoginRequest(
                VALID_EMAIL,
                VALID_PASSWORD
        );

        AuthResponse response = new AuthResponse("fake-jwt-token");

        when(authService.login(any(LoginRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post(LOGIN_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("fake-jwt-token"));

        ArgumentCaptor<LoginRequest> captor = ArgumentCaptor.forClass(LoginRequest.class);
        verify(authService).login(captor.capture());

        LoginRequest captureRequest = captor.getValue();

        assertThat(captureRequest.email()).isEqualTo(request.email());
        assertThat(captureRequest.password()).isEqualTo(request.password());

        verifyNoMoreInteractions(authService);
    }

    @Test
    void login_whenInvalidCredentials_returnsUnauthorized() throws Exception {
        LoginRequest request = new LoginRequest(
                VALID_EMAIL,
                "wrongpassword"
        );

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new BadCredentialsException(badCredentials()));

        mockMvc.perform(post(LOGIN_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.UNAUTHORIZED.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.UNAUTHORIZED.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(badCredentials()))
                .andExpect(jsonPath("$.uri").value(LOGIN_URI));

        ArgumentCaptor<LoginRequest> captor = ArgumentCaptor.forClass(LoginRequest.class);
        verify(authService).login(captor.capture());

        LoginRequest captureRequest = captor.getValue();

        assertThat(captureRequest.email()).isEqualTo(request.email());
        assertThat(captureRequest.password()).isEqualTo(request.password());

        verifyNoMoreInteractions(authService);
    }

    @Test
    void login_whenInvalidEmail_returnsBadRequest() throws Exception {
        LoginRequest request = new LoginRequest(
                INVALID_EMAIL,
                VALID_PASSWORD
        );

        mockMvc.perform(post(LOGIN_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.uri").value(LOGIN_URI))
                .andExpect(jsonPath("$.fieldErrors.email", containsInAnyOrder(emailIsInvalid())));

        verifyNoInteractions(authService);
    }

    @Test
    void login_whenEmailIsBlank_returnsBadRequest() throws Exception {
        LoginRequest request = new LoginRequest(
                "",
                VALID_PASSWORD
        );

        mockMvc.perform(post(LOGIN_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.uri").value(LOGIN_URI))
                .andExpect(jsonPath("$.fieldErrors.email", containsInAnyOrder(emailIsRequired())));

        verifyNoInteractions(authService);
    }

    @Test
    void login_whenEmailIsNull_returnsBadRequest() throws Exception {
        LoginRequest request = new LoginRequest(
                null,
                VALID_PASSWORD
        );

        mockMvc.perform(post(LOGIN_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.uri").value(LOGIN_URI))
                .andExpect(jsonPath("$.fieldErrors.email", containsInAnyOrder(emailIsRequired())));

        verifyNoInteractions(authService);
    }

    @Test
    void login_whenPasswordIsLessThan8_returnsBadRequest() throws Exception {
        LoginRequest request = new LoginRequest(
                VALID_EMAIL,
                "test123"
        );

        mockMvc.perform(post(LOGIN_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.uri").value(LOGIN_URI))
                .andExpect(jsonPath("$.fieldErrors.password", containsInAnyOrder(passwordIsInvalid())));


        verifyNoInteractions(authService);
    }

    @Test
    void login_whenPasswordIsMoreThan50_returnsBadRequest() throws Exception {
        LoginRequest request = new LoginRequest(
                VALID_EMAIL,
                "test1235645646467879461313131313456464as1d313a1sd31"
        );

        mockMvc.perform(post(LOGIN_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.uri").value(LOGIN_URI))
                .andExpect(jsonPath("$.fieldErrors.password", containsInAnyOrder(passwordIsInvalid())));

        verifyNoInteractions(authService);
    }

    @Test
    void login_whenPasswordIsBlank_returnsBadRequest() throws Exception {
        LoginRequest request = new LoginRequest(
                VALID_EMAIL,
                ""
        );

        mockMvc.perform(post(LOGIN_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.uri").value(LOGIN_URI))
                .andExpect(jsonPath("$.fieldErrors.password").isArray())
                .andExpect(jsonPath("$.fieldErrors.password", containsInAnyOrder(
                        passwordIsInvalid(),
                        passwordIsRequired())));

        verifyNoInteractions(authService);
    }

    @Test
    void login_whenPasswordIsNull_returnsBadRequest() throws Exception {
        LoginRequest request = new LoginRequest(
                VALID_EMAIL,
                null
        );

        mockMvc.perform(post(LOGIN_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.uri").value(LOGIN_URI))
                .andExpect(jsonPath("$.fieldErrors.password", containsInAnyOrder(passwordIsRequired())));

        verifyNoInteractions(authService);
    }

    @Test
    void register_whenValidRequest_returnsAuthResponse() throws Exception{
        RegisterRequest request = new RegisterRequest(
                VALID_EMAIL,
                VALID_PASSWORD,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                VALID_PHONE_NUMBER
        );

        AuthResponse response = new AuthResponse("fake-jwt-token");

        when(authService.register(any(RegisterRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post(REGISTER_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("fake-jwt-token"));

        ArgumentCaptor<RegisterRequest> captor = ArgumentCaptor.forClass(RegisterRequest.class);
        verify(authService).register(captor.capture());

        RegisterRequest captorRequest = captor.getValue();

        assertThat(captorRequest.email()).isEqualTo(request.email());
        assertThat(captorRequest.password()).isEqualTo(request.password());
        assertThat(captorRequest.firstName()).isEqualTo(request.firstName());
        assertThat(captorRequest.lastName()).isEqualTo(request.lastName());
        assertThat(captorRequest.phoneNumber()).isEqualTo(request.phoneNumber());

        verifyNoMoreInteractions(authService);
    }

    @Test
    void register_whenPhoneNumberHasPlus_returnsAuthResponse() throws Exception{
        RegisterRequest request = new RegisterRequest(
                VALID_EMAIL,
                VALID_PASSWORD,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                "+1234567891"
        );

        AuthResponse response = new AuthResponse("fake-jwt-token");

        when(authService.register(any(RegisterRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post(REGISTER_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("fake-jwt-token"));

        ArgumentCaptor<RegisterRequest> captor = ArgumentCaptor.forClass(RegisterRequest.class);
        verify(authService).register(captor.capture());

        RegisterRequest captorRequest = captor.getValue();

        assertThat(captorRequest.email()).isEqualTo(request.email());
        assertThat(captorRequest.password()).isEqualTo(request.password());
        assertThat(captorRequest.firstName()).isEqualTo(request.firstName());
        assertThat(captorRequest.lastName()).isEqualTo(request.lastName());
        assertThat(captorRequest.phoneNumber()).isEqualTo(request.phoneNumber());

        verifyNoMoreInteractions(authService);
    }

    @Test
    void register_whenEmailIsNull_returnsBadRequest() throws Exception{
        RegisterRequest request = new RegisterRequest(
                null,
                VALID_PASSWORD,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                VALID_PHONE_NUMBER
        );

        mockMvc.perform(post(REGISTER_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.uri").value(REGISTER_URI))
                .andExpect(jsonPath("$.fieldErrors.email", containsInAnyOrder(emailIsRequired())));

        verifyNoInteractions(authService);
    }

    @Test
    void register_whenEmailIsBlank_returnsBadRequest() throws Exception{
        RegisterRequest request = new RegisterRequest(
                "",
                VALID_PASSWORD,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                VALID_PHONE_NUMBER
        );

        mockMvc.perform(post(REGISTER_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.uri").value(REGISTER_URI))
                .andExpect(jsonPath("$.fieldErrors.email", containsInAnyOrder(emailIsRequired())));

        verifyNoInteractions(authService);
    }

    @Test
    void register_whenEmailIsInvalid_returnsBadRequest() throws Exception{
        RegisterRequest request = new RegisterRequest(
                INVALID_EMAIL,
                VALID_PASSWORD,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                VALID_PHONE_NUMBER
        );

        mockMvc.perform(post(REGISTER_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.uri").value(REGISTER_URI))
                .andExpect(jsonPath("$.fieldErrors.email", containsInAnyOrder(emailIsInvalid())));

        verifyNoInteractions(authService);
    }

    @Test
    void register_whenPasswordIsNull_returnsBadRequest() throws Exception{
        RegisterRequest request = new RegisterRequest(
                VALID_EMAIL,
                null,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                VALID_PHONE_NUMBER
        );

        mockMvc.perform(post(REGISTER_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.uri").value(REGISTER_URI))
                .andExpect(jsonPath("$.fieldErrors.password", containsInAnyOrder(passwordIsRequired())));

        verifyNoInteractions(authService);
    }

    @Test
    void register_whenPasswordIsBlank_returnsBadRequest() throws Exception{
        RegisterRequest request = new RegisterRequest(
                VALID_EMAIL,
                "",
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                VALID_PHONE_NUMBER
        );

        mockMvc.perform(post(REGISTER_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.uri").value(REGISTER_URI))
                .andExpect(jsonPath("$.fieldErrors.password", containsInAnyOrder(
                        passwordIsRequired(),
                        passwordIsInvalid())));

        verifyNoInteractions(authService);
    }

    @Test
    void register_whenPasswordIsLessThan8_returnsBadRequest() throws Exception{
        RegisterRequest request = new RegisterRequest(
                VALID_EMAIL,
                "test123",
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                VALID_PHONE_NUMBER
        );

        mockMvc.perform(post(REGISTER_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.uri").value(REGISTER_URI))
                .andExpect(jsonPath("$.fieldErrors.password", containsInAnyOrder(passwordIsInvalid())));

        verifyNoInteractions(authService);
    }

    @Test
    void register_whenPasswordIsMoreThan50_returnsBadRequest() throws Exception{
        RegisterRequest request = new RegisterRequest(
                VALID_EMAIL,
                "test1235645646467879461313131313456464as1d313a1sd31",
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                VALID_PHONE_NUMBER
        );

        mockMvc.perform(post(REGISTER_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.uri").value(REGISTER_URI))
                .andExpect(jsonPath("$.fieldErrors.password", containsInAnyOrder(passwordIsInvalid())));

        verifyNoInteractions(authService);
    }

    @Test
    void register_whenFirstNameIsBlank_returnsBadRequest() throws Exception{
        RegisterRequest request = new RegisterRequest(
                VALID_EMAIL,
                VALID_PASSWORD,
                "",
                VALID_LAST_NAME,
                VALID_PHONE_NUMBER
        );

        mockMvc.perform(post(REGISTER_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.uri").value(REGISTER_URI))
                .andExpect(jsonPath("$.fieldErrors.firstName", containsInAnyOrder(firstNameIsRequired())));

        verifyNoInteractions(authService);
    }

    @Test
    void register_whenFirstNameIsNull_returnsBadRequest() throws Exception{
        RegisterRequest request = new RegisterRequest(
                VALID_EMAIL,
                VALID_PASSWORD,
                null,
                VALID_LAST_NAME,
                VALID_PHONE_NUMBER
        );

        mockMvc.perform(post(REGISTER_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.uri").value(REGISTER_URI))
                .andExpect(jsonPath("$.fieldErrors.firstName", containsInAnyOrder(firstNameIsRequired())));

        verifyNoInteractions(authService);
    }

    @Test
    void register_whenLastNameIsBlank_returnsBadRequest() throws Exception{
        RegisterRequest request = new RegisterRequest(
                VALID_EMAIL,
                VALID_PASSWORD,
                VALID_FIRST_NAME,
                "",
                VALID_PHONE_NUMBER
        );

        mockMvc.perform(post(REGISTER_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.uri").value(REGISTER_URI))
                .andExpect(jsonPath("$.fieldErrors.lastName", containsInAnyOrder(lastNameIsRequired())));

        verifyNoInteractions(authService);
    }

    @Test
    void register_whenLastNameIsNull_returnsBadRequest() throws Exception{
        RegisterRequest request = new RegisterRequest(
                VALID_EMAIL,
                VALID_PASSWORD,
                VALID_FIRST_NAME,
                null,
                VALID_PHONE_NUMBER
        );

        mockMvc.perform(post(REGISTER_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.uri").value(REGISTER_URI))
                .andExpect(jsonPath("$.fieldErrors.lastName", containsInAnyOrder(lastNameIsRequired())));

        verifyNoInteractions(authService);
    }

    @Test
    void register_whenPhoneNumberIsBlank_returnsBadRequest() throws Exception{
        RegisterRequest request = new RegisterRequest(
                VALID_EMAIL,
                VALID_PASSWORD,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                ""
        );

        mockMvc.perform(post(REGISTER_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.uri").value(REGISTER_URI))
                .andExpect(jsonPath("$.fieldErrors.phoneNumber", containsInAnyOrder(
                        phoneNumberIsRequired(),
                        phoneNumberIsInvalid())));

        verifyNoInteractions(authService);
    }

    @Test
    void register_whenPhoneNumberIsNull_returnsBadRequest() throws Exception{
        RegisterRequest request = new RegisterRequest(
                VALID_EMAIL,
                VALID_PASSWORD,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                null
        );

        mockMvc.perform(post(REGISTER_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.uri").value(REGISTER_URI))
                .andExpect(jsonPath("$.fieldErrors.phoneNumber", containsInAnyOrder(phoneNumberIsRequired())));

        verifyNoInteractions(authService);
    }

    @Test
    void register_whenPhoneNumberHasLessThan10_returnsBadRequest() throws Exception{
        RegisterRequest request = new RegisterRequest(
                VALID_EMAIL,
                VALID_PASSWORD,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                "123456789"
        );

        mockMvc.perform(post(REGISTER_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.uri").value(REGISTER_URI))
                .andExpect(jsonPath("$.fieldErrors.phoneNumber", containsInAnyOrder(phoneNumberIsInvalid())));

        verifyNoInteractions(authService);
    }

    @Test
    void register_whenPhoneNumberHasMoreThan15_returnsBadRequest() throws Exception{
        RegisterRequest request = new RegisterRequest(
                VALID_EMAIL,
                VALID_PASSWORD,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                "1234567891234567"
        );

        mockMvc.perform(post(REGISTER_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.uri").value(REGISTER_URI))
                .andExpect(jsonPath("$.fieldErrors.phoneNumber", containsInAnyOrder(phoneNumberIsInvalid())));

        verifyNoInteractions(authService);
    }

    @Test
    void register_whenPhoneNumberHasInvalidSymbol_returnsBadRequest() throws Exception{
        RegisterRequest request = new RegisterRequest(
                VALID_EMAIL,
                VALID_PASSWORD,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                "-1234567891"
        );

        mockMvc.perform(post(REGISTER_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.uri").value(REGISTER_URI))
                .andExpect(jsonPath("$.fieldErrors.phoneNumber", containsInAnyOrder(phoneNumberIsInvalid())));

        verifyNoInteractions(authService);
    }

    @Test
    void register_whenEmailAlreadyExists_returnsConflict() throws Exception{
        RegisterRequest request = new RegisterRequest(
                VALID_EMAIL,
                VALID_PASSWORD,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                VALID_PHONE_NUMBER
        );

        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new DuplicateResourceException(emailDuplicate()));

        mockMvc.perform(post(REGISTER_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.CONFLICT.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.CONFLICT.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(emailDuplicate()))
                .andExpect(jsonPath("$.uri").value(REGISTER_URI));

        verify(authService).register(any(RegisterRequest.class));
        verifyNoMoreInteractions(authService);
    }

    @Test
    void register_whenPhoneNumberAlreadyExists_returnsConflict() throws Exception{
        RegisterRequest request = new RegisterRequest(
                VALID_EMAIL,
                VALID_PASSWORD,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                VALID_PHONE_NUMBER
        );

        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new DuplicateResourceException(phoneDuplicate()));

        mockMvc.perform(post(REGISTER_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.CONFLICT.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.CONFLICT.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(phoneDuplicate()))
                .andExpect(jsonPath("$.uri").value(REGISTER_URI));

        verify(authService).register(any(RegisterRequest.class));
        verifyNoMoreInteractions(authService);
    }
}
