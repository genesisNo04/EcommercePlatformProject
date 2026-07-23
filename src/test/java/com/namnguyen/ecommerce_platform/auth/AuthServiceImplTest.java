package com.namnguyen.ecommerce_platform.auth;

import com.namnguyen.ecommerce_platform.auth.dto.AuthResponse;
import com.namnguyen.ecommerce_platform.auth.dto.LoginRequest;
import com.namnguyen.ecommerce_platform.auth.dto.RegisterRequest;
import com.namnguyen.ecommerce_platform.auth.service.AuthServiceImpl;
import com.namnguyen.ecommerce_platform.common.exception.DuplicateResourceException;
import com.namnguyen.ecommerce_platform.security.jwt.JwtService;
import com.namnguyen.ecommerce_platform.security.user.CustomUserDetailsService;
import com.namnguyen.ecommerce_platform.user.dto.UserCreateRequest;
import com.namnguyen.ecommerce_platform.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static com.namnguyen.ecommerce_platform.testutil.TestMessages.*;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceImplTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private CustomUserDetailsService customUserDetailsService;

    @Mock
    private JwtService jwtService;

    @Mock
    private UserService userService;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void login_whenCredentialsAreValid_returnsAuthResponse() {
        LoginRequest request = new LoginRequest(
                "test@gmail.com",
                "test123"
        );

        UserDetails userDetails = User.withUsername(request.email())
                        .password("encodedPassword")
                        .roles("CUSTOMER")
                        .build();

        String token = "fake-jwt-token";

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(mock(Authentication.class));
        when(customUserDetailsService.loadUserByUsername(request.email()))
                .thenReturn(userDetails);
        when(jwtService.generateToken(userDetails)).thenReturn(token);

        AuthResponse response = authService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.token()).isEqualTo(token);

        ArgumentCaptor<UsernamePasswordAuthenticationToken> captor = ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
        verify(authenticationManager).authenticate(captor.capture());

        UsernamePasswordAuthenticationToken authToken = captor.getValue();

        assertThat(authToken.getPrincipal()).isEqualTo(request.email());
        assertThat(authToken.getCredentials()).isEqualTo(request.password());

        verify(customUserDetailsService).loadUserByUsername(request.email());
        verify(jwtService).generateToken(userDetails);
        verifyNoInteractions(userService);
        verifyNoMoreInteractions(authenticationManager);
        verifyNoMoreInteractions(customUserDetailsService);
        verifyNoMoreInteractions(jwtService);
    }

    @Test
    void login_whenAuthenticationFails_throwsBadCredentialsException() {
        LoginRequest request = new LoginRequest(
                "test@gmail.com",
                "test123"
        );

        when(authenticationManager
                .authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad Credentials"));

        BadCredentialsException ex = assertThrows(
                BadCredentialsException.class,
                () -> authService.login(request)
        );

        assertThat(ex).isNotNull();
        assertThat(ex.getMessage()).isEqualTo("Bad Credentials");

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verifyNoMoreInteractions(authenticationManager);
        verifyNoInteractions(customUserDetailsService);
        verifyNoInteractions(jwtService);
        verifyNoInteractions(userService);
    }

    @Test
    void register_whenEmailIsNew_createsUserAndReturnsResponse() {
        String token = "fake-jwt-token";

        RegisterRequest request = new RegisterRequest(
                "test@gmail.com",
                "test",
                "testName",
                "userLast",
                "71234567891"
        );

        UserDetails userDetails = User.withUsername(request.email())
                .password("encodedPassword")
                .roles("CUSTOMER")
                .build();

        when(customUserDetailsService.loadUserByUsername(request.email())).thenReturn(userDetails);
        when(jwtService.generateToken(userDetails)).thenReturn(token);

        AuthResponse response = authService.register(request);

        assertThat(response).isNotNull();
        assertThat(response.token()).isEqualTo(token);

        ArgumentCaptor<UserCreateRequest> captor = ArgumentCaptor.forClass(UserCreateRequest.class);
        verify(userService).createUser(captor.capture());

        UserCreateRequest userCreateRequest = captor.getValue();
        assertThat(userCreateRequest.email()).isEqualTo(request.email());
        assertThat(userCreateRequest.password()).isEqualTo(request.password());
        assertThat(userCreateRequest.firstName()).isEqualTo(request.firstName());
        assertThat(userCreateRequest.lastName()).isEqualTo(request.lastName());
        assertThat(userCreateRequest.phoneNumber()).isEqualTo(request.phoneNumber());

        verify(customUserDetailsService).loadUserByUsername(request.email());
        verify(jwtService).generateToken(userDetails);
        verifyNoInteractions(authenticationManager);
        verifyNoMoreInteractions(userService);
        verifyNoMoreInteractions(customUserDetailsService);
        verifyNoMoreInteractions(jwtService);
    }

    @Test
    void register_whenEmailAlreadyExists_throwsDuplicateResourceException() {
        RegisterRequest request = new RegisterRequest(
                "test@gmail.com",
                "test",
                "testName",
                "userLast",
                "71234567891"
        );

        when(userService.createUser(any(UserCreateRequest.class))).thenThrow(new DuplicateResourceException(duplicateEmail()));

        DuplicateResourceException ex = assertThrows(
                DuplicateResourceException.class,
                () -> authService.register(request)
        );

        assertThat(ex).isNotNull();
        assertThat(ex.getMessage()).isEqualTo(duplicateEmail());

        verify(userService).createUser(any(UserCreateRequest.class));
        verifyNoMoreInteractions(userService);
        verifyNoInteractions(authenticationManager);
        verifyNoInteractions(customUserDetailsService);
        verifyNoInteractions(jwtService);
    }

    @Test
    void register_whenPhoneNumberAlreadyExists_throwsDuplicateResourceException() {
        RegisterRequest request = new RegisterRequest(
                "test@gmail.com",
                "test",
                "testName",
                "userLast",
                "71234567891"
        );

        when(userService.createUser(any(UserCreateRequest.class))).thenThrow(new DuplicateResourceException(duplicatePhoneNumber()));

        DuplicateResourceException ex = assertThrows(
                DuplicateResourceException.class,
                () -> authService.register(request)
        );

        assertThat(ex).isNotNull();
        assertThat(ex.getMessage()).isEqualTo(duplicatePhoneNumber());

        verify(userService).createUser(any(UserCreateRequest.class));
        verifyNoInteractions(authenticationManager);
        verifyNoMoreInteractions(userService);
        verifyNoInteractions(customUserDetailsService);
        verifyNoInteractions(jwtService);
    }

    @Test
    void register_whenUserDetailsCannotBeLoaded_throwsUsernameNotFoundException() {
        RegisterRequest request = new RegisterRequest(
                "test@gmail.com",
                "test",
                "testName",
                "userLast",
                "71234567891"
        );

       when(customUserDetailsService.loadUserByUsername(request.email()))
               .thenThrow(new UsernameNotFoundException("User not found"));

        UsernameNotFoundException ex = assertThrows(
                UsernameNotFoundException.class,
                () -> authService.register(request)
        );

        assertThat(ex).isNotNull();
        assertThat(ex.getMessage()).isEqualTo("User not found");

        verify(userService).createUser(any(UserCreateRequest.class));
        verify(customUserDetailsService).loadUserByUsername(request.email());
        verifyNoInteractions(authenticationManager);
        verifyNoMoreInteractions(userService);
        verifyNoMoreInteractions(customUserDetailsService);
        verifyNoInteractions(jwtService);
    }
}
