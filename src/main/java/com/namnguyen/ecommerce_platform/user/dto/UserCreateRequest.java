package com.namnguyen.ecommerce_platform.user.dto;

import jakarta.validation.constraints.*;

public record UserCreateRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid Email format")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 50, message = "Password has to be from 8 to 50 chars")
        String password,

        @NotBlank(message = "First Name is required")
        String firstName,

        @NotBlank(message = "Last Name is required")
        String lastName,

        @NotBlank(message = "Phone Number is required")
        @Pattern(regexp = "^\\+?[0-9]{10,15}$",
                message = "Phone number must be from 10 to 15 digits (with or without +)"
        )
        String phoneNumber
) {}
