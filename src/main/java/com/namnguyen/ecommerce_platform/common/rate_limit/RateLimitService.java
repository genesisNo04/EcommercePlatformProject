package com.namnguyen.ecommerce_platform.common.rate_limit;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final StringRedisTemplate stringRedisTemplate;
    private final RedisScript<List<?>> rateLimitScript;

    public RateLimitResult isAllowed(String key, RateLimitRule rule) {
        long nowMillis = Instant.now().toEpochMilli();
        long refillPeriodMillis = rule.refillPeriod().toMillis();
        long ttlMillis = refillPeriodMillis * 2;

        List<?> result = stringRedisTemplate.execute(
                rateLimitScript,
                List.of(key),
                String.valueOf(rule.capacity()),
                String.valueOf(rule.refillTokens()),
                String.valueOf(refillPeriodMillis),
                String.valueOf(nowMillis),
                "1",
                String.valueOf(ttlMillis)
        );

        if (result == null || result.size() < 3) {
            return new RateLimitResult(
                    true,
                    rule.capacity(),
                    rule.capacity(),
                    0
            );
        }

        boolean allowed = toLong(result.get(0)) == 1L;
        int remainingTokens = (int) toLong(result.get(1));
        long retryAfterMillis = toLong(result.get(2));

        long retryAfterSeconds = retryAfterMillis <= 0
                ? 0
                : (long) Math.ceil(retryAfterMillis / 1000.0);

        return new RateLimitResult(
                allowed,
                rule.capacity(),
                remainingTokens,
                retryAfterSeconds
        );
    }

    private long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }

        return Long.parseLong(value.toString());
    }
}
