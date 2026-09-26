package com.namnguyen.ecommerce_platform.auth.service;

import com.namnguyen.ecommerce_platform.auth.dto.AuthResponse;
import com.namnguyen.ecommerce_platform.auth.dto.LoginRequest;
import com.namnguyen.ecommerce_platform.auth.dto.RegisterRequest;
import com.namnguyen.ecommerce_platform.auth.mapper.AuthMapper;
import com.namnguyen.ecommerce_platform.security.jwt.JwtService;
import com.namnguyen.ecommerce_platform.security.user.CustomUserDetails;
import com.namnguyen.ecommerce_platform.security.user.CustomUserDetailsService;
import com.namnguyen.ecommerce_platform.user.dto.UserResponse;
import com.namnguyen.ecommerce_platform.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.namnguyen.ecommerce_platform.auth.error.AuthErrorMessages.PRINCIPAL_IS_INVALID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService customUserDetailsService;
    private final JwtService jwtService;
    private final UserService userService;

    @Override
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.password()
                )
        );

        if (!(authentication.getPrincipal()
                instanceof CustomUserDetails userDetails)) {
            throw new IllegalStateException(
                    PRINCIPAL_IS_INVALID
            );
        }

        String token = jwtService.generateToken(userDetails);

        log.info("User logged in userId={}", userDetails.getUserId());
        return new AuthResponse(token);
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        UserResponse userResponse = userService.createUser(AuthMapper.toUserCreateRequest(request));

        UserDetails userDetails =
                customUserDetailsService.loadUserByUsername(request.email());

        String token = jwtService.generateToken(userDetails);

        log.info("User registered userId={}", userResponse.id());

        return new AuthResponse(token);
    }
}
