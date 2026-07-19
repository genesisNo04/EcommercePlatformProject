package com.namnguyen.ecommerce_platform.cart;

import com.namnguyen.ecommerce_platform.cart.dto.CartItemRequest;
import com.namnguyen.ecommerce_platform.cart.dto.CartItemResponse;
import com.namnguyen.ecommerce_platform.cart.dto.CartResponse;
import com.namnguyen.ecommerce_platform.cart.entity.Cart;
import com.namnguyen.ecommerce_platform.cart.entity.CartItem;
import com.namnguyen.ecommerce_platform.cart.repository.CartItemRepository;
import com.namnguyen.ecommerce_platform.cart.repository.CartRepository;
import com.namnguyen.ecommerce_platform.cart.service.CartServiceImpl;
import com.namnguyen.ecommerce_platform.common.exception.InsufficientStockException;
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
    private ProductLookupService productLookupService;

    @InjectMocks
    private CartServiceImpl cartService;

    private static final String NO_RESOURCE_FOUND_EXCEPTION_USER_MESSAGE = "User not found with id: ";
    private static final String NO_RESOURCE_FOUND_EXCEPTION_CART_MESSAGE = "Cart not found for user id: ";
    private static final String NO_RESOURCE_FOUND_EXCEPTION_PRODUCT_MESSAGE = "Product not found with id: ";
    private static final String NO_RESOURCE_FOUND_EXCEPTION_ITEM_MESSAGE = "No item found with product id: ";
    private static final String INSUFFICIENT_STOCK_EXCEPTION_MESSAGE = "Not enough stock for: ";

    @Test
    void getCart_whenCartExists_returnsCartResponse() {
        Long productId = 1L;
        Long productId1 = 2L;
        Long cartId = 10L;
        Long cartItemId = 11L;

        User user = createUser(1L);
        Product product = createProduct(
                productId,
                "PS5",
                BigDecimal.valueOf(499.99),
                10
        );
        Product product1 = createProduct(
                productId1,
                "XBOX",
                BigDecimal.valueOf(450.99),
                12
        );
        Cart cart = createCartWithItem(
                cartId,
                user,
                product,
                1);

        CartItem item = createCartItem(
                cartItemId,
                cart,
                product1,
                1);

        cart.addItem(item);

        when(userLookupService.getUserById(user.getId())).thenReturn(user);
        when(cartRepository.findByUserId(user.getId())).thenReturn(Optional.of(cart));

        CartResponse response = cartService.getCart(user.getId());

        assertThat(response).isNotNull();
        assertThat(response.items()).hasSize(2);

        CartItemResponse firstItemResponse = response.items().getFirst();
        assertThat(firstItemResponse.productId()).isEqualTo(product.getId());
        assertThat(firstItemResponse.productName()).isEqualTo(product.getName());
        assertThat(firstItemResponse.quantity()).isEqualTo(cart.getItems().getFirst().getQuantity());
        assertThat(firstItemResponse.subtotal()).isEqualByComparingTo(product.getPrice());
        assertThat(firstItemResponse.unitPrice()).isEqualByComparingTo(product.getPrice());

        CartItemResponse secondItemResponse = response.items().get(1);
        assertThat(secondItemResponse.productId()).isEqualTo(product1.getId());
        assertThat(secondItemResponse.productName()).isEqualTo(product1.getName());
        assertThat(secondItemResponse.quantity()).isEqualTo(cart.getItems().get(1).getQuantity());
        assertThat(secondItemResponse.subtotal()).isEqualByComparingTo(product1.getPrice());
        assertThat(secondItemResponse.unitPrice()).isEqualByComparingTo(product1.getPrice());


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

        CartResponse response = cartService.getCart(user.getId());

        assertThat(response).isNotNull();
        assertThat(response.items()).hasSize(0);

        ArgumentCaptor<Cart> captor = ArgumentCaptor.forClass(Cart.class);
        verify(cartRepository).save(captor.capture());

        Cart saveCart = captor.getValue();

        assertThat(saveCart.getUser()).isEqualTo(user);
        assertThat(saveCart.getItems()).isEmpty();

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
                .thenThrow(new NoResourceFoundException(NO_RESOURCE_FOUND_EXCEPTION_USER_MESSAGE + userId));

        NoResourceFoundException ex = assertThrows(
                NoResourceFoundException.class,
                () -> cartService.getCart(userId)
        );

        assertThat(ex).isNotNull();
        assertThat(ex.getMessage()).isEqualTo(NO_RESOURCE_FOUND_EXCEPTION_USER_MESSAGE + userId);

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

        User user = createUser(userId
        );
        Product product = createProduct(
                productId,
                "PS5",
                BigDecimal.valueOf(499.99),
                10
        );

        Cart cart = createCart(cartId, user);

        CartItemRequest request = new CartItemRequest(
            product.getId(),
            2
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

        CartItemResponse response = cartService.addItem(user.getId(), request);

        assertThat(response).isNotNull();
        assertThat(response.productId()).isEqualTo(product.getId());
        assertThat(response.productName()).isEqualTo(product.getName());
        assertThat(response.unitPrice()).isEqualByComparingTo(product.getPrice());
        assertThat(response.quantity()).isEqualTo(request.quantity());
        assertThat(response.quantity()).isEqualTo(2);
        assertThat(response.subtotal()).isEqualByComparingTo(product.getPrice().multiply(BigDecimal.valueOf(request.quantity())));

        ArgumentCaptor<CartItem> itemCaptor = ArgumentCaptor.forClass(CartItem.class);
        verify(cartItemRepository).save(itemCaptor.capture());

        CartItem savedItem = itemCaptor.getValue();

        assertThat(savedItem.getCart()).isEqualTo(cart);
        assertThat(savedItem.getProduct()).isEqualTo(product);
        assertThat(savedItem.getQuantity()).isEqualTo(2);

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

        User user = createUser(userId);
        Product product = createProduct(
                productId,
                "PS5",
                BigDecimal.valueOf(499.99),
                10
        );

        Cart cart = createCart(cartId, user);

        CartItem item = new CartItem(
                cartItemId,
                cart,
                product,
                initialQuantity);

        CartItemRequest request = new CartItemRequest(
                product.getId(),
                3
        );

        when(userLookupService.getUserById(user.getId())).thenReturn(user);
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(productLookupService.getProductById(product.getId())).thenReturn(product);
        when(cartItemRepository.findByCartIdAndProductId(cart.getId(), product.getId())).thenReturn(Optional.of(item));
        when(cartItemRepository.save(item)).thenReturn(item);

        CartItemResponse response = cartService.addItem(user.getId(), request);

        assertThat(response).isNotNull();
        assertThat(response.productId()).isEqualTo(product.getId());
        assertThat(response.productName()).isEqualTo(product.getName());
        assertThat(response.unitPrice()).isEqualByComparingTo(product.getPrice());
        assertThat(response.quantity()).isEqualTo(request.quantity() + initialQuantity);
        assertThat(response.subtotal()).isEqualByComparingTo(
                product.getPrice().multiply(BigDecimal.valueOf(request.quantity() + initialQuantity)));

        ArgumentCaptor<CartItem> itemCaptor = ArgumentCaptor.forClass(CartItem.class);
        verify(cartItemRepository).save(itemCaptor.capture());

        CartItem savedItem = itemCaptor.getValue();

        assertThat(savedItem.getCart()).isEqualTo(cart);
        assertThat(savedItem.getProduct()).isEqualTo(product);
        assertThat(savedItem.getQuantity()).isEqualTo(request.quantity() + initialQuantity);

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

        CartItemRequest request = new CartItemRequest(
                productId,
                3
        );

        when(userLookupService.getUserById(userId))
                .thenThrow(new NoResourceFoundException(NO_RESOURCE_FOUND_EXCEPTION_USER_MESSAGE + userId));

        NoResourceFoundException ex = assertThrows(
                NoResourceFoundException.class,
                () -> cartService.addItem(userId, request)
        );

        assertThat(ex).isNotNull();
        assertThat(ex.getMessage()).isEqualTo(NO_RESOURCE_FOUND_EXCEPTION_USER_MESSAGE + userId);

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

        CartItemRequest request = new CartItemRequest(
                productId,
                3
        );

        when(userLookupService.getUserById(userId)).thenReturn(user);
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(productLookupService.getProductById(productId))
                .thenThrow(new NoResourceFoundException(NO_RESOURCE_FOUND_EXCEPTION_PRODUCT_MESSAGE + productId));

        NoResourceFoundException ex = assertThrows(
                NoResourceFoundException.class,
                () -> cartService.addItem(userId, request)
        );

        assertThat(ex).isNotNull();
        assertThat(ex.getMessage()).isEqualTo(NO_RESOURCE_FOUND_EXCEPTION_PRODUCT_MESSAGE + productId);

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

        User user = createUser(userId);
        Product product = createProduct(
                productId,
                "PS5",
                BigDecimal.valueOf(499.99),
                10
        );

        Cart cart = createCart(cartId, user);

        CartItemRequest request = new CartItemRequest(
                product.getId(),
                11
        );

        when(userLookupService.getUserById(user.getId())).thenReturn(user);
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(productLookupService.getProductById(product.getId())).thenReturn(product);
        when(cartItemRepository.findByCartIdAndProductId(cartId, product.getId())).thenReturn(Optional.empty());

        InsufficientStockException ex = assertThrows(
                InsufficientStockException.class,
                () -> cartService.addItem(user.getId(), request)
        );

        assertThat(ex).isNotNull();
        assertThat(ex.getMessage()).isEqualTo(INSUFFICIENT_STOCK_EXCEPTION_MESSAGE + product.getName());
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

        User user = createUser(userId);
        Product product = createProduct(
                productId,
                "PS5",
                BigDecimal.valueOf(499.99),
                10
        );

        Cart cart = createCart(cartId, user);

        CartItem item = createCartItem(
                cartItemId,
                cart,
                product,
                quantity
        );

        CartItemRequest request = new CartItemRequest(
                product.getId(),
                addQuantity
        );

        when(userLookupService.getUserById(user.getId())).thenReturn(user);
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(productLookupService.getProductById(product.getId())).thenReturn(product);
        when(cartItemRepository.findByCartIdAndProductId(cartId, product.getId())).thenReturn(Optional.of(item));

        InsufficientStockException ex = assertThrows(
                InsufficientStockException.class,
                () -> cartService.addItem(user.getId(), request)
        );

        assertThat(ex).isNotNull();
        assertThat(ex.getMessage()).isEqualTo(INSUFFICIENT_STOCK_EXCEPTION_MESSAGE + product.getName());
        assertThat(item.getQuantity()).isEqualTo(quantity);

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

        Product product = createProduct(
                productId,
                "PS5",
                BigDecimal.valueOf(499.99),
                10
        );

        CartItemRequest request = new CartItemRequest(
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
            CartItem item = inv.getArgument(0);
            item.setId(cartItemId);
            return item;
        });

        CartItemResponse response = cartService.addItem(userId, request);

        assertThat(response).isNotNull();
        assertThat(response.productId()).isEqualTo(productId);
        assertThat(response.productName()).isEqualTo(product.getName());
        assertThat(response.quantity()).isEqualTo(quantity);
        assertThat(response.unitPrice()).isEqualByComparingTo(product.getPrice());
        assertThat(response.subtotal()).isEqualByComparingTo(product.getPrice().multiply(BigDecimal.valueOf(quantity)));

        ArgumentCaptor<Cart> captor = ArgumentCaptor.forClass(Cart.class);
        verify(cartRepository).save(captor.capture());

        Cart savedCart = captor.getValue();

        assertThat(savedCart.getItems()).hasSize(1);
        assertThat(savedCart.getUser()).isEqualTo(user);

        ArgumentCaptor<CartItem> captorItem = ArgumentCaptor.forClass(CartItem.class);
        verify(cartItemRepository).save(captorItem.capture());

        CartItem savedItem = captorItem.getValue();

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
        Product product = createProduct(
                productId,
                "PS5",
                BigDecimal.valueOf(499.99),
                10
        );

        Cart cart = createCart(cartId, user);
        int initialQuantity = 2;
        int updateQuantity = 5;

        CartItem item = new CartItem(
                cartItemId,
                cart,
                product,
                initialQuantity);

        cart.addItem(item);

        when(userLookupService.getUserById(userId)).thenReturn(user);
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(productLookupService.getProductById(productId)).thenReturn(product);
        when(cartItemRepository.findByCartIdAndProductId(cartId, productId)).thenReturn(Optional.of(item));

        CartResponse response = cartService.updateItemQuantity(userId, productId, updateQuantity);

        CartItemResponse firstItem = response.items().getFirst();
        assertThat(response).isNotNull();
        assertThat(firstItem.productId()).isEqualTo(product.getId());
        assertThat(firstItem.productName()).isEqualTo(product.getName());
        assertThat(firstItem.unitPrice()).isEqualByComparingTo(product.getPrice());
        assertThat(firstItem.quantity()).isEqualTo(updateQuantity);
        assertThat(firstItem.subtotal()).isEqualByComparingTo(product.getPrice().multiply(BigDecimal.valueOf(updateQuantity)));

        verify(userLookupService).getUserById(userId);
        verify(cartRepository).findByUserId(userId);
        verify(productLookupService).getProductById(productId);
        verify(cartItemRepository).findByCartIdAndProductId(cartId, productId);

        verifyNoMoreInteractions(userLookupService);
        verifyNoMoreInteractions(cartRepository);
        verifyNoMoreInteractions(productLookupService);
        verifyNoMoreInteractions(cartItemRepository);
    }

    @Test
    void updateItemQuantity_whenUserNotFound_throwNoResourceFoundException() {
        Long userId = 1L;
        Long productId = 2L;
        int updateQuantity = 4;

        when(userLookupService.getUserById(userId))
                .thenThrow(new NoResourceFoundException(NO_RESOURCE_FOUND_EXCEPTION_USER_MESSAGE + userId));

        NoResourceFoundException ex = assertThrows(
                NoResourceFoundException.class,
                () -> cartService.updateItemQuantity(userId, productId, updateQuantity)
        );

        assertThat(ex).isNotNull();
        assertThat(ex.getMessage()).isEqualTo(NO_RESOURCE_FOUND_EXCEPTION_USER_MESSAGE + userId);

        verify(userLookupService).getUserById(userId);
        verifyNoMoreInteractions(cartRepository);
        verifyNoMoreInteractions(productLookupService);
        verifyNoMoreInteractions(cartItemRepository);
    }

    @Test
    void updateItemQuantity_whenCartNotFound_throwNoResourceFoundException() {
        Long userId = 1L;
        Long productId = 2L;
        int updateQuantity = 4;

        User user = createUser(userId);

        when(userLookupService.getUserById(userId)).thenReturn(user);
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.empty());

        NoResourceFoundException ex = assertThrows(
                NoResourceFoundException.class,
                () -> cartService.updateItemQuantity(userId, productId, updateQuantity)
        );

        assertThat(ex).isNotNull();
        assertThat(ex.getMessage()).isEqualTo(NO_RESOURCE_FOUND_EXCEPTION_CART_MESSAGE + userId);

        verify(userLookupService).getUserById(userId);
        verify(cartRepository).findByUserId(userId);

        verifyNoMoreInteractions(userLookupService);
        verifyNoMoreInteractions(cartRepository);
        verifyNoMoreInteractions(productLookupService);
        verifyNoMoreInteractions(cartItemRepository);
    }

    @Test
    void updateItemQuantity_whenProductNotFound_throwNoResourceFoundException() {
        Long userId = 1L;
        Long productId = 2L;
        int updateQuantity = 4;

        User user = createUser(userId);
        Cart cart = createCart(1L, user);

        when(userLookupService.getUserById(userId)).thenReturn(user);
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(productLookupService.getProductById(productId))
                .thenThrow(new NoResourceFoundException(NO_RESOURCE_FOUND_EXCEPTION_PRODUCT_MESSAGE + productId));

        NoResourceFoundException ex = assertThrows(
                NoResourceFoundException.class,
                () -> cartService.updateItemQuantity(userId, productId, updateQuantity)
        );

        assertThat(ex).isNotNull();
        assertThat(ex.getMessage()).isEqualTo(NO_RESOURCE_FOUND_EXCEPTION_PRODUCT_MESSAGE + productId);

        verify(userLookupService).getUserById(userId);
        verify(cartRepository).findByUserId(userId);
        verify(productLookupService).getProductById(productId);
        verifyNoMoreInteractions(cartRepository);
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
        Product product = createProduct(
                productId,
                "PS5",
                BigDecimal.valueOf(499.99),
                10
        );

        when(userLookupService.getUserById(userId)).thenReturn(user);
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(productLookupService.getProductById(productId)).thenReturn(product);
        when(cartItemRepository.findByCartIdAndProductId(cartId, productId)).thenReturn(Optional.empty());

        NoResourceFoundException ex = assertThrows(
                NoResourceFoundException.class,
                () -> cartService.updateItemQuantity(userId, productId, updateQuantity)
        );

        assertThat(ex).isNotNull();
        assertThat(ex.getMessage()).isEqualTo(NO_RESOURCE_FOUND_EXCEPTION_ITEM_MESSAGE + productId);

        verify(userLookupService).getUserById(userId);
        verify(cartRepository).findByUserId(userId);
        verify(productLookupService).getProductById(productId);
        verify(cartItemRepository).findByCartIdAndProductId(cartId, productId);
        verifyNoMoreInteractions(userLookupService);
        verifyNoMoreInteractions(cartRepository);
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

        User user = createUser(userId);
        Cart cart = createCart(cartId, user);
        Product product = createProduct(
                productId,
                "PS5",
                BigDecimal.valueOf(499.99),
                10
        );

        CartItem item = new CartItem(
                cartItemId,
                cart,
                product,
                initialQuantity);

        cart.addItem(item);

        when(userLookupService.getUserById(userId)).thenReturn(user);
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(productLookupService.getProductById(productId)).thenReturn(product);
        when(cartItemRepository.findByCartIdAndProductId(cartId, productId)).thenReturn(Optional.of(item));

        InsufficientStockException ex = assertThrows(
                InsufficientStockException.class,
                () -> cartService.updateItemQuantity(userId, productId, updateQuantity)
        );

        assertThat(ex).isNotNull();
        assertThat(ex.getMessage()).isEqualTo(INSUFFICIENT_STOCK_EXCEPTION_MESSAGE + product.getName());
        assertThat(item.getQuantity()).isEqualTo(initialQuantity);

        verify(userLookupService).getUserById(userId);
        verify(cartRepository).findByUserId(userId);
        verify(productLookupService).getProductById(productId);
        verify(cartItemRepository).findByCartIdAndProductId(cartId, productId);
        verifyNoMoreInteractions(userLookupService);
        verifyNoMoreInteractions(cartRepository);
        verifyNoMoreInteractions(productLookupService);
        verifyNoMoreInteractions(cartItemRepository);
    }

    @Test
    void updateItemQuantity_whenQuantityIsZero_removesItemFromCart() {
        Long userId = 1L;
        Long productId = 2L;
        Long productId1 = 3L;
        Long cartId = 4L;
        Long cartItemId = 5L;
        Long cartItemId1 = 6L;
        int initialQuantity = 2;
        int updatedQuantity = 0;

        User user = createUser(userId);
        Cart cart = createCart(cartId, user);
        Product product = createProduct(
                productId,
                "PS5",
                BigDecimal.valueOf(499.99),
                10
        );

        Product product1 = createProduct(
                productId1,
                "XBOX",
                BigDecimal.valueOf(450.99),
                10
        );

        CartItem item = new CartItem(
                cartItemId,
                cart,
                product,
                initialQuantity);

        CartItem item1 = new CartItem(
                cartItemId1,
                cart,
                product1,
                initialQuantity);

        cart.addItem(item);
        cart.addItem(item1);

        when(userLookupService.getUserById(userId)).thenReturn(user);
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(productLookupService.getProductById(productId)).thenReturn(product);
        when(cartItemRepository.findByCartIdAndProductId(cartId, productId)).thenReturn(Optional.of(item));

        CartResponse response = cartService.updateItemQuantity(userId, productId, updatedQuantity);

        CartItemResponse firstItem = response.items().getFirst();
        assertThat(response).isNotNull();
        assertThat(response.items()).hasSize(1);
        assertThat(firstItem.productName()).isEqualTo(item1.getProduct().getName());
        assertThat(firstItem.quantity()).isEqualTo(item1.getQuantity());
        assertThat(item.getCart()).isNull();
        assertThat(item1.getCart()).isNotNull();
        assertThat(cart.getItems()).doesNotContain(item);
        assertThat(cart.getItems()).contains(item1);

        verify(userLookupService).getUserById(userId);
        verify(cartRepository).findByUserId(userId);
        verify(productLookupService).getProductById(productId);
        verify(cartItemRepository).findByCartIdAndProductId(cartId, productId);
        verifyNoMoreInteractions(userLookupService);
        verifyNoMoreInteractions(cartRepository);
        verifyNoMoreInteractions(productLookupService);
        verifyNoMoreInteractions(cartItemRepository);
    }


    @Test
    void updateItemQuantity_whenQuantityIsNegative_removesItemFromCart() {
        Long userId = 1L;
        Long productId = 2L;
        Long productId1 = 3L;
        Long cartId = 4L;
        Long cartItemId = 5L;
        Long cartItemId1 = 6L;
        int initialQuantity = 2;
        int updatedQuantity = -1;

        User user = createUser(userId);
        Cart cart = createCart(cartId, user);
        Product product = createProduct(
                productId,
                "PS5",
                BigDecimal.valueOf(499.99),
                10
        );

        Product product1 = createProduct(
                productId1,
                "XBOX",
                BigDecimal.valueOf(450.99),
                10
        );

        CartItem item = new CartItem(
                cartItemId,
                cart,
                product,
                initialQuantity);

        CartItem item1 = new CartItem(
                cartItemId1,
                cart,
                product1,
                initialQuantity);

        cart.addItem(item);
        cart.addItem(item1);

        when(userLookupService.getUserById(userId)).thenReturn(user);
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(productLookupService.getProductById(productId)).thenReturn(product);
        when(cartItemRepository.findByCartIdAndProductId(cartId, productId)).thenReturn(Optional.of(item));

        CartResponse response = cartService.updateItemQuantity(userId, productId, updatedQuantity);
        CartItemResponse firstItem = response.items().getFirst();

        assertThat(response).isNotNull();
        assertThat(response.items()).hasSize(1);
        assertThat(firstItem.productName()).isEqualTo(item1.getProduct().getName());
        assertThat(firstItem.quantity()).isEqualTo(item1.getQuantity());
        assertThat(item.getCart()).isNull();
        assertThat(item1.getCart()).isNotNull();
        assertThat(cart.getItems()).doesNotContain(item);
        assertThat(cart.getItems()).contains(item1);

        verify(userLookupService).getUserById(userId);
        verify(cartRepository).findByUserId(userId);
        verify(productLookupService).getProductById(productId);
        verify(cartItemRepository).findByCartIdAndProductId(cartId, productId);
        verifyNoMoreInteractions(userLookupService);
        verifyNoMoreInteractions(cartRepository);
        verifyNoMoreInteractions(productLookupService);
        verifyNoMoreInteractions(cartItemRepository);
    }

    @Test
    void removeItem_whenRequestIsValid_returnCartResponse() {
        Long userId = 1L;
        Long productId = 2L;
        Long productId1 = 3L;
        Long cartId = 4L;
        Long cartItemId = 5L;
        Long cartItemId1 = 6L;
        int initialQuantity = 2;

        User user = createUser(userId);
        Cart cart = createCart(cartId, user);
        Product product = createProduct(
                productId,
                "PS5",
                BigDecimal.valueOf(499.99),
                10
        );

        Product product1 = createProduct(
                productId1,
                "XBOX",
                BigDecimal.valueOf(450.99),
                10
        );

        CartItem item = new CartItem(
                cartItemId,
                cart,
                product,
                initialQuantity);

        CartItem item1 = new CartItem(
                cartItemId1,
                cart,
                product1,
                initialQuantity);

        cart.addItem(item);
        cart.addItem(item1);

        when(userLookupService.getUserById(userId)).thenReturn(user);
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdAndProductId(cartId, productId)).thenReturn(Optional.of(item));

        CartResponse response = cartService.removeItem(userId, productId);
        CartItemResponse firstItem = response.items().getFirst();

        assertThat(response).isNotNull();
        assertThat(response.items()).hasSize(1);
        assertThat(firstItem.productName()).isEqualTo(item1.getProduct().getName());
        assertThat(firstItem.quantity()).isEqualTo(item1.getQuantity());
        assertThat(item.getCart()).isNull();
        assertThat(item1.getCart()).isNotNull();
        assertThat(cart.getItems()).doesNotContain(item);
        assertThat(cart.getItems()).contains(item1);

        verify(userLookupService).getUserById(userId);
        verify(cartRepository).findByUserId(userId);
        verify(cartItemRepository).findByCartIdAndProductId(cartId, productId);
        verifyNoMoreInteractions(userLookupService);
        verifyNoMoreInteractions(cartRepository);
        verifyNoMoreInteractions(productLookupService);
        verifyNoMoreInteractions(cartItemRepository);
    }

    @Test
    void removeItem_userNotExists_throwNoResourceFoundException() {
        Long userId = 999L;
        Long cartItemId = 1L;

        when(userLookupService.getUserById(userId))
                .thenThrow(new NoResourceFoundException(NO_RESOURCE_FOUND_EXCEPTION_USER_MESSAGE + userId));

        NoResourceFoundException ex = assertThrows(
                NoResourceFoundException.class,
                () -> cartService.removeItem(userId, cartItemId)
        );

        assertThat(ex).isNotNull();
        assertThat(ex.getMessage()).isEqualTo(NO_RESOURCE_FOUND_EXCEPTION_USER_MESSAGE + userId);

        verify(userLookupService).getUserById(userId);
        verifyNoMoreInteractions(userLookupService);
        verifyNoInteractions(cartRepository);
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
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdAndProductId(cartId, productId)).thenReturn(Optional.empty());

        NoResourceFoundException ex = assertThrows(
                NoResourceFoundException.class,
                () -> cartService.removeItem(userId, productId)
        );

        assertThat(ex).isNotNull();
        assertThat(ex.getMessage()).isEqualTo(NO_RESOURCE_FOUND_EXCEPTION_ITEM_MESSAGE + productId);

        verify(userLookupService).getUserById(userId);
        verify(cartRepository).findByUserId(userId);
        verify(cartItemRepository).findByCartIdAndProductId(cartId, productId);
        verifyNoMoreInteractions(userLookupService);
        verifyNoMoreInteractions(cartRepository);
        verifyNoMoreInteractions(cartItemRepository);
        verifyNoInteractions(productLookupService);
    }

    @Test
    void removeItem_whenCartNotFound_throwNoResourceFoundException() {
        Long userId = 1L;
        Long productId = 2L;

        User user = createUser(userId);

        when(userLookupService.getUserById(userId)).thenReturn(user);
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.empty());

        NoResourceFoundException ex = assertThrows(
                NoResourceFoundException.class,
                () -> cartService.removeItem(userId, productId)
        );

        assertThat(ex).isNotNull();
        assertThat(ex.getMessage()).isEqualTo(NO_RESOURCE_FOUND_EXCEPTION_CART_MESSAGE + userId);

        verify(userLookupService).getUserById(userId);
        verify(cartRepository).findByUserId(userId);

        verifyNoMoreInteractions(userLookupService);
        verifyNoMoreInteractions(cartRepository);
        verifyNoMoreInteractions(productLookupService);
        verifyNoMoreInteractions(cartItemRepository);
    }

    @Test
    void clearCart_whenCartExists_clearsItemsAndReturnsEmptyCartResponse() {
        Long userId = 1L;
        Long productId = 2L;
        Long productId1 = 3L;
        Long cartId = 4L;
        Long cartItemId = 5L;
        Long cartItemId1 = 6L;
        int initialQuantity = 2;

        User user = createUser(userId);
        Cart cart = createCart(cartId, user);
        Product product = createProduct(
                productId,
                "PS5",
                BigDecimal.valueOf(499.99),
                10
        );

        Product product1 = createProduct(
                productId1,
                "XBOX",
                BigDecimal.valueOf(450.99),
                10
        );

        CartItem item = new CartItem(
                cartItemId,
                cart,
                product,
                initialQuantity);

        CartItem item1 = new CartItem(
                cartItemId1,
                cart,
                product1,
                initialQuantity);

        cart.addItem(item);
        cart.addItem(item1);

        assertThat(cart.getItems().size()).isEqualTo(2);

        when(userLookupService.getUserById(userId)).thenReturn(user);
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));

        CartResponse response = cartService.clearCart(userId);

        assertThat(response).isNotNull();
        assertThat(response.items()).hasSize(0);
        assertThat(cart.getItems()).doesNotContain(item);
        assertThat(cart.getItems()).doesNotContain(item1);
        assertThat(item.getCart()).isNull();
        assertThat(item1.getCart()).isNull();

        verify(userLookupService).getUserById(userId);
        verify(cartRepository).findByUserId(userId);
        verifyNoMoreInteractions(userLookupService);
        verifyNoMoreInteractions(cartRepository);
        verifyNoInteractions(productLookupService);
        verifyNoInteractions(cartItemRepository);
    }

    @Test
    void clearCart_userNotExists_throwNoResourceFoundException() {
        Long userId = 999L;

        when(userLookupService.getUserById(userId))
                .thenThrow(new NoResourceFoundException(NO_RESOURCE_FOUND_EXCEPTION_USER_MESSAGE + userId));

        NoResourceFoundException ex = assertThrows(
                NoResourceFoundException.class,
                () -> cartService.clearCart(userId)
        );

        assertThat(ex).isNotNull();
        assertThat(ex.getMessage()).isEqualTo(NO_RESOURCE_FOUND_EXCEPTION_USER_MESSAGE + userId);

        verify(userLookupService).getUserById(userId);
        verifyNoMoreInteractions(userLookupService);
        verifyNoInteractions(cartRepository);
        verifyNoInteractions(productLookupService);
        verifyNoInteractions(cartItemRepository);
    }

    @Test
    void clearCart_whenCartNotFound_throwNoResourceFoundException() {
        Long userId = 1L;

        User user = createUser(userId);

        when(userLookupService.getUserById(userId)).thenReturn(user);
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.empty());

        NoResourceFoundException ex = assertThrows(
                NoResourceFoundException.class,
                () -> cartService.clearCart(userId)
        );

        assertThat(ex).isNotNull();
        assertThat(ex.getMessage()).isEqualTo(NO_RESOURCE_FOUND_EXCEPTION_CART_MESSAGE + userId);

        verify(userLookupService).getUserById(userId);
        verify(cartRepository).findByUserId(userId);

        verifyNoMoreInteractions(userLookupService);
        verifyNoMoreInteractions(cartRepository);
        verifyNoMoreInteractions(productLookupService);
        verifyNoMoreInteractions(cartItemRepository);
    }
}
