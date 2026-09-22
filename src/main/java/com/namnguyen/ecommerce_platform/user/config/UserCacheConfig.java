    package com.namnguyen.ecommerce_platform.user.config;

    import com.namnguyen.ecommerce_platform.common.caching.CacheConfigProvider;
    import com.namnguyen.ecommerce_platform.common.caching.CacheNames;
    import com.namnguyen.ecommerce_platform.common.caching.RedisCacheConfigFactory;
    import com.namnguyen.ecommerce_platform.user.dto.UserResponse;
    import org.springframework.data.redis.cache.RedisCacheConfiguration;
    import org.springframework.stereotype.Component;

    import java.time.Duration;
    import java.util.Map;

    @Component
    public class UserCacheConfig implements CacheConfigProvider {

        private static final Duration TTL = Duration.ofMinutes(10);

        private final RedisCacheConfigFactory cacheConfigFactory;

        public UserCacheConfig(RedisCacheConfigFactory cacheConfigFactory) {
            this.cacheConfigFactory = cacheConfigFactory;
        }

        @Override
        public Map<String, RedisCacheConfiguration> cacheConfiguration() {
            return Map.of(
                    CacheNames.USERS,
                    cacheConfigFactory.forType(
                            UserResponse.class,
                            TTL
                    ),

                    CacheNames.USERS_PAGES,
                    cacheConfigFactory.forPage(
                            UserResponse.class,
                            TTL
                    )
            );
        }
    }
