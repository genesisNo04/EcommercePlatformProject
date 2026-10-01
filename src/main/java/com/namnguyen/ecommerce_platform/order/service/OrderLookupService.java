package com.namnguyen.ecommerce_platform.order.service;

import com.namnguyen.ecommerce_platform.common.exception.NoResourceFoundException;
import com.namnguyen.ecommerce_platform.order.entity.Order;
import com.namnguyen.ecommerce_platform.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static com.namnguyen.ecommerce_platform.order.error.OrderErrorMessages.orderNotFoundWithId;
import static com.namnguyen.ecommerce_platform.order.error.OrderErrorMessages.orderNotFoundWithIdAndUserId;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderLookupService {

    private final OrderRepository orderRepository;

    public Order getOrderById(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new NoResourceFoundException(
                                orderNotFoundWithId(orderId)));

        log.debug("Fetched order orderId={}", orderId);
        return order;
    }

    public Order getOrderByIdAndUserId(Long orderId, Long userId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() ->
                        new NoResourceFoundException(
                                orderNotFoundWithIdAndUserId(orderId, userId)));

        log.debug("Fetched order orderId={} userId={}", orderId, userId);

        return order; 
    }
}
