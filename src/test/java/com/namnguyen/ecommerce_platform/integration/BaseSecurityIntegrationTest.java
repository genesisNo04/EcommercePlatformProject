package com.namnguyen.ecommerce_platform.integration;

import com.namnguyen.ecommerce_platform.auth.dto.LoginRequest;
import com.namnguyen.ecommerce_platform.common.rate_limit.RateLimitResult;
import com.namnguyen.ecommerce_platform.common.rate_limit.RateLimitRule;
import com.namnguyen.ecommerce_platform.common.rate_limit.RateLimitService;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

import static com.namnguyen.ecommerce_platform.testutil.TestDataFactory.LOGIN_URI;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
public abstract class BaseSecurityIntegrationTest extends AbstractIntegrationTestSupport {

    @MockitoBean
    protected RateLimitService rateLimitService;

    @BeforeEach
    protected void allowRateLimit() {
        when(rateLimitService.isAllowed(anyString(), any(RateLimitRule.class)))
                .thenReturn(new RateLimitResult(true, 100, 99, 0L));
    }

    private record TokenResponse(String token) {}

    protected String loginAndGetToken(String email, String password) throws Exception {
        LoginRequest request = new LoginRequest (
                email,
                password
        );

        MvcResult result = mockMvc.perform(post(LOGIN_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();

        TokenResponse response = objectMapper.readValue(responseBody, TokenResponse.class);

        return response.token();
    }
}
