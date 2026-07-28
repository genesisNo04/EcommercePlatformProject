package com.namnguyen.ecommerce_platform.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid Email format")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 50, message = "Password has to be from 8 to 50 chars")
        String password
) {}
