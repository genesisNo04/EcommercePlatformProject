package com.namnguyen.ecommerce_platform.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import static com.namnguyen.ecommerce_platform.auth.error.AuthErrorMessages.*;

public record LoginRequest(

        @NotBlank(message = AUTH_EMAIL_IS_REQUIRED)
        @Email(message = AUTH_EMAIL_IS_INVALID)
        String email,

        @NotBlank(message = AUTH_PASSWORD_IS_REQUIRED)
        String password
) {}
