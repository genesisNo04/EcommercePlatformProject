package com.namnguyen.ecommerce_platform.product.dto;

import com.namnguyen.ecommerce_platform.product.enums.ProductStatus;

import java.math.BigDecimal;

public record ProductFilterRequest(
        ProductStatus status,
        String keyword,
        BigDecimal minPrice,
        BigDecimal maxPrice
) {
}
