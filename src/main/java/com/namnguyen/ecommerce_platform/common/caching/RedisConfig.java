package com.namnguyen.ecommerce_platform.common.caching;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.*;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import tools.jackson.databind.jsontype.PolymorphicTypeValidator;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
public class RedisConfig {

    @Bean
    public RedisCacheConfiguration cacheConfiguration() {

        PolymorphicTypeValidator typeValidator =
                BasicPolymorphicTypeValidator.builder()
                        .allowIfSubType("com.namnguyen.ecommerce_platform.")
                        .allowIfSubType("org.springframework.data.domain.")
                        .allowIfSubType("java.util")
                        .build();

        GenericJacksonJsonRedisSerializer serializer =
                GenericJacksonJsonRedisSerializer.builder()
                        .enableDefaultTyping(typeValidator)
                        .build();


        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .disableCachingNullValues()
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(
                                serializer
                        )
                );
    }

    @Bean
    public RedisCacheManager cacheManager(
            RedisConnectionFactory factory,
            RedisCacheConfiguration configuration
    ) {

        RedisCacheWriter cacheWriter = RedisCacheWriter.nonLockingRedisCacheWriter(
                factory,
                BatchStrategies.scan(1000)
        );

        Map<String, RedisCacheConfiguration> configs = new HashMap<>();

        configs.put(
                CacheNames.PRODUCTS,
                configuration.entryTtl(Duration.ofMinutes(10))
        );

        configs.put(
                CacheNames.PRODUCT_PAGES,
                configuration.entryTtl(Duration.ofMinutes(10))
        );

        return RedisCacheManager.builder(cacheWriter)
                .cacheDefaults(configuration)
                .withInitialCacheConfigurations(configs)
                .transactionAware()
                .build();
    }
}
