package com.namnguyen.ecommerce_platform.cart.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import static com.namnguyen.ecommerce_platform.cart.error.CartErrorMessages.*;

public record CartItemRequest(

        @NotNull(message = CART_ITEM_PRODUCT_ID_IS_REQUIRED)
        @Positive(message = CART_ITEM_PRODUCT_ID_IS_INVALID)
        Long productId,

        @NotNull(message = CART_ITEM_QUANTITY_IS_REQUIRED)
        @Min(value = 1, message = CART_ITEM_QUANTITY_IS_INVALID)
        Integer quantity
) {}
