package com.namnguyen.ecommerce_platform.cart.service;

import com.namnguyen.ecommerce_platform.cart.dto.CartItemRequest;
import com.namnguyen.ecommerce_platform.cart.dto.CartItemResponse;
import com.namnguyen.ecommerce_platform.cart.dto.CartResponse;
import com.namnguyen.ecommerce_platform.cart.entity.Cart;
import com.namnguyen.ecommerce_platform.cart.entity.CartItem;
import com.namnguyen.ecommerce_platform.cart.exception.InvalidQuantityException;
import com.namnguyen.ecommerce_platform.cart.mapper.CartItemMapper;
import com.namnguyen.ecommerce_platform.cart.mapper.CartMapper;
import com.namnguyen.ecommerce_platform.cart.repository.CartItemRepository;
import com.namnguyen.ecommerce_platform.cart.repository.CartRepository;
import com.namnguyen.ecommerce_platform.product.exception.InsufficientStockException;
import com.namnguyen.ecommerce_platform.common.exception.NoResourceFoundException;
import com.namnguyen.ecommerce_platform.product.entity.Product;
import com.namnguyen.ecommerce_platform.product.service.ProductLookupService;
import com.namnguyen.ecommerce_platform.user.entity.User;
import com.namnguyen.ecommerce_platform.user.service.UserLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static com.namnguyen.ecommerce_platform.cart.error.CartErrorMessages.CART_ITEM_UPDATE_QUANTITY_IS_INVALID;
import static com.namnguyen.ecommerce_platform.cart.error.CartErrorMessages.cartItemNotFoundWithProductId;
import static com.namnguyen.ecommerce_platform.product.error.ProductErrorMessages.insufficientStockForProduct;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final UserLookupService userLookUpService;
    private final ProductLookupService productLookUpService;
    private final CartLookupService cartLookupService;

    @Transactional
    public Cart createCartForUser(User user) {
        Cart cart = Cart.builder()
                .user(user)
                .build();

        return cartRepository.save(cart);
    }

    private CartItem getCartItem(Cart cart, Long productId) {
        return cartItemRepository.findByCartIdAndProductId(cart.getId(), productId)
                .orElseThrow(() -> new NoResourceFoundException(cartItemNotFoundWithProductId(productId)));
    }

    private Cart getCartOrCreateIfAbsent(Long userId) {
        User user = userLookUpService.getUserById(userId);
        return cartRepository.findByUserId(userId).orElseGet(() -> createCartForUser(user));
    }

    private void stockCheck(Product product, int quantity) {
        if (quantity > product.getQuantity()) {
            throw new InsufficientStockException(insufficientStockForProduct(product.getName()));
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
        if (quantity < 0) {
            throw new InvalidQuantityException(CART_ITEM_UPDATE_QUANTITY_IS_INVALID);
        }

        Cart cart = cartLookupService.getCartByUserId(userId);
        CartItem item = getCartItem(cart, productId);

        if (quantity == 0) {
            cart.removeItem(item);
        } else {
            Product product = productLookUpService.getProductById(productId);
            stockCheck(product, quantity);
            item.setQuantity(quantity);
        }

        return CartMapper.toResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse removeItem(Long userId, Long productId) {
        Cart cart = cartLookupService.getCartByUserId(userId);
        CartItem item = getCartItem(cart, productId);
        cart.removeItem(item);
        return CartMapper.toResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse clearCart(Long userId) {
        Cart cart = cartLookupService.getCartByUserId(userId);
        cart.getItems().forEach(item -> item.setCart(null));
        cart.getItems().clear();
        return CartMapper.toResponse(cart);
    }
}
