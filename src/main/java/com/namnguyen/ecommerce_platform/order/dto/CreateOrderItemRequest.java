package com.namnguyen.ecommerce_platform.order.dto;

import jakarta.validation.constraints.*;


public record CreateOrderItemRequest(

        @NotNull(message = "Product id is required")
        Long productId,

        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        Integer quantity
) {}
