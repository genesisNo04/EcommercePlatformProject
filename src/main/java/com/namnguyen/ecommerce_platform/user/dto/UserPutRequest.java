package com.namnguyen.ecommerce_platform.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import static com.namnguyen.ecommerce_platform.user.error.UserErrorMessages.*;

public record UserPutRequest(

        @NotBlank(message = EMAIL_IS_REQUIRED)
        @Email(message = EMAIL_IS_INVALID)
        String email,

        @NotBlank(message = PASSWORD_IS_REQUIRED)
        @Size(min = 8, max = 50, message = PASSWORD_IS_INVALID)
        String password,

        @NotBlank(message = FIRST_NAME_IS_REQUIRED)
        String firstName,

        @NotBlank(message = LAST_NAME_IS_REQUIRED)
        String lastName,

        @NotBlank(message = PHONE_NUMBER_IS_REQUIRED)
        @Pattern(
                regexp = "^\\+?[0-9]{10,15}$",
                message = PHONE_NUMBER_IS_INVALID
        )
        String phoneNumber
) {}
