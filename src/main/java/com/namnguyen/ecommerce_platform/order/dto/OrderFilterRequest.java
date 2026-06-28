package com.namnguyen.ecommerce_platform.order.dto;

import com.namnguyen.ecommerce_platform.order.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderFilterRequest(
        OrderStatus status,
        BigDecimal minTotal,
        BigDecimal maxTotal,
        LocalDateTime createdAfter,
        LocalDateTime createdBefore
) {
}
