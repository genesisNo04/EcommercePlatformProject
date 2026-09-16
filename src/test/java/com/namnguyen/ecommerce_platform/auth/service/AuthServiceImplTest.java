package com.namnguyen.ecommerce_platform.auth.service;

import com.namnguyen.ecommerce_platform.auth.dto.AuthResponse;
import com.namnguyen.ecommerce_platform.auth.dto.LoginRequest;
import com.namnguyen.ecommerce_platform.auth.dto.RegisterRequest;
import com.namnguyen.ecommerce_platform.common.exception.DuplicateResourceException;
import com.namnguyen.ecommerce_platform.security.jwt.JwtService;
import com.namnguyen.ecommerce_platform.security.user.CustomUserDetailsService;
import com.namnguyen.ecommerce_platform.user.dto.UserCreateRequest;
import com.namnguyen.ecommerce_platform.user.enums.Role;
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

import static com.namnguyen.ecommerce_platform.testutil.TestDataFactory.*;
import static com.namnguyen.ecommerce_platform.testutil.messages.AuthTestMessages.*;
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
        LoginRequest loginRequest = createDefaultLoginRequest();

        UserDetails userDetails = User.withUsername(loginRequest.email())
                        .password(ENCODED_PASSWORD)
                        .roles(Role.CUSTOMER.name())
                        .build();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(mock(Authentication.class));
        when(customUserDetailsService.loadUserByUsername(loginRequest.email()))
                .thenReturn(userDetails);
        when(jwtService.generateToken(userDetails)).thenReturn(MOCK_JWT_TOKEN);

        AuthResponse authResponse = authService.login(loginRequest);

        assertThat(authResponse).isNotNull();
        assertThat(authResponse.token()).isEqualTo(MOCK_JWT_TOKEN);

        ArgumentCaptor<UsernamePasswordAuthenticationToken> authenticationCaptor
                = ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);

        verify(authenticationManager).authenticate(authenticationCaptor.capture());

        UsernamePasswordAuthenticationToken authToken = authenticationCaptor.getValue();

        assertThat(authToken.getPrincipal()).isEqualTo(loginRequest.email());
        assertThat(authToken.getCredentials()).isEqualTo(loginRequest.password());

        verify(customUserDetailsService).loadUserByUsername(loginRequest.email());
        verify(jwtService).generateToken(userDetails);
        verifyNoInteractions(userService);
        verifyNoMoreInteractions(authenticationManager);
        verifyNoMoreInteractions(customUserDetailsService);
        verifyNoMoreInteractions(jwtService);
    }

    @Test
    void login_whenAuthenticationFails_throwsBadCredentialsException() {
        LoginRequest loginRequest = createDefaultLoginRequest();

        when(authenticationManager
                .authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException(BAD_CREDENTIALS));

        BadCredentialsException ex = assertThrows(
                BadCredentialsException.class,
                () -> authService.login(loginRequest)
        );

        assertThat(ex.getMessage()).isEqualTo(BAD_CREDENTIALS);

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verifyNoMoreInteractions(authenticationManager);
        verifyNoInteractions(customUserDetailsService);
        verifyNoInteractions(jwtService);
        verifyNoInteractions(userService);
    }

    @Test
    void register_whenEmailIsNew_createsUserAndReturnsResponse() {
        RegisterRequest registerRequest = createDefaultRegisterRequest();

        UserDetails userDetails = User.withUsername(registerRequest.email())
                .password(ENCODED_PASSWORD)
                .roles(Role.CUSTOMER.name())
                .build();

        when(customUserDetailsService.loadUserByUsername(registerRequest.email())).thenReturn(userDetails);
        when(jwtService.generateToken(userDetails)).thenReturn(MOCK_JWT_TOKEN);

        AuthResponse authResponse = authService.register(registerRequest);

        assertThat(authResponse).isNotNull();
        assertThat(authResponse.token()).isEqualTo(MOCK_JWT_TOKEN);

        ArgumentCaptor<UserCreateRequest> userCreateRequestCaptor = ArgumentCaptor.forClass(UserCreateRequest.class);
        verify(userService).createUser(userCreateRequestCaptor.capture());

        UserCreateRequest userCreateRequest = userCreateRequestCaptor.getValue();
        assertThat(userCreateRequest.email()).isEqualTo(registerRequest.email());
        assertThat(userCreateRequest.password()).isEqualTo(registerRequest.password());
        assertThat(userCreateRequest.firstName()).isEqualTo(registerRequest.firstName());
        assertThat(userCreateRequest.lastName()).isEqualTo(registerRequest.lastName());
        assertThat(userCreateRequest.phoneNumber()).isEqualTo(registerRequest.phoneNumber());

        verify(customUserDetailsService).loadUserByUsername(registerRequest.email());
        verify(jwtService).generateToken(userDetails);
        verifyNoInteractions(authenticationManager);
        verifyNoMoreInteractions(userService);
        verifyNoMoreInteractions(customUserDetailsService);
        verifyNoMoreInteractions(jwtService);
    }

    @Test
    void register_whenEmailAlreadyExists_throwsDuplicateResourceException() {
        RegisterRequest registerRequest = createDefaultRegisterRequest();

        when(userService.createUser(any(UserCreateRequest.class)))
                .thenThrow(new DuplicateResourceException(DUPLICATE_EMAIL));

        DuplicateResourceException ex = assertThrows(
                DuplicateResourceException.class,
                () -> authService.register(registerRequest)
        );

        assertThat(ex).isNotNull();
        assertThat(ex.getMessage()).isEqualTo(DUPLICATE_EMAIL);

        verify(userService).createUser(any(UserCreateRequest.class));
        verifyNoMoreInteractions(userService);
        verifyNoInteractions(authenticationManager);
        verifyNoInteractions(customUserDetailsService);
        verifyNoInteractions(jwtService);
    }

    @Test
    void register_whenPhoneNumberAlreadyExists_throwsDuplicateResourceException() {
        RegisterRequest registerRequest = createDefaultRegisterRequest();

        when(userService.createUser(any(UserCreateRequest.class)))
                .thenThrow(new DuplicateResourceException(DUPLICATE_PHONE));

        DuplicateResourceException ex = assertThrows(
                DuplicateResourceException.class,
                () -> authService.register(registerRequest)
        );

        assertThat(ex).isNotNull();
        assertThat(ex.getMessage()).isEqualTo(DUPLICATE_PHONE);

        verify(userService).createUser(any(UserCreateRequest.class));
        verifyNoInteractions(authenticationManager);
        verifyNoMoreInteractions(userService);
        verifyNoInteractions(customUserDetailsService);
        verifyNoInteractions(jwtService);
    }

    @Test
    void register_whenUserDetailsCannotBeLoaded_throwsUsernameNotFoundException() {
        RegisterRequest registerRequest = createDefaultRegisterRequest();

       when(customUserDetailsService.loadUserByUsername(registerRequest.email()))
               .thenThrow(new UsernameNotFoundException(USER_NOT_FOUND));

        UsernameNotFoundException ex = assertThrows(
                UsernameNotFoundException.class,
                () -> authService.register(registerRequest)
        );

        assertThat(ex).isNotNull();
        assertThat(ex.getMessage()).isEqualTo(USER_NOT_FOUND);

        verify(userService).createUser(any(UserCreateRequest.class));
        verify(customUserDetailsService).loadUserByUsername(registerRequest.email());
        verifyNoInteractions(authenticationManager);
        verifyNoMoreInteractions(userService);
        verifyNoMoreInteractions(customUserDetailsService);
        verifyNoInteractions(jwtService);
    }
}
