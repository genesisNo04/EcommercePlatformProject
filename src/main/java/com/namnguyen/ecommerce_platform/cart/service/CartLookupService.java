package com.namnguyen.ecommerce_platform.cart.service;

import com.namnguyen.ecommerce_platform.cart.entity.Cart;
import com.namnguyen.ecommerce_platform.cart.repository.CartRepository;
import com.namnguyen.ecommerce_platform.common.exception.NoResourceFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import static com.namnguyen.ecommerce_platform.cart.error.CartErrorMessages.cartNotFoundWithUserId;

@Service
@RequiredArgsConstructor
public class CartLookupService {

    private final CartRepository cartRepository;

    public Cart getCartByUserId(Long userId) {
        return cartRepository.findByUserId(userId)
                .orElseThrow(() -> new NoResourceFoundException(cartNotFoundWithUserId(userId)));
    }
}
