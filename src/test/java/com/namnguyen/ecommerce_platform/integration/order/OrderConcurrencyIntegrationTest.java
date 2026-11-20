package com.namnguyen.ecommerce_platform.integration.order;

import com.namnguyen.ecommerce_platform.integration.BaseIntegrationTest;
import com.namnguyen.ecommerce_platform.order.dto.CreateOrderItemRequest;
import com.namnguyen.ecommerce_platform.order.dto.CreateOrderRequest;
import com.namnguyen.ecommerce_platform.order.dto.OrderResponse;
import com.namnguyen.ecommerce_platform.order.exception.InvalidOrderStateException;
import com.namnguyen.ecommerce_platform.order.service.OrderService;
import com.namnguyen.ecommerce_platform.product.entity.Product;
import com.namnguyen.ecommerce_platform.product.exception.InsufficientStockException;
import com.namnguyen.ecommerce_platform.user.entity.User;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class OrderConcurrencyIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private OrderService orderService;

    @RepeatedTest(5)
    void concurrentOrders_shouldNeverOversell()  throws Exception {
        Product product = persistProductWithQuantity(5);
        List<Long> userIds = persistCustomers(20)
                .stream()
                .map(User::getId)
                .toList();

        ExecutorService pool = Executors.newFixedThreadPool(20);
        CountDownLatch startGate = new CountDownLatch(1);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger outOfStock = new AtomicInteger();

        List<Future<?>> futures = new ArrayList<>();

        for (Long userId : userIds) {
            futures.add(pool.submit(() -> {
                startGate.await();
                try {
                    orderService.createOrder(
                            new CreateOrderRequest(List.of(new CreateOrderItemRequest(product.getId(), 1))), userId);
                    success.incrementAndGet();
                } catch (InsufficientStockException e) {
                    outOfStock.incrementAndGet();
                }
                return null;
            }));
        }

        startGate.countDown();
        pool.shutdown();
        pool.awaitTermination(30, TimeUnit.SECONDS);

        for (Future<?> future : futures) {
            future.get();
        }

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(productRepository.findById(product.getId()).orElseThrow().getQuantity()).isZero();
            softly.assertThat(success.get()).isEqualTo(5);
            softly.assertThat(outOfStock.get()).isEqualTo(15);
            softly.assertThat(orderRepository.count()).isEqualTo(5);
        });
    }

    @RepeatedTest(5)
    void concurrentCancels_shouldRestoreStockOnlyOnce() throws Exception {
        Product product = persistProductWithQuantity(10);
        User user = persistDefaultCustomer();

        OrderResponse orderResponse = orderService.createOrder(
                new CreateOrderRequest(List.of(new CreateOrderItemRequest(product.getId(), 3))),
                user.getId()
        );

        int threads = 10;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch startGate = new CountDownLatch(1);
        AtomicInteger cancelled = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            futures.add(pool.submit(() -> {
                startGate.await();
                try {
                    orderService.cancelOrder(orderResponse.orderId(), user.getId());
                    cancelled.incrementAndGet();
                } catch (InvalidOrderStateException e) {
                    rejected.incrementAndGet();
                }
                return null;
            }));
        }

        startGate.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).isTrue();
        for (Future<?> future : futures) {
            future.get();
        }

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(cancelled.get()).isEqualTo(1);
            softly.assertThat(rejected.get()).isEqualTo(threads - 1);
            softly.assertThat(productRepository.findById(product.getId()).orElseThrow().getQuantity()).isEqualTo(10);
        });
    }
}
