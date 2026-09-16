package com.namnguyen.ecommerce_platform.order.dto;

import jakarta.validation.constraints.*;

import static com.namnguyen.ecommerce_platform.order.error.OrderErrorMessages.*;


public record CreateOrderItemRequest(

        @NotNull(message = ORDER_ITEM_PRODUCT_ID_IS_REQUIRED)
        @Positive(message = ORDER_ITEM_PRODUCT_ID_IS_INVALID)
        Long productId,

        @NotNull(message = ORDER_ITEM_QUANTITY_IS_REQUIRED)
        @Min(value = 1, message = ORDER_ITEM_QUANTITY_IS_INVALID)
        Integer quantity
) {}
