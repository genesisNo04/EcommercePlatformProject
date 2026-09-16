package com.namnguyen.ecommerce_platform.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import static com.namnguyen.ecommerce_platform.auth.error.AuthErrorMessages.*;

public record RegisterRequest(
        @NotBlank(message = AUTH_EMAIL_IS_REQUIRED)
        @Email(message = AUTH_EMAIL_IS_INVALID)
        String email,

        @NotBlank(message = AUTH_PASSWORD_IS_REQUIRED)
        @Size(min = 8, max = 50, message = AUTH_PASSWORD_IS_INVALID)
        String password,

        @NotBlank(message = AUTH_FIRST_NAME_IS_REQUIRED)
        String firstName,

        @NotBlank(message = AUTH_LAST_NAME_IS_REQUIRED)
        String lastName,

        @NotBlank(message = AUTH_PHONE_NUMBER_IS_REQUIRED)
        @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = AUTH_PHONE_NUMBER_IS_INVALID)
        String phoneNumber
)
{}
