package com.namnguyen.ecommerce_platform.integration;

import com.namnguyen.ecommerce_platform.common.rate_limit.RateLimitResult;
import com.namnguyen.ecommerce_platform.common.rate_limit.RateLimitRule;
import com.namnguyen.ecommerce_platform.common.rate_limit.RateLimitService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.cache.CacheManager;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.ObjectMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@Import(BaseSecurityIntegrationTest.NoCacheTestConfig.class)
public class BaseSecurityIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:16-alpine");

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @MockitoBean
    protected RateLimitService rateLimitService;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.execute("""
                TRUNCATE TABLE
                    payments,
                    order_items,
                    orders,
                    cart_items,
                    carts,
                    products,
                    users
                RESTART IDENTITY CASCADE
                """);

        when(rateLimitService.isAllowed(any(), any()))
                .thenReturn(new RateLimitResult(true, 100, 99, 0L));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @TestConfiguration
    static class NoCacheTestConfig {
        @Bean
        @Primary
        CacheManager noOpCacheManager() {
            return new NoOpCacheManager();
        }
    }
}
