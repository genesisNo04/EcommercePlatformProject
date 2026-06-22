package com.namnguyen.ecommerce_platform.user.dto;

import jakarta.validation.constraints.*;

public record UserPatchRequest(

        @Email(message = "Invalid Email format")
        String email,

        @Size(min = 8, max = 50, message = "Password has to be from 8 to 50 chars")
        String password,

        @Size(min = 1)
        @Pattern(regexp = ".*\\S.*", message = "First name cannot be empty")
        String firstName,

        @Size(min = 1)
        @Pattern(regexp = ".*\\S.*", message = "Last name cannot be empty")
        String lastName,

        @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Invalid phone number format")
        String phoneNumber
) {}
