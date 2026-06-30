package com.namnguyen.ecommerce_platform.cart.dto;

import java.math.BigDecimal;

public record CartItemResponse(
        Long productId,
        String productName,
        BigDecimal price,
        Integer quantity
) {}
