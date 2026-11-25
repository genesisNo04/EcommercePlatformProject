package com.namnguyen.ecommerce_platform.order.service;

import com.namnguyen.ecommerce_platform.order.dto.OrderResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.dialect.lock.PessimisticEntityLockException;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderRetryFacade {

    private static final int MAX_ATTEMPS = 3;
    private final OrderService orderService;

    public OrderResponse checkoutCart(Long userId) {
        for (int attemp = 1; ; attemp++) {
            try {
                return orderService.checkoutCart(userId);
            } catch (PessimisticEntityLockException e) {
                if (attemp == MAX_ATTEMPS) {
                    throw e;
                }

                log.warn("Checkout lock conflict, retrying attempt={} userId={}", attemp, userId);
                sleepWithBackOff(attemp);
            }
        }
    }

    private void sleepWithBackOff(int attemp) {
        long base = 50L * (1L << (attemp - 1));
        long jitter = ThreadLocalRandom.current().nextLong(25);
        try {
            Thread.sleep(base + jitter);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
