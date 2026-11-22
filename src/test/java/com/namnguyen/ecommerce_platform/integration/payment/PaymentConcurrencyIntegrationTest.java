package com.namnguyen.ecommerce_platform.integration.payment;

import com.namnguyen.ecommerce_platform.integration.BaseIntegrationTest;
import com.namnguyen.ecommerce_platform.order.dto.CreateOrderItemRequest;
import com.namnguyen.ecommerce_platform.order.dto.CreateOrderRequest;
import com.namnguyen.ecommerce_platform.order.dto.OrderResponse;
import com.namnguyen.ecommerce_platform.order.entity.Order;
import com.namnguyen.ecommerce_platform.order.enums.OrderStatus;
import com.namnguyen.ecommerce_platform.order.exception.InvalidOrderStateException;
import com.namnguyen.ecommerce_platform.order.service.OrderService;
import com.namnguyen.ecommerce_platform.payment.dto.PaymentRequest;
import com.namnguyen.ecommerce_platform.payment.entity.Payment;
import com.namnguyen.ecommerce_platform.payment.enums.PaymentMethod;
import com.namnguyen.ecommerce_platform.payment.enums.PaymentStatus;
import com.namnguyen.ecommerce_platform.payment.service.PaymentService;
import com.namnguyen.ecommerce_platform.product.entity.Product;
import com.namnguyen.ecommerce_platform.user.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.assertj.core.api.Assertions.*;
import org.assertj.core.api.SoftAssertions;

public class PaymentConcurrencyIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private OrderService orderService;

    @Test
    void concurrentPayAndCancel_onlyOneWinsPerOrder() throws InterruptedException, ExecutionException {
        int orderCount = 10;
        int quantityPerOrder = 3;
        Product product = persistProductWithQuantity(100);
        User user = persistDefaultCustomer();

        List<Long> orderIds = new ArrayList<>();
        for (int i = 0; i < orderCount; i++) {
            OrderResponse orderResponse = orderService.createOrder(
                    new CreateOrderRequest(List.of(new CreateOrderItemRequest(product.getId(), quantityPerOrder))), user.getId());
            paymentService.submitPayment(orderResponse.orderId(), user.getId(), new PaymentRequest(PaymentMethod.CARD));
            orderIds.add(orderResponse.orderId());
        }

        ExecutorService pool = Executors.newFixedThreadPool(orderCount * 2);
        CountDownLatch startGate = new CountDownLatch(1);
        AtomicInteger succeeded = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();

        for (Long orderId : orderIds) {
            futures.add(pool.submit(() -> {
                startGate.await();
                try {
                    paymentService.confirmPayment(orderId, user.getId(), PaymentStatus.SUCCESS);
                    succeeded.incrementAndGet();
                } catch (InvalidOrderStateException e) {
                    rejected.incrementAndGet();
                }
                return null;
            }));

            futures.add(pool.submit(() -> {
                startGate.await();
                try {
                    orderService.cancelOrder(orderId, user.getId());
                    succeeded.incrementAndGet();
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

        long paidOrders = orderIds.stream()
                .map(id -> orderRepository.findById(id).orElseThrow())
                .filter(o -> o.getStatus() == OrderStatus.PAID)
                .count();

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(succeeded.get()).isEqualTo(orderCount);
            softly.assertThat(rejected.get()).isEqualTo(orderCount);
            softly.assertThat(productRepository.findById(product.getId()).orElseThrow().getQuantity())
                    .isEqualTo(100 - (int) paidOrders * quantityPerOrder);

            for (Long id : orderIds) {
                Payment payment = paymentRepository.findByOrderId(id).orElseThrow();
                Order order = orderRepository.findById(id).orElseThrow();
                if (order.getStatus() == OrderStatus.CANCELLED) {
                    softly.assertThat(payment.getPaymentStatus())
                            .as("order %d is cancelled but payment succeeded", id)
                            .isNotEqualTo(PaymentStatus.SUCCESS);
                }
            }
        });
    }
}
