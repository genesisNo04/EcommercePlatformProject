package com.namnguyen.ecommerce_platform.common.caching;

import com.namnguyen.ecommerce_platform.common.response.PageResponse;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;

@Component
public class RedisCacheConfigFactory {

    private final ObjectMapper objectMapper;

    public RedisCacheConfigFactory(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public RedisCacheConfiguration forType(
            Class<?> type,
            Duration ttl
    ) {
        JavaType javaType = objectMapper
                .getTypeFactory()
                .constructType(type);

        return buildConfiguration(javaType, ttl);
    }

    public RedisCacheConfiguration forPage(
            Class<?> contentType,
            Duration ttl
    ) {
        JavaType pageType = objectMapper
                .getTypeFactory()
                .constructParametricType(
                        PageResponse.class,
                        contentType
                );

        return buildConfiguration(pageType, ttl);
    }

    private RedisCacheConfiguration buildConfiguration(
            JavaType javaType,
            Duration ttl
    ) {
        JacksonJsonRedisSerializer<Object> serializer =
                new JacksonJsonRedisSerializer<>(

                        objectMapper,
                        javaType
                );

        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(ttl)
                .disableCachingNullValues()
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(serializer)
                );
    }
}
