package com.namnguyen.ecommerce_platform.cart.mapper;

import com.namnguyen.ecommerce_platform.cart.dto.CartItemResponse;
import com.namnguyen.ecommerce_platform.cart.dto.CartResponse;
import com.namnguyen.ecommerce_platform.cart.entity.Cart;

import java.math.BigDecimal;
import java.util.List;

public class CartMapper {

    public static CartResponse toResponse(Cart cart) {
        List<CartItemResponse> items = cart
                .getItems()
                .stream()
                .map(CartItemMapper::toResponse)
                .toList();

        BigDecimal total = cart
                .getItems()
                .stream()
                .map(item -> item.getProduct().getPrice()
                        .multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CartResponse(items, total);
    }
}
