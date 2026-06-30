package com.namnguyen.ecommerce_platform.cart.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CartItemRequest(

        @NotNull(message = "Product id is required")
        Long productId,

        @NotNull(message = "Quantity id is required")
        @Min(value = 1, message = "Quantity need to be at least 1")
        Integer quantity
) {}
