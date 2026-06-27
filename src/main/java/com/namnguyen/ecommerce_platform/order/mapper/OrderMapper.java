package com.namnguyen.ecommerce_platform.order.mapper;

import com.namnguyen.ecommerce_platform.order.dto.OrderItemResponse;
import com.namnguyen.ecommerce_platform.order.dto.OrderResponse;
import com.namnguyen.ecommerce_platform.order.entity.Order;

import java.util.List;

public class OrderMapper {

    public static OrderResponse toResponse(Order order) {
        List<OrderItemResponse> items = order.getOrderItems().stream().map(OrderItemMapper::toResponse).toList();
        return new OrderResponse(
                order.getId(),
                order.getUser().getId(),
                items,
                order.getTotal(),
                order.getStatus(),
                order.getCreatedAt(),
                order.getUpdatedAt());
    }
}
