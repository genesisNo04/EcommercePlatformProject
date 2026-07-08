package com.namnguyen.ecommerce_platform.common.rate_limit;

import io.netty.util.internal.StringUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitService rateLimitService;

    private static final RateLimitRule LOGIN_RULE =
            new RateLimitRule("login", 5, 5, Duration.ofMinutes(1));

    private static final RateLimitRule REGISTER_RULE =
            new RateLimitRule("register", 3, 3, Duration.ofMinutes(1));

    private static final RateLimitRule PRODUCT_READ_RULE =
            new RateLimitRule("product-read", 60, 60, Duration.ofMinutes(1));

    private static final RateLimitRule CHECKOUT_RULE =
            new RateLimitRule("checkout", 10, 10, Duration.ofMinutes(1));

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        RateLimitRule rule = resolveRule(request);

        if (rule == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String key = buildRateLimitKey(request, rule);

        RateLimitResult result = rateLimitService.isAllowed(key, rule);

        addRateLimitHeaders(response, result);

        if (!result.allowed()) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);

            response.getWriter().write("""
                    {
                        "message": "Too many requests. Please try again later.",
                        "status": 429,
                        "path": "%s"
                    }
                    """.formatted(request.getRequestURI()));

            return;
        }

        filterChain.doFilter(request, response);
    }

    private RateLimitRule resolveRule(HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI();

        if ("POST".equals(method) && path.equals("/api/auth/login")) {
            return LOGIN_RULE;
        }

        if ("POST".equals(method) && path.equals("/api/auth/register")) {
            return REGISTER_RULE;
        }

        if ("GET".equals(method) && path.equals("/api/products")) {
            return PRODUCT_READ_RULE;
        }

        if ("POST".equals(method) && path.equals("/api/orders/checkout")) {
            return CHECKOUT_RULE;
        }

        return null;
    }

    private String buildRateLimitKey(HttpServletRequest request, RateLimitRule rule) {
        String identity = getIdentity(request);

        return "rate_limit:" + rule.name() + ":" + identity;
    }

    private String getIdentity(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null
            && authentication.isAuthenticated()
            && !"anonymousUser".equals(authentication.getPrincipal())) {
            return "user:" + authentication.getName();
        }

        return "ip:" + getClientIp(request);
    }

    private String getClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");

        if (StringUtils.hasText(forwardedFor)) {
            return forwardedFor.split(",")[0].trim();
        }

        return request.getRemoteAddr();
    }

    private void addRateLimitHeaders(HttpServletResponse response, RateLimitResult result) {
        response.setHeader("X-RateLimit-Limit", String.valueOf(result.limit()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(result.remainingTokens()));

        if (!result.allowed()) {
            response.setHeader("Retry-After", String.valueOf(result.retryAfterSeconds()));
        }
    }
}
