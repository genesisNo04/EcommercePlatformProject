package com.namnguyen.ecommerce_platform.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserPatchRequest(

        @Email(message = "Invalid Email format")
        String email,

        @Size(min = 8, max = 50, message = "Password has to be from 8 to 15 chars")
        String password,

        String firstName,

        String lastName,

        @Pattern(regexp = "^\\+?[0-9]{10,15}$")
        String phoneNumber
) {}
