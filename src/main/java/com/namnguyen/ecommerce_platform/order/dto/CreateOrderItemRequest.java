package com.namnguyen.ecommerce_platform.order.dto;

import jakarta.validation.constraints.*;


public record CreateOrderItemRequest(

        @NotNull(message = "Product Id is required")
        Long productId,

        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity need to be at least 1")
        Integer quantity
) {}
