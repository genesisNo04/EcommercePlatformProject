package com.namnguyen.ecommerce_platform.order.service;

import com.namnguyen.ecommerce_platform.order.dto.CreateOrderRequest;
import com.namnguyen.ecommerce_platform.order.dto.OrderFilterRequest;
import com.namnguyen.ecommerce_platform.order.dto.OrderResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderService {

    OrderResponse createOrder(CreateOrderRequest request, Long userId);

    OrderResponse checkoutCart(Long userId);

    OrderResponse getOrderById(Long orderId, Long userId);

    Page<OrderResponse> getOrders(Long userId, OrderFilterRequest request, Pageable pageable);

    void cancelOrder(Long orderId, Long userId);
}
