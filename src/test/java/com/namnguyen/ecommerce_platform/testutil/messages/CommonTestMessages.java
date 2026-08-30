package com.namnguyen.ecommerce_platform.testutil.messages;

public final class CommonTestMessages {
    public static final String VALIDATION_FAILED =
            "Validation failed.";

    private CommonTestMessages() {}

    public static String invalidParameter(String parameter) {
        return "Invalid parameter: " + parameter;
    }
}
