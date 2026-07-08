package com.namnguyen.ecommerce_platform.common.rate_limit;

public record RateLimitResult(
        boolean allowed,
        int limit,
        int remainingTokens,
        long retryAfterSeconds
) {}
