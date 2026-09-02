package com.namnguyen.ecommerce_platform.cart.service;

import com.namnguyen.ecommerce_platform.cart.dto.CartItemRequest;
import com.namnguyen.ecommerce_platform.cart.dto.CartItemResponse;
import com.namnguyen.ecommerce_platform.cart.dto.CartResponse;
import com.namnguyen.ecommerce_platform.cart.entity.Cart;
import com.namnguyen.ecommerce_platform.cart.entity.CartItem;
import com.namnguyen.ecommerce_platform.cart.exception.InvalidQuantityException;
import com.namnguyen.ecommerce_platform.cart.repository.CartItemRepository;
import com.namnguyen.ecommerce_platform.cart.repository.CartRepository;
import com.namnguyen.ecommerce_platform.product.exception.InsufficientStockException;
import com.namnguyen.ecommerce_platform.common.exception.NoResourceFoundException;
import com.namnguyen.ecommerce_platform.product.entity.Product;
import com.namnguyen.ecommerce_platform.product.service.ProductLookupService;
import com.namnguyen.ecommerce_platform.user.entity.User;
import com.namnguyen.ecommerce_platform.user.service.UserLookupService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static com.namnguyen.ecommerce_platform.testutil.messages.CartTestMessages.*;
import static com.namnguyen.ecommerce_platform.testutil.TestDataFactory.*;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CartServiceImplTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private UserLookupService userLookupService;

    @Mock
    private CartLookupService cartLookupService;

    @Mock
    private ProductLookupService productLookupService;

    @InjectMocks
    private CartServiceImpl cartService;

    @Test
    void getCart_whenCartExists_returnsCartResponse() {
        Long firstProductId = 1L;
        Long secondProductId = 2L;
        Long cartId = 10L;
        Long cartItemId = 11L;
        int quantity = 1;

        User user = createUser(1L);
        Product firstProduct = createDefaultProduct(firstProductId);
        Product secondProduct = createDefaultProduct(secondProductId);

        Cart cart = createCartWithItem(
                cartId,
                user,
                firstProduct,
                quantity);

        CartItem cartItem = createCartItem(
                cartItemId,
                cart,
                secondProduct,
                quantity);

        cart.addItem(cartItem);

        when(userLookupService.getUserById(user.getId())).thenReturn(user);
        when(cartRepository.findByUserId(user.getId())).thenReturn(Optional.of(cart));

        CartResponse cartResponse = cartService.getCart(user.getId());

        assertThat(cartResponse).isNotNull();
        assertThat(cartResponse.items()).hasSize(2);

        CartItemResponse firstItemResponse = cartResponse.items().getFirst();
        assertThat(firstItemResponse.productId()).isEqualTo(firstProduct.getId());
        assertThat(firstItemResponse.productName()).isEqualTo(firstProduct.getName());
        assertThat(firstItemResponse.quantity()).isEqualTo(cart.getItems().getFirst().getQuantity());
        assertThat(firstItemResponse.subtotal()).isEqualByComparingTo(firstProduct.getPrice());
        assertThat(firstItemResponse.unitPrice()).isEqualByComparingTo(firstProduct.getPrice());

        CartItemResponse secondItemResponse = cartResponse.items().get(1);
        assertThat(secondItemResponse.productId()).isEqualTo(secondProduct.getId());
        assertThat(secondItemResponse.productName()).isEqualTo(secondProduct.getName());
        assertThat(secondItemResponse.quantity()).isEqualTo(cart.getItems().get(1).getQuantity());
        assertThat(secondItemResponse.subtotal()).isEqualByComparingTo(secondProduct.getPrice());
        assertThat(secondItemResponse.unitPrice()).isEqualByComparingTo(secondProduct.getPrice());

        verify(userLookupService).getUserById(user.getId());
        verify(cartRepository).findByUserId(user.getId());
        verify(cartRepository, never()).save(any(Cart.class));
        verifyNoMoreInteractions(userLookupService);
        verifyNoMoreInteractions(cartRepository);
    }

    @Test
    void getCart_whenCartDoesNotExist_createsNewCartAndReturnsEmptyCartResponse() {
        Long userId = 1L;

        User user = createUser(userId);

        when(userLookupService.getUserById(user.getId())).thenReturn(user);
        when(cartRepository.findByUserId(user.getId())).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> {
            Cart cart = inv.getArgument(0);
            cart.setId(1L);
            cart.setUser(user);
            return cart;
        });

        CartResponse cartResponse = cartService.getCart(user.getId());

        assertThat(cartResponse).isNotNull();
        assertThat(cartResponse.items()).hasSize(0);

        ArgumentCaptor<Cart> cartCaptor = ArgumentCaptor.forClass(Cart.class);
        verify(cartRepository).save(cartCaptor.capture());

        Cart savedCart = cartCaptor.getValue();

        assertThat(savedCart.getUser()).isEqualTo(user);
        assertThat(savedCart.getItems()).isEmpty();

        verify(userLookupService).getUserById(user.getId());
        verify(cartRepository).findByUserId(user.getId());
        verifyNoMoreInteractions(userLookupService);
        verifyNoMoreInteractions(cartRepository);
        verifyNoInteractions(cartItemRepository);
        verifyNoInteractions(productLookupService);
    }

    @Test
    void getCart_userNotFound_throwNoResourceFoundException() {
        Long userId = 999L;

        when(userLookupService.getUserById(userId))
                .thenThrow(new NoResourceFoundException(userNotFoundWithId(userId)));

        NoResourceFoundException ex = assertThrows(
                NoResourceFoundException.class,
                () -> cartService.getCart(userId)
        );

        assertThat(ex).isNotNull();
        assertThat(ex.getMessage()).isEqualTo(userNotFoundWithId(userId));

        verify(userLookupService).getUserById(userId);
        verify(cartRepository, never()).save(any(Cart.class));
        verifyNoMoreInteractions(userLookupService);
        verifyNoInteractions(cartRepository);
        verifyNoInteractions(cartItemRepository);
        verifyNoInteractions(productLookupService);
    }

    @Test
    void addItem_whenProductNotInCart_addsNewCartItem() {
        Long userId = 1L;
        Long cartId = 10L;
        Long cartItemId = 11L;
        Long productId = 12L;
        int quantity = 2;

        User user = createUser(userId
        );
        Product product = createDefaultProduct(productId);

        Cart cart = createCart(cartId, user);

        CartItemRequest cartItemRequest = new CartItemRequest(
                productId,
                quantity
        );

        when(userLookupService.getUserById(user.getId())).thenReturn(user);
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(productLookupService.getProductById(product.getId())).thenReturn(product);
        when(cartItemRepository.findByCartIdAndProductId(cart.getId(), product.getId())).thenReturn(Optional.empty());
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(inv -> {
            CartItem savedItem = inv.getArgument(0);
            savedItem.setId(cartItemId);
            return savedItem;
        });

        CartItemResponse cartItemResponse = cartService.addItem(user.getId(), cartItemRequest);

        assertThat(cartItemResponse).isNotNull();
        assertThat(cartItemResponse.productId()).isEqualTo(product.getId());
        assertThat(cartItemResponse.productName()).isEqualTo(product.getName());
        assertThat(cartItemResponse.unitPrice()).isEqualByComparingTo(product.getPrice());
        assertThat(cartItemResponse.quantity()).isEqualTo(cartItemRequest.quantity());
        assertThat(cartItemResponse.subtotal()).isEqualByComparingTo(product.getPrice().multiply(BigDecimal.valueOf(cartItemRequest.quantity())));

        ArgumentCaptor<CartItem> itemCaptor = ArgumentCaptor.forClass(CartItem.class);
        verify(cartItemRepository).save(itemCaptor.capture());

        CartItem savedItem = itemCaptor.getValue();

        assertThat(savedItem.getCart()).isEqualTo(cart);
        assertThat(savedItem.getProduct()).isEqualTo(product);
        assertThat(savedItem.getQuantity()).isEqualTo(quantity);

        verify(userLookupService).getUserById(userId);
        verify(cartRepository).findByUserId(userId);
        verify(productLookupService).getProductById(product.getId());
        verify(cartItemRepository).findByCartIdAndProductId(cart.getId(), product.getId());

        verifyNoMoreInteractions(userLookupService);
        verifyNoMoreInteractions(cartRepository);
        verifyNoMoreInteractions(productLookupService);
        verifyNoMoreInteractions(cartItemRepository);
    }

    @Test
    void addItem_whenProductAlreadyInCart_increasesQuantity() {
        Long userId = 1L;
        Long cartItemId = 2L;
        Long productId = 3L;
        Long cartId = 4L;
        int initialQuantity = 2;
        int addQuantity = 3;

        User user = createUser(userId);
        Product product = createDefaultProduct(productId);

        Cart cart = createCart(cartId, user);

        CartItem cartItem = createCartItem(
                cartItemId,
                cart,
                product,
                initialQuantity
        );

        CartItemRequest cartItemRequest = new CartItemRequest(
                product.getId(),
                addQuantity
        );

        when(userLookupService.getUserById(user.getId())).thenReturn(user);
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(productLookupService.getProductById(product.getId())).thenReturn(product);
        when(cartItemRepository.findByCartIdAndProductId(cart.getId(), product.getId())).thenReturn(Optional.of(cartItem));
        when(cartItemRepository.save(cartItem)).thenReturn(cartItem);

        CartItemResponse cartItemResponse = cartService.addItem(user.getId(), cartItemRequest);

        assertThat(cartItemResponse).isNotNull();
        assertThat(cartItemResponse.productId()).isEqualTo(product.getId());
        assertThat(cartItemResponse.productName()).isEqualTo(product.getName());
        assertThat(cartItemResponse.unitPrice()).isEqualByComparingTo(product.getPrice());
        assertThat(cartItemResponse.quantity()).isEqualTo(cartItemRequest.quantity() + initialQuantity);
        assertThat(cartItemResponse.subtotal()).isEqualByComparingTo(
                product.getPrice().multiply(BigDecimal.valueOf(cartItemRequest.quantity() + initialQuantity)));

        ArgumentCaptor<CartItem> itemCaptor = ArgumentCaptor.forClass(CartItem.class);
        verify(cartItemRepository).save(itemCaptor.capture());

        CartItem savedItem = itemCaptor.getValue();

        assertThat(savedItem.getCart()).isEqualTo(cart);
        assertThat(savedItem.getProduct()).isEqualTo(product);
        assertThat(savedItem.getQuantity()).isEqualTo(cartItemRequest.quantity() + initialQuantity);

        verify(userLookupService).getUserById(userId);
        verify(cartRepository).findByUserId(userId);
        verify(productLookupService).getProductById(product.getId());
        verify(cartItemRepository).findByCartIdAndProductId(cart.getId(), product.getId());

        verifyNoMoreInteractions(userLookupService);
        verifyNoMoreInteractions(cartRepository);
        verifyNoMoreInteractions(productLookupService);
        verifyNoMoreInteractions(cartItemRepository);
    }

    @Test
    void addItem_whenUserNotFound_throwNoResourceFoundException() {
        Long userId = 1L;
        Long productId = 2L;

        CartItemRequest cartItemRequest = new CartItemRequest(
                productId,
                3
        );

        when(userLookupService.getUserById(userId))
                .thenThrow(new NoResourceFoundException(userNotFoundWithId(userId)));

        NoResourceFoundException ex = assertThrows(
                NoResourceFoundException.class,
                () -> cartService.addItem(userId, cartItemRequest)
        );

        assertThat(ex).isNotNull();
        assertThat(ex.getMessage()).isEqualTo(userNotFoundWithId(userId));

        verify(userLookupService).getUserById(userId);
        verifyNoInteractions(cartRepository);
        verifyNoInteractions(productLookupService);
        verifyNoInteractions(cartItemRepository);
    }

    @Test
    void addItem_whenProductNotFound_throwNoResourceFoundException() {
        Long userId = 1L;
        Long productId = 999L;
        Long cartId = 2L;

        User user = createUser(userId);
        Cart cart = createCart(cartId, user);

        CartItemRequest cartItemRequest = new CartItemRequest(
                productId,
                3
        );

        when(userLookupService.getUserById(userId)).thenReturn(user);
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(productLookupService.getProductById(productId))
                .thenThrow(new NoResourceFoundException(productNotFoundWithId(productId)));

        NoResourceFoundException ex = assertThrows(
                NoResourceFoundException.class,
                () -> cartService.addItem(userId, cartItemRequest)
        );

        assertThat(ex).isNotNull();
        assertThat(ex.getMessage()).isEqualTo(productNotFoundWithId(productId));

        verify(userLookupService).getUserById(userId);
        verify(cartRepository).findByUserId(userId);
        verify(productLookupService).getProductById(productId);
        verifyNoInteractions(cartItemRepository);
    }

    @Test
    void addItem_whenQuantityMoreThanStock_throwInsufficientStockException() {
        Long userId = 1L;
        Long cartId = 2L;
        Long productId = 3L;
        int availableStock = 10;
        int addQuantity = 11;

        User user = createUser(userId);
        Product product = createProduct(
                productId,
                VALID_PRODUCT_NAME,
                VALID_PRODUCT_PRICE,
                availableStock
        );

        Cart cart = createCart(cartId, user);

        CartItemRequest cartItemRequest = new CartItemRequest(
                product.getId(),
                addQuantity
        );

        when(userLookupService.getUserById(user.getId())).thenReturn(user);
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(productLookupService.getProductById(product.getId())).thenReturn(product);
        when(cartItemRepository.findByCartIdAndProductId(cartId, product.getId())).thenReturn(Optional.empty());

        InsufficientStockException ex = assertThrows(
                InsufficientStockException.class,
                () -> cartService.addItem(user.getId(), cartItemRequest)
        );

        assertThat(ex).isNotNull();
        assertThat(ex.getMessage()).isEqualTo(insufficientStock(product.getName()));
        assertThat(cart.getItems()).isEmpty();

        verify(userLookupService).getUserById(userId);
        verify(cartRepository).findByUserId(userId);
        verify(productLookupService).getProductById(product.getId());
        verify(cartItemRepository).findByCartIdAndProductId(cart.getId(), product.getId());
        verify(cartItemRepository, never()).save(any(CartItem.class));
        verifyNoMoreInteractions(cartItemRepository);
    }

    @Test
    void addItem_whenExistingQuantityPlusRequestQuantityExceedsStock_throwsInsufficientStockException() {
        Long userId = 1L;
        Long cartId = 2L;
        Long productId = 3L;
        Long cartItemId = 4L;
        int quantity = 2;
        int addQuantity = 9;
        int availableStock = 10;

        User user = createUser(userId);
        Product product = createProduct(
                productId,
                VALID_PRODUCT_NAME,
                VALID_PRODUCT_PRICE,
                availableStock
        );

        Cart cart = createCart(cartId, user);

        CartItem cartItem = createCartItem(
                cartItemId,
                cart,
                product,
                quantity
        );

        CartItemRequest cartItemRequest = new CartItemRequest(
                product.getId(),
                addQuantity
        );

        when(userLookupService.getUserById(user.getId())).thenReturn(user);
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(productLookupService.getProductById(product.getId())).thenReturn(product);
        when(cartItemRepository.findByCartIdAndProductId(cartId, product.getId())).thenReturn(Optional.of(cartItem));

        InsufficientStockException ex = assertThrows(
                InsufficientStockException.class,
                () -> cartService.addItem(user.getId(), cartItemRequest)
        );

        assertThat(ex).isNotNull();
        assertThat(ex.getMessage()).isEqualTo(insufficientStock(product.getName()));
        assertThat(cartItem.getQuantity()).isEqualTo(quantity);

        verify(userLookupService).getUserById(userId);
        verify(cartRepository).findByUserId(userId);
        verify(productLookupService).getProductById(product.getId());
        verify(cartItemRepository).findByCartIdAndProductId(cart.getId(), product.getId());
        verify(cartItemRepository, never()).save(any(CartItem.class));
        verifyNoMoreInteractions(cartItemRepository);
    }

    @Test
    void addItem_whenCartDoesNotExist_createsCartAndAddsNewItem() {
        Long userId = 1L;
        Long cartId = 2L;
        Long productId = 3L;
        Long cartItemId = 4L;
        int quantity = 2;

        User user = createUser(userId);

        Product product = createDefaultProduct(productId);

        CartItemRequest cartItemRequest = new CartItemRequest(
                productId,
                quantity
        );

        when(userLookupService.getUserById(user.getId())).thenReturn(user);
        when(productLookupService.getProductById(productId)).thenReturn(product);
        when(cartItemRepository.findByCartIdAndProductId(cartId, productId)).thenReturn(Optional.empty());
        when(cartRepository.findByUserId(user.getId())).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> {
            Cart cart = inv.getArgument(0);
            cart.setId(cartId);
            cart.setUser(user);
            return cart;
        });
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(inv -> {
            CartItem cartItem = inv.getArgument(0);
            cartItem.setId(cartItemId);
            return cartItem;
        });

        CartItemResponse cartItemResponse = cartService.addItem(userId, cartItemRequest);

        assertThat(cartItemResponse).isNotNull();
        assertThat(cartItemResponse.productId()).isEqualTo(productId);
        assertThat(cartItemResponse.productName()).isEqualTo(product.getName());
        assertThat(cartItemResponse.quantity()).isEqualTo(quantity);
        assertThat(cartItemResponse.unitPrice()).isEqualByComparingTo(product.getPrice());
        assertThat(cartItemResponse.subtotal()).isEqualByComparingTo(product.getPrice().multiply(BigDecimal.valueOf(quantity)));

        ArgumentCaptor<Cart> cartCaptor = ArgumentCaptor.forClass(Cart.class);
        verify(cartRepository).save(cartCaptor.capture());

        Cart savedCart = cartCaptor.getValue();

        assertThat(savedCart.getItems()).hasSize(1);
        assertThat(savedCart.getUser()).isEqualTo(user);

        ArgumentCaptor<CartItem> itemCaptor = ArgumentCaptor.forClass(CartItem.class);
        verify(cartItemRepository).save(itemCaptor.capture());

        CartItem savedItem = itemCaptor.getValue();

        assertThat(savedItem.getId()).isEqualTo(cartItemId);
        assertThat(savedItem.getCart()).isEqualTo(savedCart);
        assertThat(savedItem.getProduct()).isEqualTo(product);
        assertThat(savedItem.getQuantity()).isEqualTo(quantity);
        assertThat(savedCart.getItems()).contains(savedItem);

        verify(userLookupService).getUserById(user.getId());
        verify(productLookupService).getProductById(productId);
        verify(cartItemRepository).findByCartIdAndProductId(cartId, productId);
        verify(cartRepository).findByUserId(user.getId());
        verifyNoMoreInteractions(userLookupService);
        verifyNoMoreInteractions(productLookupService);
        verifyNoMoreInteractions(cartRepository);
        verifyNoMoreInteractions(cartItemRepository);
    }

    @Test
    void updateItemQuantity_whenRequestIsValid_updateQuantity() {
        Long userId = 1L;
        Long cartItemId = 2L;
        Long productId = 3L;
        Long cartId = 4L;

        User user = createUser(userId);
        Product product = createDefaultProduct(productId);

        Cart cart = createCart(cartId, user);
        int initialQuantity = 2;
        int updateQuantity = 5;

        CartItem cartItem = createCartItem(
                cartItemId,
                cart,
                product,
                initialQuantity);

        cart.addItem(cartItem);

        when(userLookupService.getUserById(userId)).thenReturn(user);
        when(cartLookupService.getCartByUserId(userId)).thenReturn(cart);
        when(productLookupService.getProductById(productId)).thenReturn(product);
        when(cartItemRepository.findByCartIdAndProductId(cartId, productId)).thenReturn(Optional.of(cartItem));

        CartResponse cartResponse = cartService.updateItemQuantity(userId, productId, updateQuantity);

        CartItemResponse itemResponse = cartResponse.items().getFirst();
        assertThat(cartResponse).isNotNull();
        assertThat(itemResponse.productId()).isEqualTo(product.getId());
        assertThat(itemResponse.productName()).isEqualTo(product.getName());
        assertThat(itemResponse.unitPrice()).isEqualByComparingTo(product.getPrice());
        assertThat(itemResponse.quantity()).isEqualTo(updateQuantity);
        assertThat(itemResponse.subtotal()).isEqualByComparingTo(product.getPrice().multiply(BigDecimal.valueOf(updateQuantity)));

        verify(userLookupService).getUserById(userId);
        verify(cartLookupService).getCartByUserId(userId);
        verify(productLookupService).getProductById(productId);
        verify(cartItemRepository).findByCartIdAndProductId(cartId, productId);

        verifyNoMoreInteractions(userLookupService);
        verifyNoMoreInteractions(cartLookupService);
        verifyNoMoreInteractions(productLookupService);
        verifyNoMoreInteractions(cartItemRepository);
    }

    @Test
    void updateItemQuantity_whenUserNotFound_throwNoResourceFoundException() {
        Long userId = 1L;
        Long productId = 2L;
        int updateQuantity = 4;

        when(userLookupService.getUserById(userId))
                .thenThrow(new NoResourceFoundException(userNotFoundWithId(userId)));

        NoResourceFoundException ex = assertThrows(
                NoResourceFoundException.class,
                () -> cartService.updateItemQuantity(userId, productId, updateQuantity)
        );

        assertThat(ex).isNotNull();
        assertThat(ex.getMessage()).isEqualTo(userNotFoundWithId(userId));

        verify(userLookupService).getUserById(userId);
        verifyNoMoreInteractions(cartRepository);
        verifyNoMoreInteractions(productLookupService);
        verifyNoMoreInteractions(cartItemRepository);
    }

    @Test
    void updateItemQuantity_whenCartNotFound_throwsNoResourceFoundException() {
        Long userId = 1L;
        Long productId = 2L;
        int updateQuantity = 4;

        when(cartLookupService.getCartByUserId(userId))
                .thenThrow(new NoResourceFoundException(
                        cartNotFoundWithUserId(userId)
                ));

        NoResourceFoundException ex = assertThrows(
                NoResourceFoundException.class,
                () -> cartService.updateItemQuantity(
                        userId,
                        productId,
                        updateQuantity
                )
        );

        assertThat(ex.getMessage())
                .isEqualTo(cartNotFoundWithUserId(userId));

        verify(cartLookupService).getCartByUserId(userId);

        verifyNoMoreInteractions(cartLookupService);
        verifyNoInteractions(productLookupService);
        verifyNoInteractions(cartItemRepository);
    }

    @Test
    void updateItemQuantity_whenProductNotFound_throwNoResourceFoundException() {
        Long userId = 1L;
        Long productId = 2L;
        int updateQuantity = 4;

        User user = createUser(userId);
        Cart cart = createCart(1L, user);

        when(userLookupService.getUserById(userId)).thenReturn(user);
        when(cartLookupService.getCartByUserId(userId)).thenReturn(cart);
        when(productLookupService.getProductById(productId))
                .thenThrow(new NoResourceFoundException(productNotFoundWithId(productId)));

        NoResourceFoundException ex = assertThrows(
                NoResourceFoundException.class,
                () -> cartService.updateItemQuantity(userId, productId, updateQuantity)
        );

        assertThat(ex).isNotNull();
        assertThat(ex.getMessage()).isEqualTo(productNotFoundWithId(productId));

        verify(userLookupService).getUserById(userId);
        verify(cartLookupService).getCartByUserId(userId);
        verify(productLookupService).getProductById(productId);
        verifyNoMoreInteractions(cartLookupService);
        verifyNoMoreInteractions(productLookupService);
        verifyNoMoreInteractions(cartItemRepository);
    }

    @Test
    void updateItemQuantity_whenCartItemNotFound_throwNoResourceFoundException() {
        Long userId = 1L;
        Long productId = 2L;
        Long cartId = 3L;
        int updateQuantity = 4;

        User user = createUser(userId);
        Cart cart = createCart(cartId, user);
        Product product = createDefaultProduct(productId);

        when(userLookupService.getUserById(userId)).thenReturn(user);
        when(cartLookupService.getCartByUserId(userId)).thenReturn(cart);
        when(productLookupService.getProductById(productId)).thenReturn(product);
        when(cartItemRepository.findByCartIdAndProductId(cartId, productId)).thenReturn(Optional.empty());

        NoResourceFoundException ex = assertThrows(
                NoResourceFoundException.class,
                () -> cartService.updateItemQuantity(userId, productId, updateQuantity)
        );

        assertThat(ex).isNotNull();
        assertThat(ex.getMessage()).isEqualTo(cartItemNotFoundWithProductId(productId));

        verify(userLookupService).getUserById(userId);
        verify(cartLookupService).getCartByUserId(userId);
        verify(productLookupService).getProductById(productId);
        verify(cartItemRepository).findByCartIdAndProductId(cartId, productId);
        verifyNoMoreInteractions(userLookupService);
        verifyNoMoreInteractions(cartLookupService);
        verifyNoMoreInteractions(productLookupService);
        verifyNoMoreInteractions(cartItemRepository);
    }

    @Test
    void updateItemQuantity_whenQuantityLargerThanStock_throwInsufficientStockException() {
        Long userId = 1L;
        Long productId = 2L;
        Long cartId = 3L;
        Long cartItemId = 4L;
        int initialQuantity = 2;
        int updateQuantity = 11;
        int availableStock = 10;

        User user = createUser(userId);
        Cart cart = createCart(cartId, user);
        Product product = createProduct(
                productId,
                VALID_PRODUCT_NAME,
                VALID_PRODUCT_PRICE,
                availableStock
        );

        CartItem cartItem = createCartItem(
                cartItemId,
                cart,
                product,
                initialQuantity);

        cart.addItem(cartItem);

        when(userLookupService.getUserById(userId)).thenReturn(user);
        when(cartLookupService.getCartByUserId(userId)).thenReturn(cart);
        when(productLookupService.getProductById(productId)).thenReturn(product);
        when(cartItemRepository.findByCartIdAndProductId(cartId, productId)).thenReturn(Optional.of(cartItem));

        InsufficientStockException ex = assertThrows(
                InsufficientStockException.class,
                () -> cartService.updateItemQuantity(userId, productId, updateQuantity)
        );

        assertThat(ex).isNotNull();
        assertThat(ex.getMessage()).isEqualTo(insufficientStock(product.getName()));
        assertThat(cartItem.getQuantity()).isEqualTo(initialQuantity);

        verify(userLookupService).getUserById(userId);
        verify(cartLookupService).getCartByUserId(userId);
        verify(productLookupService).getProductById(productId);
        verify(cartItemRepository).findByCartIdAndProductId(cartId, productId);
        verifyNoMoreInteractions(userLookupService);
        verifyNoMoreInteractions(cartLookupService);
        verifyNoMoreInteractions(productLookupService);
        verifyNoMoreInteractions(cartItemRepository);
    }

    @Test
    void updateItemQuantity_whenQuantityIsZero_removesItemFromCart() {
        Long userId = 1L;
        Long firstProductId = 2L;
        Long secondProductId = 3L;
        Long cartId = 4L;
        Long firstCartItemId = 5L;
        Long secondCartItemId = 6L;
        int initialQuantity = 2;
        int updatedQuantity = 0;

        User user = createUser(userId);
        Cart cart = createCart(cartId, user);
        Product firstProduct = createDefaultProduct(firstProductId);
        Product secondProduct = createDefaultProduct(secondProductId);

        CartItem firstCartItem = createCartItem(
                firstCartItemId,
                cart,
                firstProduct,
                initialQuantity);

        CartItem secondCartItem = createCartItem(
                secondCartItemId,
                cart,
                secondProduct,
                initialQuantity);

        cart.addItem(firstCartItem);
        cart.addItem(secondCartItem);

        when(userLookupService.getUserById(userId)).thenReturn(user);
        when(cartLookupService.getCartByUserId(userId)).thenReturn(cart);
        when(productLookupService.getProductById(firstProductId)).thenReturn(firstProduct);
        when(cartItemRepository.findByCartIdAndProductId(cartId, firstProductId)).thenReturn(Optional.of(firstCartItem));

        CartResponse cartResponse = cartService.updateItemQuantity(userId, firstProductId, updatedQuantity);

        CartItemResponse cartItemResponse = cartResponse.items().getFirst();
        assertThat(cartResponse).isNotNull();
        assertThat(cartResponse.items()).hasSize(1);
        assertThat(cartItemResponse.productId()).isEqualTo(secondProductId);
        assertThat(cartItemResponse.productName()).isEqualTo(secondCartItem.getProduct().getName());
        assertThat(cartItemResponse.quantity()).isEqualTo(secondCartItem.getQuantity());
        assertThat(firstCartItem.getCart()).isNull();
        assertThat(secondCartItem.getCart()).isNotNull();
        assertThat(cart.getItems()).doesNotContain(firstCartItem);
        assertThat(cart.getItems()).contains(secondCartItem);

        verify(userLookupService).getUserById(userId);
        verify(cartLookupService).getCartByUserId(userId);
        verify(productLookupService).getProductById(firstProductId);
        verify(cartItemRepository).findByCartIdAndProductId(cartId, firstProductId);
        verifyNoMoreInteractions(userLookupService);
        verifyNoMoreInteractions(cartRepository);
        verifyNoMoreInteractions(productLookupService);
        verifyNoMoreInteractions(cartItemRepository);
    }


    @Test
    void updateItemQuantity_whenQuantityIsNegative_throwsInvalidQuantityException() {
        Long userId = 1L;
        Long productId = 2L;
        int updatedQuantity = -1;

        InvalidQuantityException ex = assertThrows(
                InvalidQuantityException.class,
                () -> cartService.updateItemQuantity(
                        userId,
                        productId,
                        updatedQuantity
                )
        );

        assertThat(ex.getMessage())
                .isEqualTo(CART_ITEM_UPDATE_QUANTITY_IS_INVALID);

        verifyNoInteractions(userLookupService);
        verifyNoInteractions(cartLookupService);
        verifyNoInteractions(productLookupService);
        verifyNoInteractions(cartItemRepository);
    }

    @Test
    void removeItem_whenRequestIsValid_returnCartResponse() {
        Long userId = 1L;
        Long firstProductId = 2L;
        Long secondProductId = 3L;
        Long cartId = 4L;
        Long firstCartItemId = 5L;
        Long secondCartItemId = 6L;
        int initialQuantity = 2;

        User user = createUser(userId);
        Cart cart = createCart(cartId, user);
        Product firstProduct = createDefaultProduct(firstProductId);
        Product secondProduct = createDefaultProduct(secondProductId);

        CartItem firstCartItem = createCartItem(
                firstCartItemId,
                cart,
                firstProduct,
                initialQuantity);

        CartItem secondCartItem = createCartItem(
                secondCartItemId,
                cart,
                secondProduct,
                initialQuantity);

        cart.addItem(firstCartItem);
        cart.addItem(secondCartItem);

        when(userLookupService.getUserById(userId)).thenReturn(user);
        when(cartLookupService.getCartByUserId(userId)).thenReturn(cart);
        when(cartItemRepository.findByCartIdAndProductId(cartId, firstProductId)).thenReturn(Optional.of(firstCartItem));

        CartResponse cartResponse = cartService.removeItem(userId, firstProductId);
        CartItemResponse cartItemResponse = cartResponse.items().getFirst();

        assertThat(cartResponse).isNotNull();
        assertThat(cartResponse.items()).hasSize(1);
        assertThat(cartItemResponse.productId()).isEqualTo(secondProductId);
        assertThat(cartItemResponse.productName()).isEqualTo(secondCartItem.getProduct().getName());
        assertThat(cartItemResponse.quantity()).isEqualTo(secondCartItem.getQuantity());
        assertThat(firstCartItem.getCart()).isNull();
        assertThat(secondCartItem.getCart()).isNotNull();
        assertThat(cart.getItems()).doesNotContain(firstCartItem);
        assertThat(cart.getItems()).contains(secondCartItem);

        verify(userLookupService).getUserById(userId);
        verify(cartLookupService).getCartByUserId(userId);
        verify(cartItemRepository).findByCartIdAndProductId(cartId, firstProductId);
        verifyNoMoreInteractions(userLookupService);
        verifyNoMoreInteractions(cartLookupService);
        verifyNoMoreInteractions(productLookupService);
        verifyNoMoreInteractions(cartItemRepository);
    }

    @Test
    void removeItem_userNotExists_throwNoResourceFoundException() {
        Long userId = 999L;
        Long productId = 1L;

        when(userLookupService.getUserById(userId))
                .thenThrow(new NoResourceFoundException(userNotFoundWithId(userId)));

        NoResourceFoundException ex = assertThrows(
                NoResourceFoundException.class,
                () -> cartService.removeItem(userId, productId)
        );

        assertThat(ex).isNotNull();
        assertThat(ex.getMessage()).isEqualTo(userNotFoundWithId(userId));

        verify(userLookupService).getUserById(userId);
        verifyNoMoreInteractions(userLookupService);
        verifyNoInteractions(cartLookupService);
        verifyNoInteractions(productLookupService);
        verifyNoInteractions(cartItemRepository);
    }

    @Test
    void removeItem_itemNotExists_throwNoResourceFoundException() {
        Long userId = 1L;
        Long cartId = 2L;
        Long productId = 3L;

        User user = createUser(userId);
        Cart cart = createCart(cartId, user);

        when(userLookupService.getUserById(userId)).thenReturn(user);
        when(cartLookupService.getCartByUserId(userId)).thenReturn(cart);
        when(cartItemRepository.findByCartIdAndProductId(cartId, productId)).thenReturn(Optional.empty());

        NoResourceFoundException ex = assertThrows(
                NoResourceFoundException.class,
                () -> cartService.removeItem(userId, productId)
        );

        assertThat(ex).isNotNull();
        assertThat(ex.getMessage()).isEqualTo(cartItemNotFoundWithProductId(productId));

        verify(userLookupService).getUserById(userId);
        verify(cartLookupService).getCartByUserId(userId);
        verify(cartItemRepository).findByCartIdAndProductId(cartId, productId);
        verifyNoMoreInteractions(cartLookupService);
        verifyNoMoreInteractions(cartLookupService);
        verifyNoMoreInteractions(cartItemRepository);
        verifyNoInteractions(productLookupService);
    }

    @Test
    void removeItem_whenCartNotFound_throwNoResourceFoundException() {
        Long userId = 1L;
        Long productId = 2L;

        User user = createUser(userId);

        when(userLookupService.getUserById(userId)).thenReturn(user);
        when(cartLookupService.getCartByUserId(userId)).thenThrow(
                new NoResourceFoundException(cartNotFoundWithUserId(userId))
        );

        NoResourceFoundException ex = assertThrows(
                NoResourceFoundException.class,
                () -> cartService.removeItem(userId, productId)
        );

        assertThat(ex).isNotNull();
        assertThat(ex.getMessage()).isEqualTo(cartNotFoundWithUserId(userId));

        verify(userLookupService).getUserById(userId);
        verify(cartLookupService).getCartByUserId(userId);

        verifyNoMoreInteractions(userLookupService);
        verifyNoMoreInteractions(cartLookupService);
        verifyNoMoreInteractions(productLookupService);
        verifyNoMoreInteractions(cartItemRepository);
    }

    @Test
    void clearCart_whenCartExists_clearsItemsAndReturnsEmptyCartResponse() {
        Long userId = 1L;
        Long firstProductId = 2L;
        Long secondProductId = 3L;
        Long cartId = 4L;
        Long firstCartItemId = 5L;
        Long secondCartItemId = 6L;
        int initialQuantity = 2;

        User user = createUser(userId);
        Cart cart = createCart(cartId, user);
        Product firstProduct = createDefaultProduct(firstProductId);
        Product secondProduct = createDefaultProduct(secondProductId);

        CartItem firstCartItem = createCartItem(
                firstCartItemId,
                cart,
                firstProduct,
                initialQuantity);

        CartItem secondCartItem = createCartItem(
                secondCartItemId,
                cart,
                secondProduct,
                initialQuantity);

        cart.addItem(firstCartItem);
        cart.addItem(secondCartItem);

        assertThat(cart.getItems().size()).isEqualTo(2);

        when(userLookupService.getUserById(userId)).thenReturn(user);
        when(cartLookupService.getCartByUserId(userId)).thenReturn(cart);

        CartResponse cartResponse = cartService.clearCart(userId);

        assertThat(cartResponse).isNotNull();
        assertThat(cartResponse.items()).hasSize(0);
        assertThat(cart.getItems()).doesNotContain(firstCartItem);
        assertThat(cart.getItems()).doesNotContain(secondCartItem);
        assertThat(firstCartItem.getCart()).isNull();
        assertThat(secondCartItem.getCart()).isNull();

        verify(userLookupService).getUserById(userId);
        verify(cartLookupService).getCartByUserId(userId);
        verifyNoMoreInteractions(userLookupService);
        verifyNoMoreInteractions(cartLookupService);
        verifyNoInteractions(productLookupService);
        verifyNoInteractions(cartItemRepository);
    }

    @Test
    void clearCart_userNotExists_throwNoResourceFoundException() {
        Long userId = 999L;

        when(userLookupService.getUserById(userId))
                .thenThrow(new NoResourceFoundException(userNotFoundWithId(userId)));

        NoResourceFoundException ex = assertThrows(
                NoResourceFoundException.class,
                () -> cartService.clearCart(userId)
        );

        assertThat(ex).isNotNull();
        assertThat(ex.getMessage()).isEqualTo(userNotFoundWithId(userId));

        verify(userLookupService).getUserById(userId);
        verifyNoMoreInteractions(userLookupService);
        verifyNoInteractions(cartLookupService);
        verifyNoInteractions(productLookupService);
        verifyNoInteractions(cartItemRepository);
    }

    @Test
    void clearCart_whenCartNotFound_throwNoResourceFoundException() {
        Long userId = 1L;

        User user = createUser(userId);

        when(userLookupService.getUserById(userId)).thenReturn(user);
        when(cartLookupService.getCartByUserId(userId)).thenThrow(new NoResourceFoundException(
                cartNotFoundWithUserId(userId))
        );

        NoResourceFoundException ex = assertThrows(
                NoResourceFoundException.class,
                () -> cartService.clearCart(userId)
        );

        assertThat(ex).isNotNull();
        assertThat(ex.getMessage()).isEqualTo(cartNotFoundWithUserId(userId));

        verify(userLookupService).getUserById(userId);
        verify(cartLookupService).getCartByUserId(userId);

        verifyNoMoreInteractions(userLookupService);
        verifyNoMoreInteractions(cartLookupService);
        verifyNoMoreInteractions(productLookupService);
        verifyNoMoreInteractions(cartItemRepository);
    }
}
