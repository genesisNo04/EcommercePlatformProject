package com.namnguyen.ecommerce_platform.common.response;

import java.time.LocalDateTime;

public record ApiErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String uri
) {}
