package com.namnguyen.ecommerce_platform.integration.order;

import com.namnguyen.ecommerce_platform.integration.BaseIntegrationTest;
import com.namnguyen.ecommerce_platform.order.dto.CreateOrderItemRequest;
import com.namnguyen.ecommerce_platform.order.dto.CreateOrderRequest;
import com.namnguyen.ecommerce_platform.order.dto.OrderResponse;
import com.namnguyen.ecommerce_platform.order.exception.InvalidOrderStateException;
import com.namnguyen.ecommerce_platform.order.service.OrderService;
import com.namnguyen.ecommerce_platform.product.entity.Product;
import com.namnguyen.ecommerce_platform.product.enums.ProductStatus;
import com.namnguyen.ecommerce_platform.product.exception.InsufficientStockException;
import com.namnguyen.ecommerce_platform.user.entity.User;
import com.namnguyen.ecommerce_platform.user.enums.Role;
import org.assertj.core.api.SoftAssertions;
import org.hibernate.dialect.lock.PessimisticEntityLockException;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static com.namnguyen.ecommerce_platform.testutil.TestDataFactory.*;
import static org.assertj.core.api.Assertions.*;
import static org.testcontainers.shaded.org.bouncycastle.asn1.cmc.CMCStatus.failed;

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

    @RepeatedTest(5)
    void concurrentOrders_withOppositeItemOrder_shouldNotDeadlock() throws Exception {
        int stock = 1000;
        int orderQuantity = 10;
        int threads = 10;
        int totalOrders = threads * 2;
        int expectedStock = stock - totalOrders * orderQuantity;

        Product firstProduct = persistProduct(
                VALID_PRODUCT_NAME,
                VALID_PRODUCT_DESCRIPTION,
                VALID_PRODUCT_PRICE,
                stock,
                ProductStatus.ACTIVE
        );

        Product secondProduct = persistProduct(
                VALID_PRODUCT_NAME,
                VALID_PRODUCT_DESCRIPTION,
                VALID_PRODUCT_PRICE,
                stock,
                ProductStatus.ACTIVE
        );

        List<User> listUsers = persistCustomers(2);


        ExecutorService pool = Executors.newFixedThreadPool(totalOrders);
        CountDownLatch startGate = new CountDownLatch(1);
        AtomicInteger success = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            futures.add(pool.submit(() -> {
                startGate.await();
                orderService.createOrder(
                        new CreateOrderRequest(List.of(
                                new CreateOrderItemRequest(firstProduct.getId(), orderQuantity),
                                new CreateOrderItemRequest(secondProduct.getId(), orderQuantity)
                        )),
                        listUsers.getFirst().getId());
                success.incrementAndGet();
                return null;
            }));

            futures.add(pool.submit(() -> {
                startGate.await();
                orderService.createOrder(
                        new CreateOrderRequest(List.of(
                                new CreateOrderItemRequest(secondProduct.getId(), orderQuantity),
                                new CreateOrderItemRequest(firstProduct.getId(), orderQuantity)
                        )),
                        listUsers.getLast().getId());
                success.incrementAndGet();
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
            softly.assertThat(success.get()).isEqualTo(totalOrders);
            softly.assertThat(orderRepository.count()).isEqualTo(totalOrders);
            softly.assertThat(productRepository.findById(firstProduct.getId())
                    .orElseThrow().getQuantity()).isEqualTo(expectedStock);
            softly.assertThat(productRepository.findById(secondProduct.getId())
                    .orElseThrow().getQuantity()).isEqualTo(expectedStock);
        });
    }
}
