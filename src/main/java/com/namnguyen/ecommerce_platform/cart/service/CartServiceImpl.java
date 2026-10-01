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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static com.namnguyen.ecommerce_platform.cart.error.CartErrorMessages.CART_ITEM_UPDATE_QUANTITY_IS_INVALID;
import static com.namnguyen.ecommerce_platform.cart.error.CartErrorMessages.cartItemNotFoundWithProductId;
import static com.namnguyen.ecommerce_platform.product.error.ProductErrorMessages.insufficientStockForProduct;

@Slf4j
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

        Cart savedCart = cartRepository.save(cart);

        log.info(
                "User cart created cartId={} userId={}",
                savedCart.getId(),
                user.getId()
        );

        return savedCart;
    }

    private CartItem getCartItem(Cart cart, Long productId) {
        CartItem item = cartItemRepository.findByCartIdAndProductId(cart.getId(), productId)
                .orElseThrow(() -> new NoResourceFoundException(cartItemNotFoundWithProductId(productId)));

        log.debug(
                "Fetched cart item cartItemId={} cartId={} userId={}",
                item.getId(),
                cart.getId(),
                cart.getUser().getId()
        );

        return item;
    }

    private Cart getCartOrCreateIfAbsent(Long userId) {
        User user = userLookUpService.getUserById(userId);
        Cart savedCart = cartRepository.findByUserId(userId).orElseGet(() -> createCartForUser(user));

        log.debug(
                "Fetched user cart cartId={} userId={}",
                savedCart.getId(),
                userId
        );

        return savedCart;
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

        log.info(
                "Adding cart item cartId={} productId={} quantity={} userId={}",
                cart.getId(),
                request.productId(),
                request.quantity(),
                userId
        );

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

        CartItem savedItem = cartItemRepository.save(item);

        log.info(
                "Cart item added cartItemId={} cartId={} productId={} quantity={} userId={}",
                savedItem.getId(),
                cart.getId(),
                request.productId(),
                savedItem.getQuantity(),
                userId
        );

        return CartItemMapper.toResponse(savedItem);
    }

    @Override
    @Transactional
    public CartResponse updateItemQuantity(Long userId, Long productId, int quantity) {
        if (quantity < 0) {
            throw new InvalidQuantityException(CART_ITEM_UPDATE_QUANTITY_IS_INVALID);
        }

        Cart cart = cartLookupService.getCartByUserId(userId);

        log.info(
                "Updating cart item quantity cartId={} productId={} quantity={} userId={}",
                cart.getId(),
                productId,
                quantity,
                userId
        );
        CartItem item = getCartItem(cart, productId);

        if (quantity == 0) {
            cart.removeItem(item);
        } else {
            Product product = productLookUpService.getProductById(productId);
            stockCheck(product, quantity);
            item.setQuantity(quantity);
        }

        Cart savedCart = cartRepository.save(cart);

        log.info(
                "Cart item quantity updated cartId={} productId={} quantity={} userId={}",
                savedCart.getId(),
                productId,
                quantity,
                userId
        );
        return CartMapper.toResponse(savedCart);
    }

    @Override
    @Transactional
    public CartResponse removeItem(Long userId, Long productId) {
        Cart cart = cartLookupService.getCartByUserId(userId);

        log.info(
                "Removing cart item cartId={} productId={} userId={}",
                cart.getId(),
                productId,
                userId
        );

        CartItem item = getCartItem(cart, productId);
        cart.removeItem(item);
        Cart savedCart = cartRepository.save(cart);

        log.info(
                "Cart item removed cartId={} productId={} userId={}",
                savedCart.getId(),
                productId,
                userId
        );

        return CartMapper.toResponse(savedCart);
    }

    @Override
    @Transactional
    public CartResponse clearCart(Long userId) {
        Cart cart = cartLookupService.getCartByUserId(userId);

        log.info("Clearing cart cartId={} userId={}", cart.getId(), userId);

        cart.getItems().forEach(item -> item.setCart(null));
        cart.getItems().clear();
        Cart savedCart = cartRepository.save(cart);

        log.info("Cart cleared cartId={} userId={}", savedCart.getId(), userId);

        return CartMapper.toResponse(savedCart);
    }
}
