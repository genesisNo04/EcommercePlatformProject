package com.namnguyen.ecommerce_platform.common.response;

import java.time.LocalDateTime;
import java.util.Map;

public record ValidationErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String uri,
        Map<String, String> fieldErrors
) {}
