package com.namnguyen.ecommerce_platform.common.rate_limit;

import java.time.Duration;

public record RateLimitRule(
        String name,
        int capacity,
        int refillTokens,
        Duration refillPeriod
) {}
