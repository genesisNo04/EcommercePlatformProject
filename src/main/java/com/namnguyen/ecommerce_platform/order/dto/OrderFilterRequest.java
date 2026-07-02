package com.namnguyen.ecommerce_platform.order.dto;

import com.namnguyen.ecommerce_platform.order.enums.OrderStatus;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderFilterRequest(
        OrderStatus status,
        BigDecimal minTotal,
        BigDecimal maxTotal,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        LocalDateTime createdAfter,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        LocalDateTime createdBefore
) {
}
