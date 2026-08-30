package com.namnguyen.ecommerce_platform.user.error;

public final class UserErrorMessages {

    public static final String EMAIL_ALREADY_EXISTS =
            "Email already exists.";

    public static final String PHONE_NUMBER_ALREADY_EXISTS =
            "Phone number already exists.";

    public static final String EMAIL_IS_REQUIRED =
            "Email is required.";

    public static final String EMAIL_IS_EMPTY =
            "Email cannot be empty.";

    public static final String EMAIL_IS_INVALID =
            "Email format is invalid.";

    public static final String PASSWORD_IS_REQUIRED =
            "Password is required.";

    public static final String PASSWORD_IS_EMPTY =
            "Password cannot be empty.";

    public static final String PASSWORD_IS_INVALID =
            "Password must be between 8 and 50 characters.";

    public static final String FIRST_NAME_IS_REQUIRED =
            "First name is required.";

    public static final String FIRST_NAME_IS_EMPTY =
            "First name cannot be empty.";

    public static final String LAST_NAME_IS_REQUIRED =
            "Last name is required.";

    public static final String LAST_NAME_IS_EMPTY =
            "Last name cannot be empty.";

    public static final String PHONE_NUMBER_IS_REQUIRED =
            "Phone number is required.";

    public static final String PHONE_NUMBER_IS_INVALID =
            "Phone number must contain 10 to 15 digits, with an optional leading +.";

    private UserErrorMessages() {}

    public static String userNotFoundWithId(Long userId) {
        return "User not found with id: " + userId + ".";
    }

    public static String userNotFoundWithEmail(String email) {
        return "User not found with email: " + email + ".";
    }


}
