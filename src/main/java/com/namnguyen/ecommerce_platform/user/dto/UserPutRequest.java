package com.namnguyen.ecommerce_platform.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserPutRequest(

        @NotBlank(message = "Email cannot be empty")
        @Email(message = "Invalid Email format")
        String email,

        @NotBlank(message = "password cannot be empty")
        @Size(min = 8, max = 50, message = "Password has to be from 8 to 15 chars")
        String password,

        @NotBlank(message = "First Name cannot be empty")
        String firstName,

        @NotBlank(message = "Last Name cannot be empty")
        String lastName,

        @NotBlank(message = "Phone Number cannot be empty")
        @Pattern(regexp = "^\\+?[0-9]{10,15}$")
        String phoneNumber
) {}
