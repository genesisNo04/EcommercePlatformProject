package com.namnguyen.ecommerce_platform.testutil.messages;

public final class AuthTestMessages {
    public static final String AUTH_EMAIL_IS_REQUIRED =
            "Email is required.";

    public static final String AUTH_EMAIL_IS_INVALID =
            "Email format is invalid.";

    public static final String AUTH_PASSWORD_IS_REQUIRED =
            "Password is required.";

    public static final String AUTH_PASSWORD_IS_INVALID =
            "Password must be between 8 and 50 characters.";

    public static final String AUTH_FIRST_NAME_IS_REQUIRED =
            "First name is required.";

    public static final String AUTH_LAST_NAME_IS_REQUIRED =
            "Last name is required.";

    public static final String AUTH_PHONE_NUMBER_IS_REQUIRED =
            "Phone number is required.";

    public static final String AUTH_PHONE_NUMBER_IS_INVALID =
            "Phone number must contain 10 to 15 digits, with an optional leading +.";

    public static final String INVALID_CREDENTIALS =
            "Invalid email or password.";

    public static final String DUPLICATE_EMAIL =
            "Email already exists.";

    public static final String DUPLICATE_PHONE =
            "Phone number already exists.";

    public static final String USER_NOT_FOUND =
            "User not found";

    public static final String BAD_CREDENTIALS =
            "Bad credentials";


    private AuthTestMessages() {}
}
