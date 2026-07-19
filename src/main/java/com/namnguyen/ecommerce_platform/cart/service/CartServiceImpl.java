package com.namnguyen.ecommerce_platform.cart.service;

import com.namnguyen.ecommerce_platform.cart.dto.CartItemRequest;
import com.namnguyen.ecommerce_platform.cart.dto.CartItemResponse;
import com.namnguyen.ecommerce_platform.cart.dto.CartResponse;
import com.namnguyen.ecommerce_platform.cart.entity.Cart;
import com.namnguyen.ecommerce_platform.cart.entity.CartItem;
import com.namnguyen.ecommerce_platform.cart.mapper.CartItemMapper;
import com.namnguyen.ecommerce_platform.cart.mapper.CartMapper;
import com.namnguyen.ecommerce_platform.cart.repository.CartItemRepository;
import com.namnguyen.ecommerce_platform.cart.repository.CartRepository;
import com.namnguyen.ecommerce_platform.common.exception.InsufficientStockException;
import com.namnguyen.ecommerce_platform.common.exception.NoResourceFoundException;
import com.namnguyen.ecommerce_platform.product.entity.Product;
import com.namnguyen.ecommerce_platform.product.service.ProductLookupService;
import com.namnguyen.ecommerce_platform.user.entity.User;
import com.namnguyen.ecommerce_platform.user.service.UserLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final UserLookupService userLookUpService;
    private final ProductLookupService productLookUpService;

    @Transactional
    public Cart createCartForUser(User user) {
        Cart cart = Cart.builder()
                .user(user)
                .build();

        return cartRepository.save(cart);
    }

    private CartItem getCartItem(Cart cart, Long productId) {
        return cartItemRepository.findByCartIdAndProductId(cart.getId(), productId)
                .orElseThrow(() -> new NoResourceFoundException("No item found with product id: " + productId));
    }

    private Cart getCartOrCreateIfAbsent(Long userId) {
        User user = userLookUpService.getUserById(userId);
        return cartRepository.findByUserId(userId).orElseGet(() -> createCartForUser(user));
    }

    private Cart getExistingCartOrThrow(Long userId) {
        userLookUpService.getUserById(userId);
        return cartRepository.findByUserId(userId)
                .orElseThrow(() -> new NoResourceFoundException("Cart not found for user id: " + userId));
    }

    private void stockCheck(Product product, int quantity) {
        if (quantity > product.getQuantity()) {
            throw new InsufficientStockException("Not enough stock for: " + product.getName());
        }
    }

    @Override
    @Transactional
    public CartResponse getCart(Long userId) {
        return CartMapper.toResponse(getCartOrCreateIfAbsent(userId));
    }

        @Override
        @Transactional
        public CartItemResponse addItem(Long userId, CartItemRequest request) {
            Cart cart = getCartOrCreateIfAbsent(userId);
            Product product = productLookUpService.getProductById(request.productId());

            Optional<CartItem> existingItem =
                    cartItemRepository.findByCartIdAndProductId(cart.getId(), product.getId());

            int currentQuantity = existingItem.map(CartItem::getQuantity).orElse(0);

            int newQuantity = request.quantity() + currentQuantity;
            stockCheck(product, newQuantity);

            CartItem item = existingItem.orElseGet(() -> {
                        CartItem newItem = CartItem.builder()
                                .quantity(0)
                                .product(product)
                                .build();

                        cart.addItem(newItem);
                        return newItem;
                    });

            item.setQuantity(newQuantity);

            return CartItemMapper.toResponse(cartItemRepository.save(item));
        }

    @Override
    @Transactional
    public CartResponse updateItemQuantity(Long userId, Long productId, int quantity) {
        Cart cart = getExistingCartOrThrow(userId);
        Product product = productLookUpService.getProductById(productId);
        CartItem item = getCartItem(cart, productId);

        if (quantity <= 0) {
            cart.removeItem(item);
        } else {
            stockCheck(product, quantity);
            item.setQuantity(quantity);
        }

        return CartMapper.toResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse removeItem(Long userId, Long productId) {
        Cart cart = getExistingCartOrThrow(userId);
        CartItem item = getCartItem(cart, productId);
        cart.removeItem(item);
        return CartMapper.toResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse clearCart(Long userId) {
        Cart cart = getExistingCartOrThrow(userId);
        cart.getItems().forEach(item -> item.setCart(null));
        cart.getItems().clear();
        return CartMapper.toResponse(cart);
    }
}
