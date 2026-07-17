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
    CartItemRepository cartItemRepository;

    @Mock
    private UserLookupService userLookupService;

    @Mock
    private ProductLookupService productLookupService;

    @InjectMocks
    private CartServiceImpl cartService;

    private static final String NO_RESOURCE_FOUND_EXCEPTION_USER_MESSAGE = "User not found with id: ";
    private static final String NO_RESOURCE_FOUND_EXCEPTION_PRODUCT_MESSAGE = "Product not found with id: ";
    private static final String NO_RESOURCE_FOUND_EXCEPTION_ITEM_MESSAGE = "No item found with product id: ";
    private static final String INSUFFICIENT_STOCK_EXCEPTION_MESSAGE = "Not enough stock for: ";

    @Test
    void getCart_whenCartExist_returnCartResponse() {
        User user = createUser(1L);
        Product product = createProduct(
                1L,
                "PS5",
                BigDecimal.valueOf(499.99),
                10
        );
        Product product1 = createProduct(
                2L,
                "XBOX",
                BigDecimal.valueOf(450.99),
                12
        );
        Cart cart = createCartWithItem(
                1L,
                user,
                product,
                1);

        CartItem item = createCartItem(
                1L,
                cart,
                product1,
                1);

        cart.addItem(item);

        when(userLookupService.getUserById(user.getId())).thenReturn(user);
        when(cartRepository.findByUserId(user.getId())).thenReturn(Optional.of(cart));

        CartResponse response = cartService.getCart(user.getId());

        assertThat(response).isNotNull();
        assertThat(response.items().size()).isEqualTo(2);

        CartItemResponse firstItemResponse = response.items().getFirst();
        assertThat(firstItemResponse.productId()).isEqualTo(product.getId());
        assertThat(firstItemResponse.productName()).isEqualTo(product.getName());
        assertThat(firstItemResponse.quantity()).isEqualTo(cart.getItems().getFirst().getQuantity());
        assertThat(firstItemResponse.subtotal()).isEqualTo(product.getPrice());
        assertThat(firstItemResponse.unitPrice()).isEqualTo(product.getPrice());

        CartItemResponse secondItemResponse = response.items().get(1);
        assertThat(secondItemResponse.productId()).isEqualTo(product1.getId());
        assertThat(secondItemResponse.productName()).isEqualTo(product1.getName());
        assertThat(secondItemResponse.quantity()).isEqualTo(cart.getItems().get(1).getQuantity());
        assertThat(secondItemResponse.subtotal()).isEqualTo(product1.getPrice());
        assertThat(secondItemResponse.unitPrice()).isEqualTo(product1.getPrice());


        verify(userLookupService).getUserById(user.getId());
        verify(cartRepository).findByUserId(user.getId());
        verifyNoMoreInteractions(userLookupService);
        verifyNoMoreInteractions(cartRepository);
    }

    @Test
    void getCart_whenCartDoesNotExist_createsNewCartAndReturnsEmptyCartResponse() {
        User user = createUser(1L);

        assertThat(user.getCart()).isNull();

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
        assertThat(response.items().size()).isEqualTo(0);

        verify(userLookupService).getUserById(user.getId());
        verify(cartRepository).findByUserId(user.getId());
        verifyNoMoreInteractions(userLookupService);
        verifyNoMoreInteractions(cartRepository);
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

        verifyNoMoreInteractions(userLookupService);
        verifyNoInteractions(cartRepository);
        verifyNoInteractions(cartItemRepository);
        verifyNoInteractions(productLookupService);
    }

    @Test
    void addItem_whenProductNotInCart_addsNewCartItem() {
        Long userId = 1L;
        Long cartItemId = 1L;
        User user = createUser(userId
        );
        Product product = createProduct(
                1L,
                "PS5",
                BigDecimal.valueOf(499.99),
                10
        );

        Cart cart = createCart(1L, user);

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
            savedItem.setProduct(product);
            savedItem.setCart(cart);
            savedItem.setQuantity(request.quantity());
            return savedItem;
        });

        CartItemResponse response = cartService.addItem(user.getId(), request);

        assertThat(response).isNotNull();
        assertThat(response.productId()).isEqualTo(product.getId());
        assertThat(response.productName()).isEqualTo(product.getName());
        assertThat(response.unitPrice()).isEqualTo(product.getPrice());
        assertThat(response.quantity()).isEqualTo(request.quantity());
        assertThat(response.quantity()).isEqualTo(2);
        assertThat(response.subtotal()).isEqualTo(product.getPrice().multiply(BigDecimal.valueOf(request.quantity())));

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
        verifyNoMoreInteractions(productLookupService);
        verifyNoMoreInteractions(productLookupService);
        verifyNoMoreInteractions(cartItemRepository);
    }

    @Test
    void addItem_whenProductAlreadyInCart_increasesQuantity() {
        Long userId = 1L;
        Long cartItemId = 1L;
        User user = createUser(userId);
        Product product = createProduct(
                1L,
                "PS5",
                BigDecimal.valueOf(499.99),
                10
        );

        Cart cart = createCart(1L, user);
        int initialQuantity = 2;

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
        Long productId = 1L;

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
        User user = createUser(userId);
        Cart cart = createCart(1L, user);

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
        User user = createUser(userId
        );
        Product product = createProduct(
                1L,
                "PS5",
                BigDecimal.valueOf(499.99),
                10
        );

        Cart cart = createCart(1L, user);

        CartItemRequest request = new CartItemRequest(
                product.getId(),
                11
        );

        when(userLookupService.getUserById(user.getId())).thenReturn(user);
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(productLookupService.getProductById(product.getId())).thenReturn(product);
        when(cartItemRepository.findByCartIdAndProductId(userId, product.getId())).thenReturn(Optional.empty());

        InsufficientStockException ex = assertThrows(
                InsufficientStockException.class,
                () -> cartService.addItem(user.getId(), request)
        );

        assertThat(ex).isNotNull();
        assertThat(ex.getMessage()).isEqualTo(INSUFFICIENT_STOCK_EXCEPTION_MESSAGE + product.getName());

        verify(userLookupService).getUserById(userId);
        verify(cartRepository).findByUserId(userId);
        verify(productLookupService).getProductById(product.getId());
        verify(cartItemRepository).findByCartIdAndProductId(cart.getId(), product.getId());
        verifyNoMoreInteractions(cartItemRepository);
    }

    @Test
    void updateItemQuantity_whenRequestIsValid_updateQuantity() {
        Long userId = 1L;
        Long cartItemId = 1L;
        Long productId = 1L;
        Long cartId = 1L;

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
        when(cartItemRepository.findByCartIdAndProductId(userId, productId)).thenReturn(Optional.of(item));

        CartResponse response = cartService.updateItemQuantity(userId, productId, updateQuantity);

        assertThat(response).isNotNull();
        assertThat(response.items().getFirst().productId()).isEqualTo(product.getId());
        assertThat(response.items().getFirst().productName()).isEqualTo(product.getName());
        assertThat(response.items().getFirst().unitPrice()).isEqualTo(product.getPrice());
        assertThat(response.items().getFirst().quantity()).isEqualTo(updateQuantity);
        assertThat(response.items().getFirst().subtotal()).isEqualTo(product.getPrice().multiply(BigDecimal.valueOf(updateQuantity)));

        verify(userLookupService, times(2)).getUserById(userId);
        verify(cartRepository, times(2)).findByUserId(userId);
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
        Long productId = 1L;
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
    void updateItemQuantity_whenProductNotFound_throwNoResourceFoundException() {
        Long userId = 1L;
        Long productId = 1L;
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
        Long productId = 1L;
        Long cartId = 1L;
        int updateQuantity = 4;

        User user = createUser(userId);
        Cart cart = createCart(1L, user);
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

        verify(userLookupService, times(2)).getUserById(userId);
        verify(cartRepository, times(2)).findByUserId(userId);
        verify(productLookupService).getProductById(productId);
        verify(cartItemRepository).findByCartIdAndProductId(cartId, productId);
        verifyNoMoreInteractions(cartRepository);
        verifyNoMoreInteractions(productLookupService);
        verifyNoMoreInteractions(cartItemRepository);
    }
}
