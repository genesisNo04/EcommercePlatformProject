package com.namnguyen.ecommerce_platform.user.dto;

import jakarta.validation.constraints.*;

import static com.namnguyen.ecommerce_platform.user.error.UserErrorMessages.*;

public record UserPatchRequest(

        @Pattern(regexp = ".*\\S.*", message = EMAIL_IS_EMPTY)
        @Email(message = EMAIL_IS_INVALID)
        String email,

        @Pattern(regexp = ".*\\S.*", message = PASSWORD_IS_EMPTY)
        @Size(min = 8, max = 50, message = PASSWORD_IS_INVALID)
        String password,

        @Pattern(regexp = ".*\\S.*", message = FIRST_NAME_IS_EMPTY)
        String firstName,

        @Pattern(regexp = ".*\\S.*", message = LAST_NAME_IS_EMPTY)
        String lastName,

        @Pattern(regexp = "^\\+?[0-9]{10,15}$",
                message = PHONE_NUMBER_IS_INVALID
        )
        String phoneNumber
) {}
