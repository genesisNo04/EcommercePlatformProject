package com.namnguyen.ecommerce_platform.order.service;

import com.namnguyen.ecommerce_platform.common.exception.NoResourceFoundException;
import com.namnguyen.ecommerce_platform.order.entity.Order;
import com.namnguyen.ecommerce_platform.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderLookupService {

    private final OrderRepository orderRepository;

    public Order getOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new NoResourceFoundException(
                                "No order found with id: " + orderId));
    }

    public Order getOrderByIdAndUserId(Long orderId, Long userId) {
        return orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() ->
                        new NoResourceFoundException(
                                "No order found with id: " + orderId +
                                " for user id: " + userId));
    }
}
