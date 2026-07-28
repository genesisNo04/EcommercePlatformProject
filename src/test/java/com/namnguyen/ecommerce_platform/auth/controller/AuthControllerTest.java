package com.namnguyen.ecommerce_platform.auth.controller;

import com.namnguyen.ecommerce_platform.auth.dto.AuthResponse;
import com.namnguyen.ecommerce_platform.auth.dto.LoginRequest;
import com.namnguyen.ecommerce_platform.auth.dto.RegisterRequest;
import com.namnguyen.ecommerce_platform.auth.service.AuthService;
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
import org.springframework.web.bind.MethodArgumentNotValidException;
import tools.jackson.databind.ObjectMapper;

import static com.namnguyen.ecommerce_platform.testutil.TestMessages.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
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

    @Test
    void login_whenValidRequest_returnsAuthResponse() throws Exception{
        LoginRequest request = new LoginRequest(
                "test@gmail.com",
                "test1237"
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
    void login_whenInvalidCredentials_throwsBadCredentialsException() throws Exception {
        LoginRequest request = new LoginRequest(
                "test@gmail.com",
                "wrongpassword"
        );

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new BadCredentialsException(badCredentials()));

        mockMvc.perform(post(LOGIN_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.uri").value(LOGIN_URI))
                .andExpect(jsonPath("$.message").value(badCredentials()))
                .andExpect(jsonPath("$.error").value(HttpStatus.UNAUTHORIZED.getReasonPhrase()))
                .andExpect(jsonPath("$.status").value(HttpStatus.UNAUTHORIZED.value()));

        ArgumentCaptor<LoginRequest> captor = ArgumentCaptor.forClass(LoginRequest.class);
        verify(authService).login(captor.capture());

        LoginRequest captureRequest = captor.getValue();

        assertThat(captureRequest.email()).isEqualTo(request.email());
        assertThat(captureRequest.password()).isEqualTo(request.password());

        verifyNoMoreInteractions(authService);
    }

    @Test
    void login_whenInvalidEmail_throwsMethodArgumentNotValidException() throws Exception {
        LoginRequest request = new LoginRequest(
                "testgmail.com",
                "test123"
        );

        mockMvc.perform(post(LOGIN_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.uri").value(LOGIN_URI))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()));

        verifyNoInteractions(authService);
    }

    @Test
    void login_whenEmailIsBlank_throwsMethodArgumentNotValidException() throws Exception {
        LoginRequest request = new LoginRequest(
                "",
                "test123"
        );

        mockMvc.perform(post(LOGIN_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.uri").value(LOGIN_URI))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()));

        verifyNoInteractions(authService);
    }

    @Test
    void login_whenEmailIsNull_throwsMethodArgumentNotValidException() throws Exception {
        LoginRequest request = new LoginRequest(
                null,
                "test123"
        );

        mockMvc.perform(post(LOGIN_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.uri").value(LOGIN_URI))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()));

        verifyNoInteractions(authService);
    }

    @Test
    void login_whenPasswordIsLessThan8_throwsMethodArgumentNotValidException() throws Exception {
        LoginRequest request = new LoginRequest(
                "testgmail.com",
                "test123"
        );

        mockMvc.perform(post(LOGIN_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.uri").value(LOGIN_URI))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()));

        verifyNoInteractions(authService);
    }

    @Test
    void login_whenPasswordIsMoreThan50_throwsMethodArgumentNotValidException() throws Exception {
        LoginRequest request = new LoginRequest(
                "testgmail.com",
                "test1235645646467879461313131313456464as1d313a1sd31"
        );

        mockMvc.perform(post(LOGIN_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.uri").value(LOGIN_URI))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()));

        verifyNoInteractions(authService);
    }

    @Test
    void login_whenPasswordIsBlank_throwsMethodArgumentNotValidException() throws Exception {
        LoginRequest request = new LoginRequest(
                "testgmail.com",
                ""
        );

        mockMvc.perform(post(LOGIN_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.uri").value(LOGIN_URI))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()));

        verifyNoInteractions(authService);
    }

    @Test
    void login_whenPasswordIsNull_throwsMethodArgumentNotValidException() throws Exception {
        LoginRequest request = new LoginRequest(
                "testgmail.com",
                null
        );

        mockMvc.perform(post(LOGIN_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.uri").value(LOGIN_URI))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()));

        verifyNoInteractions(authService);
    }

    @Test
    void register_whenValidRequest_returnAuthResponse() throws Exception{
        RegisterRequest request = new RegisterRequest(
                "test@gmail.com",
                "test1234",
                "test",
                "user",
                "1234567891"
        );

        AuthResponse response = new AuthResponse("fake-jwt-token");

        when(authService.register(any(RegisterRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post(REGISTER_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
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
    void register_whenEmailIsNull_returnAuthResponse() throws Exception{
        RegisterRequest request = new RegisterRequest(
                null,
                "test1234",
                "test",
                "user",
                "1234567891"
        );

        mockMvc.perform(post(REGISTER_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.uri").value(REGISTER_URI));

        verifyNoInteractions(authService);
    }

    @Test
    void register_whenEmailIsBlank_returnAuthResponse() throws Exception{
        RegisterRequest request = new RegisterRequest(
                "",
                "test1234",
                "test",
                "user",
                "1234567891"
        );

        mockMvc.perform(post(REGISTER_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.uri").value(REGISTER_URI));

        verifyNoInteractions(authService);
    }

    @Test
    void register_whenEmailIsInvalid_returnAuthResponse() throws Exception{
        RegisterRequest request = new RegisterRequest(
                "testgmail.com",
                "test1234",
                "test",
                "user",
                "1234567891"
        );

        mockMvc.perform(post(REGISTER_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.uri").value(REGISTER_URI));

        verifyNoInteractions(authService);
    }

    @Test
    void register_whenPasswordIsNull_returnAuthResponse() throws Exception{
        RegisterRequest request = new RegisterRequest(
                "testgmail.com",
                null,
                "test",
                "user",
                "1234567891"
        );

        mockMvc.perform(post(REGISTER_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.uri").value(REGISTER_URI));

        verifyNoInteractions(authService);
    }

    @Test
    void register_whenPasswordIsBlank_returnAuthResponse() throws Exception{
        RegisterRequest request = new RegisterRequest(
                "testgmail.com",
                "",
                "test",
                "user",
                "1234567891"
        );

        mockMvc.perform(post(REGISTER_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.uri").value(REGISTER_URI));

        verifyNoInteractions(authService);
    }

    @Test
    void register_whenPasswordIsLessThan8_returnAuthResponse() throws Exception{
        RegisterRequest request = new RegisterRequest(
                "testgmail.com",
                "test123",
                "test",
                "user",
                "1234567891"
        );

        mockMvc.perform(post(REGISTER_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.uri").value(REGISTER_URI));

        verifyNoInteractions(authService);
    }

    @Test
    void register_whenPasswordIsMoreThan50_returnAuthResponse() throws Exception{
        RegisterRequest request = new RegisterRequest(
                "testgmail.com",
                "test1235645646467879461313131313456464as1d313a1sd31",
                "test",
                "user",
                "1234567891"
        );

        mockMvc.perform(post(REGISTER_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.uri").value(REGISTER_URI));

        verifyNoInteractions(authService);
    }

    @Test
    void register_whenFirstNameIsBlank_returnAuthResponse() throws Exception{
        RegisterRequest request = new RegisterRequest(
                "testgmail.com",
                "test1234",
                "",
                "user",
                "1234567891"
        );

        mockMvc.perform(post(REGISTER_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.uri").value(REGISTER_URI));

        verifyNoInteractions(authService);
    }

    @Test
    void register_whenFirstNameIsNull_returnAuthResponse() throws Exception{
        RegisterRequest request = new RegisterRequest(
                "testgmail.com",
                "test1234",
                null,
                "user",
                "1234567891"
        );

        mockMvc.perform(post(REGISTER_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.uri").value(REGISTER_URI));

        verifyNoInteractions(authService);
    }

    @Test
    void register_whenLastNameIsBlank_returnAuthResponse() throws Exception{
        RegisterRequest request = new RegisterRequest(
                "testgmail.com",
                "test1234",
                "test",
                "",
                "1234567891"
        );

        mockMvc.perform(post(REGISTER_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.uri").value(REGISTER_URI));

        verifyNoInteractions(authService);
    }

    @Test
    void register_whenLastNameIsNull_returnAuthResponse() throws Exception{
        RegisterRequest request = new RegisterRequest(
                "testgmail.com",
                "test1234",
                "test",
                null,
                "1234567891"
        );

        mockMvc.perform(post(REGISTER_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.uri").value(REGISTER_URI));

        verifyNoInteractions(authService);
    }

    @Test
    void register_whenPhoneNumberIsBlank_returnAuthResponse() throws Exception{
        RegisterRequest request = new RegisterRequest(
                "testgmail.com",
                "test1234",
                "test",
                "user",
                ""
        );

        mockMvc.perform(post(REGISTER_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.uri").value(REGISTER_URI));

        verifyNoInteractions(authService);
    }

    @Test
    void register_whenPhoneNumberIsNull_returnAuthResponse() throws Exception{
        RegisterRequest request = new RegisterRequest(
                "testgmail.com",
                "test1234",
                "test",
                "user",
                null
        );

        mockMvc.perform(post(REGISTER_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.uri").value(REGISTER_URI));

        verifyNoInteractions(authService);
    }

    @Test
    void register_whenPhoneNumberHasLessThan10_returnAuthResponse() throws Exception{
        RegisterRequest request = new RegisterRequest(
                "testgmail.com",
                "test1234",
                "test",
                "user",
                "123456789"
        );

        mockMvc.perform(post(REGISTER_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.uri").value(REGISTER_URI));

        verifyNoInteractions(authService);
    }

    @Test
    void register_whenPhoneNumberHasMoreThan15_returnAuthResponse() throws Exception{
        RegisterRequest request = new RegisterRequest(
                "testgmail.com",
                "test1234",
                "test",
                "user",
                "1234567891234567"
        );

        mockMvc.perform(post(REGISTER_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.uri").value(REGISTER_URI))
                .andExpect(jsonPath("fieldErrors.phoneNumber").value(phoneNumberFormat()));

        verifyNoInteractions(authService);
    }
}
