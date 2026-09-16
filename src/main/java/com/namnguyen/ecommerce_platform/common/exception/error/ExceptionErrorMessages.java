package com.namnguyen.ecommerce_platform.common.exception.error;

import java.util.Arrays;

public final class ExceptionErrorMessages {

    private ExceptionErrorMessages() {}

    public static final String VALIDATION_FAILED =
            "Validation failed.";

    public static final String UNEXPECTED_ERROR =
            "An unexpected error occurred";

    public static final String INVALID_CREDENTIALS =
            "Invalid email or password.";

    public static String invalidParameter(String parameter) {
        return "Invalid parameter: " + parameter  + ".";
    }

    public static String invalidEnumValue(
            Object value,
            String parameter,
            Object[] allowedValues
    ) {
        return "Invalid value '" + value
                + "' for parameter '" + parameter
                + "'. Allowed values: "
                + Arrays.toString(allowedValues)
                + ".";
    }
}
