package com.namnguyen.ecommerce_platform.integration.cart;

import com.namnguyen.ecommerce_platform.cart.dto.CartItemRequest;
import com.namnguyen.ecommerce_platform.cart.entity.Cart;
import com.namnguyen.ecommerce_platform.cart.entity.CartItem;
import com.namnguyen.ecommerce_platform.integration.BaseIntegrationTest;
import com.namnguyen.ecommerce_platform.product.entity.Product;
import com.namnguyen.ecommerce_platform.product.enums.ProductStatus;
import com.namnguyen.ecommerce_platform.user.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.math.BigDecimal;

import static com.namnguyen.ecommerce_platform.testutil.MockAuthentication.*;
import static com.namnguyen.ecommerce_platform.testutil.TestDataFactory.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class CartIntegrationTest extends BaseIntegrationTest {

    @Test
    void getCart_whenCartDoesNotExist_createsAndReturnsEmptyCart() throws Exception {
        User user = persistDefaultCustomer();

        authenticateUser(user.getId());

        mockMvc.perform(get(CART_URI))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)))
                .andExpect(jsonPath("$.total").value(0));

        Cart cart = cartRepository.findByUserId(user.getId()).orElseThrow();

        assertThat(cart.getUser().getId()).isEqualTo(user.getId());
    }

    @Test
    void getCart_whenCartHasItems_returnsItemsAndTotal() throws Exception {
        User user = persistDefaultCustomer();
        Product product = persistDefaultProduct();

        Cart cart = persistCart(user);
        persistCartItem(cart, product, 2);

        BigDecimal expectedSubtotal = product.getPrice().multiply(BigDecimal.valueOf(2));

        authenticateUser(user.getId());

        mockMvc.perform(get(CART_URI))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].productId").value(product.getId()))
                .andExpect(jsonPath("$.items[0].productName").value(product.getName()))
                .andExpect(jsonPath("$.items[0].unitPrice").value(product.getPrice().doubleValue()))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.items[0].subtotal").value(expectedSubtotal.doubleValue()))
                .andExpect(jsonPath("$.total").value(expectedSubtotal.doubleValue()));
    }

    @Test
    void addItem_whenUserAuthenticated_savesItemToDatabase() throws Exception {
        User user = persistDefaultCustomer();
        Product product = persistDefaultProduct();
        int addQuantity = 10;

        authenticateUser(user.getId());

        CartItemRequest cartItemRequest = createCartItemRequest(product.getId(), addQuantity);

        BigDecimal expectedSubtotal = product.getPrice().multiply(BigDecimal.valueOf(addQuantity));

        mockMvc.perform(post(CART_ITEM_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cartItemRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.productId").value(product.getId()))
                .andExpect(jsonPath("$.productName").value(product.getName()))
                .andExpect(jsonPath("$.unitPrice").value(product.getPrice().doubleValue()))
                .andExpect(jsonPath("$.quantity").value(addQuantity))
                .andExpect(jsonPath("$.subtotal").value(expectedSubtotal.doubleValue()));

        Cart savedCart = cartRepository.findByUserId(user.getId()).orElseThrow();

        CartItem savedItem = cartItemRepository.findByCartIdAndProductId(savedCart.getId(), cartItemRequest.productId()).orElseThrow();

        assertThat(savedItem.getProduct().getId()).isEqualTo(product.getId());
        assertThat(savedItem.getQuantity()).isEqualTo(addQuantity);
        assertThat(savedItem.getCart().getId()).isEqualTo(savedCart.getId());
        assertThat(savedCart.getUser().getId()).isEqualTo(user.getId());
    }

    @Test
    void addItem_whenItemAlreadyInCart_updatesQuantity() throws Exception {
        User user = persistDefaultCustomer();
        Product product = persistDefaultProduct();
        int initialQuantity = 10;
        int addQuantity = 2;

        Cart cart = persistCart(user);

        BigDecimal expectedSubtotal = product.getPrice().multiply(BigDecimal.valueOf(initialQuantity + addQuantity));

        authenticateUser(user.getId());

        persistCartItem(cart, product, initialQuantity);

        CartItemRequest cartItemRequest = createCartItemRequest(product.getId(), addQuantity);

        mockMvc.perform(post(CART_ITEM_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cartItemRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.productId").value(product.getId()))
                .andExpect(jsonPath("$.productName").value(product.getName()))
                .andExpect(jsonPath("$.unitPrice").value(product.getPrice().doubleValue()))
                .andExpect(jsonPath("$.quantity").value(initialQuantity + addQuantity))
                .andExpect(jsonPath("$.subtotal").value(expectedSubtotal    .doubleValue()));

        CartItem updatedItem = cartItemRepository
                .findByCartIdAndProductId(cart.getId(), product.getId())
                .orElseThrow();

        assertThat(updatedItem.getQuantity()).isEqualTo(initialQuantity + addQuantity);
        assertThat(updatedItem.getProduct().getId()).isEqualTo(product.getId());
        assertThat(updatedItem.getCart().getId()).isEqualTo(cart.getId());
    }

    @Test
    void addItem_whenQuantityExceedStock_returnsBadRequest() throws Exception {
        User user = persistDefaultCustomer();
        Product product = persistProduct(
                VALID_PRODUCT_NAME,
                VALID_PRODUCT_DESCRIPTION,
                VALID_PRODUCT_PRICE,
                10,
                ProductStatus.ACTIVE
        );
        int quantity = 11;

        authenticateUser(user.getId());

        CartItemRequest cartItemRequest = createCartItemRequest(product.getId(), quantity);

        mockMvc.perform(post(CART_ITEM_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cartItemRequest)))
                .andExpect(status().isBadRequest());

        assertThat(cartItemRepository.findAll()).isEmpty();
    }

    @Test
    void addItem_whenProductNotFound_returnsNotFound() throws Exception {
        User user = persistDefaultCustomer();

        authenticateUser(user.getId());

        CartItemRequest cartItemRequest = createCartItemRequest(1L, 10);

        mockMvc.perform(post(CART_ITEM_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cartItemRequest)))
                .andExpect(status().isNotFound());
    }

    @Test
    void addItem_whenProductOutOfStock_returnsBadRequest() throws Exception {
        User user = persistDefaultCustomer();

        Product product = persistProduct(
                VALID_PRODUCT_NAME,
                VALID_PRODUCT_DESCRIPTION,
                VALID_PRODUCT_PRICE,
                0,
                ProductStatus.OUT_OF_STOCK
        );

        authenticateUser(user.getId());

        CartItemRequest cartItemRequest = createCartItemRequest(product.getId(), 1);

        mockMvc.perform(post(CART_ITEM_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cartItemRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateItem_whenItemExistsInCart_updatesQuantityInDatabase() throws Exception {
        User user = persistDefaultCustomer();
        Product product = persistDefaultProduct();
        int initialQuantity = 10;
        int updateQuantity = 2;

        Cart cart = persistCart(user);

        BigDecimal total = product.getPrice().multiply(BigDecimal.valueOf(updateQuantity));

        authenticateUser(user.getId());

        persistCartItem(cart, product, initialQuantity);

        mockMvc.perform(patch(cartItemUri(product.getId()))
                        .param("quantity", String.valueOf(updateQuantity)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].productId").value(product.getId()))
                .andExpect(jsonPath("$.items[0].productName").value(product.getName()))
                .andExpect(jsonPath("$.items[0].unitPrice").value(product.getPrice().doubleValue()))
                .andExpect(jsonPath("$.items[0].quantity").value(updateQuantity))
                .andExpect(jsonPath("$.items[0].subtotal").value(total.doubleValue()))
                .andExpect(jsonPath("$.total").value(total.doubleValue()));

        CartItem savedItem = cartItemRepository.findByCartIdAndProductId(cart.getId(), product.getId()).orElseThrow();

        assertThat(savedItem.getProduct().getId()).isEqualTo(product.getId());
        assertThat(savedItem.getQuantity()).isEqualTo(updateQuantity);
        assertThat(savedItem.getCart().getId()).isEqualTo(cart.getId());
        assertThat(cart.getUser().getId()).isEqualTo(user.getId());
    }

    @Test
    void updateItem_whenQuantityExceedStock_returnsBadRequest() throws Exception {
        User user = persistDefaultCustomer();
        Product product = persistProduct(
                VALID_PRODUCT_NAME,
                VALID_PRODUCT_DESCRIPTION,
                VALID_PRODUCT_PRICE,
                11,
                ProductStatus.ACTIVE
        );

        int initialQuantity = 10;
        int updateQuantity = 12;

        Cart cart = persistCart(user);

        authenticateUser(user.getId());

        persistCartItem(cart, product, initialQuantity);

        mockMvc.perform(patch(cartItemUri(product.getId()))
                        .param("quantity", String.valueOf(updateQuantity)))
                .andExpect(status().isBadRequest());

        CartItem savedItem = cartItemRepository
                .findByCartIdAndProductId(cart.getId(), product.getId())
                .orElseThrow();

        assertThat(savedItem.getQuantity()).isEqualTo(initialQuantity);
    }

    @Test
    void updateItem_whenQuantityIsNegative_returnsBadRequest() throws Exception {
        User user = persistDefaultCustomer();
        Product product = persistDefaultProduct();

        int initialQuantity = 10;
        int quantity = -1;

        Cart cart = persistCart(user);

        authenticateUser(user.getId());

        persistCartItem(cart, product, initialQuantity);

        mockMvc.perform(patch(cartItemUri(product.getId()))
                        .param("quantity", String.valueOf(quantity)))
                .andExpect(status().isBadRequest());

        CartItem savedItem = cartItemRepository
                .findByCartIdAndProductId(cart.getId(), product.getId())
                .orElseThrow();

        assertThat(savedItem.getQuantity()).isEqualTo(initialQuantity);
    }

    @Test
    void updateItem_whenQuantityIsZero_removesItemFromCart() throws Exception {
        User user = persistDefaultCustomer();
        Product product = persistDefaultProduct();
        int initialQuantity = 10;
        int quantity = 0;

        Cart cart = persistCart(user);

        authenticateUser(user.getId());

        persistCartItem(cart, product, initialQuantity);

        mockMvc.perform(patch(cartItemUri(product.getId()))
                        .param("quantity", String.valueOf(quantity)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)))
                .andExpect(jsonPath("$.total").value(0));

        assertThat(cartItemRepository.findByCartIdAndProductId(cart.getId(), product.getId()))
                .isEmpty();
    }

    @Test
    void updateItem_whenItemNotInCart_returnsNotFound() throws Exception {
        User user = persistDefaultCustomer();
        Product product = persistDefaultProduct();
        persistCart(user);

        int quantity = 2;

        authenticateUser(user.getId());

        mockMvc.perform(patch(cartItemUri(product.getId()))
                        .param("quantity", String.valueOf(quantity)))
                .andExpect(status().isNotFound());
    }

    @Test
    void removeItem_whenItemExistsInCart_removesItemFromDatabase() throws Exception {
        User user = persistDefaultCustomer();
        Product product = persistDefaultProduct();
        int initialQuantity = 10;

        Cart cart = persistCart(user);

        authenticateUser(user.getId());

        persistCartItem(cart, product, initialQuantity);

        mockMvc.perform(delete(cartItemUri(product.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)))
                .andExpect(jsonPath("$.total").value(0));

        assertThat(
                cartItemRepository.findByCartIdAndProductId(
                        cart.getId(),
                        product.getId()
                )
        ).isEmpty();
    }

    @Test
    void removeItem_whenProductExistsButItemNotInCart_returnsNotFound() throws Exception {
        User user = persistDefaultCustomer();
        persistCart(user);
        Product product = persistDefaultProduct();

        authenticateUser(user.getId());

        mockMvc.perform(delete(cartItemUri(product.getId())))
                .andExpect(status().isNotFound());
    }

    @Test
    void clearCart_whenCartIsEmpty_returnsEmptyCartResponse() throws Exception {
        User user = persistDefaultCustomer();
        persistCart(user);

        authenticateUser(user.getId());

        mockMvc.perform(delete(CART_URI))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)))
                .andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void clearCart_whenCartHasItems_removesAllItemsFromDatabase() throws Exception {
        User user = persistDefaultCustomer();
        Cart cart = persistCart(user);
        int initialQuantity = 10;

        Product product = persistDefaultProduct();

        persistCartItem(cart, product, initialQuantity);

        authenticateUser(user.getId());

        mockMvc.perform(delete(CART_URI))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)))
                .andExpect(jsonPath("$.total").value(0));

        assertThat(cartItemRepository.findByCartIdAndProductId(cart.getId(), product.getId()))
                .isEmpty();
    }
}
