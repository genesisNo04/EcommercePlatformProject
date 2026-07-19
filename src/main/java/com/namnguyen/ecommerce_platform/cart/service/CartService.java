package com.namnguyen.ecommerce_platform.cart.service;

import com.namnguyen.ecommerce_platform.cart.dto.CartItemRequest;
import com.namnguyen.ecommerce_platform.cart.dto.CartItemResponse;
import com.namnguyen.ecommerce_platform.cart.dto.CartResponse;
import com.namnguyen.ecommerce_platform.cart.entity.Cart;
import com.namnguyen.ecommerce_platform.user.entity.User;

import java.util.Optional;

public interface CartService {

    CartResponse getCart(Long userId);

    CartItemResponse addItem(Long userId, CartItemRequest request);

    CartResponse updateItemQuantity(Long userId, Long productId, int quantity);

    CartResponse removeItem(Long userId, Long productId);

    CartResponse clearCart(Long userId);
}
