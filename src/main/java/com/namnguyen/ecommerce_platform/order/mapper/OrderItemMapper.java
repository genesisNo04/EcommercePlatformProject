package com.namnguyen.ecommerce_platform.order.mapper;

import com.namnguyen.ecommerce_platform.order.dto.OrderItemResponse;
import com.namnguyen.ecommerce_platform.order.entity.OrderItem;

public class OrderItemMapper {

    public static OrderItemResponse toResponse(OrderItem orderItem) {
        return new OrderItemResponse(
                orderItem.getProduct().getId(),
                orderItem.getProduct().getName(),
                orderItem.getQuantity(),
                orderItem.getPrice());
    }
}
