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

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final UserLookupService userLookUpService;
    private final ProductLookupService productLookUpService;

    private CartItem getCartItem(Long userId, Long productId) {
        Cart cart = getCartOrCreateIfAbsent(userId);
        return cartItemRepository.findByCartIdAndProductId(cart.getId(), productId)
                .orElseThrow(() -> new NoResourceFoundException("No item found with product id: " + productId));
    }

    private Cart getCartOrCreateIfAbsent(Long userId) {
        User user = userLookUpService.getUserById(userId);
        return cartRepository.findByUserId(userId).orElseGet(() -> createCartForUser(user));
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

        CartItem item = cartItemRepository.findByCartIdAndProductId(cart.getId(), product.getId())
                .orElseGet(() -> CartItem.builder()
                        .cart(cart)
                        .quantity(0)
                        .product(product)
                        .build());

        int newQuantity = request.quantity() + item.getQuantity();
        stockCheck(product, newQuantity);

        item.setQuantity(newQuantity);

        return CartItemMapper.toResponse(cartItemRepository.save(item));
    }

    @Override
    @Transactional
    public CartResponse updateItemQuantity(Long userId, Long productId, int quantity) {
        Cart cart = getCartOrCreateIfAbsent(userId);
        Product product = productLookUpService.getProductById(productId);
        CartItem item = getCartItem(userId, productId);

        if (quantity <= 0) {
            cart.getItems().remove(item);
        } else {
            stockCheck(product, quantity);
            item.setQuantity(quantity);
        }

        return CartMapper.toResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse removeItem(Long userId, Long productId) {
        Cart cart = getCartOrCreateIfAbsent(userId);
        CartItem item = getCartItem(userId, productId);
        cart.getItems().remove(item);
        return CartMapper.toResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse clearCart(Long userId) {
        Cart cart = getCartOrCreateIfAbsent(userId);
        cart.getItems().clear();
        return CartMapper.toResponse(cart);
    }

    @Override
    @Transactional
    public Cart createCartForUser(User user) {
        Cart cart = Cart.builder()
                .user(user)
                .build();

        return cartRepository.save(cart);
    }
}
