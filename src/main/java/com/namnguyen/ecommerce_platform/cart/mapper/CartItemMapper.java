package com.namnguyen.ecommerce_platform.cart.mapper;

import com.namnguyen.ecommerce_platform.cart.dto.CartItemResponse;
import com.namnguyen.ecommerce_platform.cart.entity.CartItem;

import java.math.BigDecimal;
import java.util.Locale;

public class CartItemMapper {

    public static CartItemResponse toResponse(CartItem item) {
        return new CartItemResponse(
                item.getProduct().getId(),
                item.getProduct().getName(),
                item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())),
                item.getQuantity()
        );
    }
}
