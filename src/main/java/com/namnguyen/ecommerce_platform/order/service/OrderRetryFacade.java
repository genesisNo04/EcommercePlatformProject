package com.namnguyen.ecommerce_platform.order.service;

import com.namnguyen.ecommerce_platform.order.dto.CreateOrderRequest;
import com.namnguyen.ecommerce_platform.order.dto.OrderResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

@Slf4j
@Service
public class OrderRetryFacade {

    private static final int MAX_ATTEMPTS = 3;
    private final OrderService orderService;
    private final long baseDelayMs;

    public OrderRetryFacade(OrderService orderService,
                            @Value("${order.retry.base-delay-ms:50}") long baseDelayMs) {
        this.orderService = orderService;
        this.baseDelayMs = baseDelayMs;
    }

    public OrderResponse checkoutCart(Long userId) {
        return withRetry(() -> orderService.checkoutCart(userId), userId);
    }

    public OrderResponse createOrder(CreateOrderRequest request, Long userId) {
        return withRetry(() -> orderService.createOrder(request, userId), userId);
    }

    private void sleepWithBackOff(int attempt) {
        long base = baseDelayMs * attempt;
        long jitter = ThreadLocalRandom.current().nextLong(baseDelayMs / 2 + 1);
        try {
            Thread.sleep(base + jitter);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    private OrderResponse withRetry(Supplier<OrderResponse> action, Long userId) {
        for (int attempt = 1; ; attempt++) {
            try {
                return action.get();
            } catch (PessimisticLockingFailureException e) {
                if (attempt == MAX_ATTEMPTS) {
                    throw e;
                }

                log.warn("Lock conflict, retrying attempt={} userId={}", attempt, userId);
                sleepWithBackOff(attempt);
            }
        }
    }
}
