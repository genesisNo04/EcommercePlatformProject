package com.namnguyen.ecommerce_platform.common.caching;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.*;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import tools.jackson.databind.jsontype.PolymorphicTypeValidator;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
@EnableCaching
public class RedisConfig {

    @Bean
    public RedisCacheConfiguration defaultCacheConfiguration() {
        PolymorphicTypeValidator typeValidator =
                BasicPolymorphicTypeValidator.builder()
                        .allowIfSubType("com.namnguyen.ecommerce_platform.")
                        .allowIfSubType("java.util.")
                        .allowIfSubType("java.math.")
                        .allowIfSubType("java.time.")
                        .build();

        GenericJacksonJsonRedisSerializer serializer =
                GenericJacksonJsonRedisSerializer.builder()
                        .enableDefaultTyping(typeValidator)
                        .build();

        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .disableCachingNullValues()
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(serializer)
                );
    }

    @Bean
    public RedisCacheManager cacheManager(
            RedisConnectionFactory connectionFactory,
            List<CacheConfigProvider> cacheConfigProviders,
            RedisCacheConfiguration configuration
    ) {

        Map<String, RedisCacheConfiguration> configs = new HashMap<>();

        for (CacheConfigProvider provider : cacheConfigProviders) {
            configs.putAll(provider.cacheConfiguration());
        }

        RedisCacheWriter cacheWriter = RedisCacheWriter.nonLockingRedisCacheWriter(
                connectionFactory,
                BatchStrategies.scan(1000)
        );

        return RedisCacheManager.builder(cacheWriter)
                .cacheDefaults(configuration)
                .withInitialCacheConfigurations(configs)
                .transactionAware()
                .build();
    }
}
