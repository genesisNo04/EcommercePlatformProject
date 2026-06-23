package com.namnguyen.ecommerce_platform.product.dto;

import com.namnguyen.ecommerce_platform.product.enums.ProductStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductResponse(
        Long id,
        String name,
        String description,
        BigDecimal price,
        Integer quantity,
        ProductStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
