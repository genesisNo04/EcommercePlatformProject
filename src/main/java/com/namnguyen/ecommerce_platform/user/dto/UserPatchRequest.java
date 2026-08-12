package com.namnguyen.ecommerce_platform.user.dto;

import jakarta.validation.constraints.*;

public record UserPatchRequest(

        @Pattern(regexp = ".*\\S.*", message = "Email cannot be empty")
        @Email(message = "Invalid Email format")
        String email,

        @Pattern(regexp = ".*\\S.*", message = "Password cannot be empty")
        @Size(min = 8, max = 50, message = "Password has to be from 8 to 50 chars")
        String password,

        @Pattern(regexp = ".*\\S.*", message = "First Name cannot be empty")
        String firstName,

        @Pattern(regexp = ".*\\S.*", message = "Last Name cannot be empty")
        String lastName,

        @Pattern(regexp = "^\\+?[0-9]{10,15}$",
                message = "Phone number must be from 10 to 15 digits (with or without +)"
        )
        String phoneNumber
) {}
