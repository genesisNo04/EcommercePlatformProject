package com.namnguyen.ecommerce_platform.common.rate_limit;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;

@Configuration
public class RateLimitConfig {

    @Bean
    public RedisScript<List<?>> rateLimitScript() {
        DefaultRedisScript<List<?>> script = new DefaultRedisScript<>();

        script.setScriptText("""
                local key = KEYS[1]
                
                local capacity = tonumber(ARGV[1])
                local refillTokens = tonumber(ARGV[2])
                local refillPeriodMillis = tonumber(ARGV[3])
                local nowMillis = tonumber(ARGV[4])
                local requestedTokens = tonumber(ARGV[5])
                local ttlMillis = tonumber(ARGV[6])
                
                local bucket = redis.call('HMGET', key, 'tokens', 'lastRefillTime')
                local tokens = tonumber(bucket[1])
                local lastRefillTime = tonumber(bucket[2])
                
                if tokens == nil then
                    tokens = capacity
                    lastRefillTime = nowMillis
                end
                
                local elapsedMillis = math.max(0, nowMillis - lastRefillTime)
                local tokensToAdd = (elapsedMillis * refillTokens) / refillPeriodMillis
                
                if tokensToAdd > 0 then
                    tokens = math.min(capacity, tokens + tokensToAdd)
                    lastRefillTime = nowMillis
                end
                
                local allowed = 0
                local retryAfterMillis = 0
                
                if tokens >= requestedTokens then
                    tokens = tokens - requestedTokens
                    allowed = 1
                else
                    local missingTokens = requestedTokens - tokens
                    retryAfterMillis = math.ceil((missingTokens * refillPeriodMillis) / refillTokens)
                end
                
                redis.call('HSET', key, 'tokens', tokens, 'lastRefillTime', lastRefillTime)
                redis.call('PEXPIRE', key, ttlMillis)
                
                return {allowed, math.floor(tokens), retryAfterMillis}
                """
        );

        script.setResultType((Class<List<?>>) (Class<?>) List.class);
        return script;
    }
}
