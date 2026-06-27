package com.namnguyen.ecommerce_platform.auth.service;

import com.namnguyen.ecommerce_platform.auth.dto.AuthResponse;
import com.namnguyen.ecommerce_platform.auth.dto.LoginRequest;
import com.namnguyen.ecommerce_platform.auth.dto.RegisterRequest;
import com.namnguyen.ecommerce_platform.auth.mapper.AuthMapper;
import com.namnguyen.ecommerce_platform.security.jwt.JwtService;
import com.namnguyen.ecommerce_platform.security.user.CustomUserDetailsService;
import com.namnguyen.ecommerce_platform.user.dto.UserCreateRequest;
import com.namnguyen.ecommerce_platform.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService customUserDetailsService;
    private final JwtService jwtService;
    private final UserService userService;

    @Override
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.password()
                )
        );

        UserDetails userDetails = customUserDetailsService.loadUserByUsername(request.email());

        String token = jwtService.generateToken(userDetails);
        return new AuthResponse(token);
    }

    @Override
    public AuthResponse register(RegisterRequest request) {
        userService.createUser(AuthMapper.toUserCreateRequest(request));

        UserDetails userDetails =
                customUserDetailsService.loadUserByUsername(request.email());

        String token = jwtService.generateToken(userDetails);
        return new AuthResponse(token);
    }
}
