package com.namnguyen.ecommerce_platform.product.config;

import com.namnguyen.ecommerce_platform.common.caching.CacheConfigProvider;
import com.namnguyen.ecommerce_platform.common.caching.CacheNames;
import com.namnguyen.ecommerce_platform.common.caching.RedisCacheConfigFactory;
import com.namnguyen.ecommerce_platform.product.dto.ProductResponse;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;

@Component
public class ProductCacheConfig implements CacheConfigProvider {

    private static final Duration TTL = Duration.ofMinutes(10);

    private final RedisCacheConfigFactory cacheConfigFactory;

    public ProductCacheConfig(RedisCacheConfigFactory cacheConfigFactory) {
        this.cacheConfigFactory = cacheConfigFactory;
    }

    @Override
    public Map<String, RedisCacheConfiguration> cacheConfiguration() {
        return Map.of(
                CacheNames.PRODUCTS,
                cacheConfigFactory.forType(
                        ProductResponse.class,
                        TTL
                ),

                CacheNames.PRODUCT_PAGES,
                cacheConfigFactory.forPage(
                        ProductResponse.class,
                        TTL
                )
        );
    }
}
