package com.namnguyen.ecommerce_platform.cart.service;

import com.namnguyen.ecommerce_platform.cart.entity.Cart;
import com.namnguyen.ecommerce_platform.cart.repository.CartRepository;
import com.namnguyen.ecommerce_platform.common.exception.NoResourceFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static com.namnguyen.ecommerce_platform.cart.error.CartErrorMessages.cartNotFoundWithUserId;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartLookupService {

    private final CartRepository cartRepository;

    public Cart getCartByUserId(Long userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new NoResourceFoundException(cartNotFoundWithUserId(userId)));

        log.debug("Fetched cart cartId={} userId={}", cart.getId(), userId);

        return cart;
    }
}
