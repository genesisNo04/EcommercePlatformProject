package com.namnguyen.ecommerce_platform.order.dto;

import com.namnguyen.ecommerce_platform.order.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
   Long orderId,
   Long userId,
   List<OrderItemResponse> items,
   BigDecimal total,
   OrderStatus status,
   LocalDateTime createdAt,
   LocalDateTime updatedAt
) {}
