package com.namnguyen.ecommerce_platform.order.service;

import com.namnguyen.ecommerce_platform.order.dto.CreateOrderRequest;
import com.namnguyen.ecommerce_platform.order.dto.OrderResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderService {

    OrderResponse createOrder(CreateOrderRequest request);

    OrderResponse getOrderByIdAndUserId(Long orderId);

    Page<OrderResponse> getOrdersAndUserId(Pageable pageable);

    void cancelOrder(Long orderId);
}
