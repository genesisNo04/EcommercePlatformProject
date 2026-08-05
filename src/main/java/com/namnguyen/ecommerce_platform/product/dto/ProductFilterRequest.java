package com.namnguyen.ecommerce_platform.product.dto;

import com.namnguyen.ecommerce_platform.product.enums.ProductStatus;
import org.springframework.validation.FieldError;

import java.math.BigDecimal;

public record ProductFilterRequest(
        ProductStatus status,
        String keyword,
        BigDecimal minPrice,
        BigDecimal maxPrice
) {
}
