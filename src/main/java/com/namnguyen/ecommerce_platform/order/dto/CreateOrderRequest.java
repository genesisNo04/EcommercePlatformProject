package com.namnguyen.ecommerce_platform.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

import static com.namnguyen.ecommerce_platform.order.error.OrderErrorMessages.ORDER_IS_EMPTY;

public record CreateOrderRequest(

        @NotEmpty(message = ORDER_IS_EMPTY)
        List<@Valid CreateOrderItemRequest> items
) {}
