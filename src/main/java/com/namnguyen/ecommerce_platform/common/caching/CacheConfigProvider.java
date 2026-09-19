package com.namnguyen.ecommerce_platform.common.caching;

import org.springframework.data.redis.cache.RedisCacheConfiguration;

import java.util.Map;

public interface CacheConfigProvider {

    Map<String, RedisCacheConfiguration> cacheConfiguration();
}
