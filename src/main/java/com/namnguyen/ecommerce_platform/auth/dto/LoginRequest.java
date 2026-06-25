package com.namnguyen.ecommerce_platform.auth.dto;

public record LoginRequest(
        String email,
        String password
) {}
