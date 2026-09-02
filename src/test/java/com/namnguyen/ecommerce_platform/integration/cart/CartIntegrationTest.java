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
    void getCart_whenUserAuthenticated_returnsCartResponse() throws Exception {
        User user = createDefaultCustomer();

        authenticateUser(user.getId());

        mockMvc.perform(get(CART_URI))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").exists())
                .andExpect(jsonPath("$.total").exists());

        Cart cart = cartRepository.findByUserId(user.getId()).orElseThrow();

        assertThat(cart.getUser().getId()).isEqualTo(user.getId());
    }

    @Test
    void getCart_whenCartHasItems_returnsItemsAndTotal() throws Exception {
        User user = createDefaultCustomer();
        Product product = createDefaultProduct();

        Cart cart = createCart(user);
        createCartItem(cart, product, 2);

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
        User user = createDefaultCustomer();
        Product product = createDefaultProduct();

        authenticateUser(user.getId());

        CartItemRequest request = createCartItemRequest(product.getId(), 10);

        BigDecimal expectedSubtotal = product.getPrice().multiply(BigDecimal.valueOf(10));

        mockMvc.perform(post(CART_URI + "/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.productId").value(product.getId()))
                .andExpect(jsonPath("$.productName").value(product.getName()))
                .andExpect(jsonPath("$.unitPrice").value(product.getPrice().doubleValue()))
                .andExpect(jsonPath("$.quantity").value(10))
                .andExpect(jsonPath("$.subtotal").value(expectedSubtotal.doubleValue()));

        Cart cart = cartRepository.findByUserId(user.getId()).orElseThrow();

        CartItem item = cartItemRepository.findByCartIdAndProductId(cart.getId(), request.productId()).orElseThrow();

        assertThat(item.getProduct().getId()).isEqualTo(product.getId());
        assertThat(item.getQuantity()).isEqualTo(10);
        assertThat(item.getCart().getId()).isEqualTo(cart.getId());
        assertThat(cart.getUser().getId()).isEqualTo(user.getId());
    }

    @Test
    void addItem_whenItemAlreadyInCart_updatesQuantity() throws Exception {
        User user = createDefaultCustomer();
        Product product = createProduct(
                "Keyboard",
                "Mechanical keyboard",
                BigDecimal.valueOf(99.99),
                20,
                ProductStatus.ACTIVE
        );
        int initialQuantity = 10;
        int addQuantity = 2;

        Cart cart = createCart(user);

        BigDecimal total = product.getPrice().multiply(BigDecimal.valueOf(initialQuantity + addQuantity));

        authenticateUser(user.getId());

        createCartItem(cart, product, initialQuantity);

        CartItemRequest request = createCartItemRequest(product.getId(), addQuantity);

        mockMvc.perform(post(CART_URI + "/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.productId").value(product.getId()))
                .andExpect(jsonPath("$.productName").value(product.getName()))
                .andExpect(jsonPath("$.unitPrice").value(product.getPrice().doubleValue()))
                .andExpect(jsonPath("$.quantity").value(initialQuantity + addQuantity))
                .andExpect(jsonPath("$.subtotal").value(total.doubleValue()));

        CartItem updatedItem = cartItemRepository
                .findByCartIdAndProductId(cart.getId(), product.getId())
                .orElseThrow();

        assertThat(updatedItem.getQuantity()).isEqualTo(initialQuantity + addQuantity);
        assertThat(updatedItem.getProduct().getId()).isEqualTo(product.getId());
        assertThat(updatedItem.getCart().getId()).isEqualTo(cart.getId());
    }

    @Test
    void addItem_whenQuantityExceedStock_returnsBadRequest() throws Exception {
        User user = createDefaultCustomer();
        Product product = createProduct(
                "Keyboard",
                "Mechanical keyboard",
                BigDecimal.valueOf(99.99),
                10,
                ProductStatus.ACTIVE
        );
        int quantity = 11;

        authenticateUser(user.getId());

        CartItemRequest request = createCartItemRequest(product.getId(), quantity);

        mockMvc.perform(post(CART_URI + "/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        assertThat(cartItemRepository.findAll()).isEmpty();
    }

    @Test
    void addItem_whenProductNotFound_returnsNotFound() throws Exception {
        User user = createDefaultCustomer();

        authenticateUser(user.getId());

        CartItemRequest request = createCartItemRequest(1L, 10);

        mockMvc.perform(post(CART_URI + "/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void addItem_whenProductOutOfStock_returnsBadRequest() throws Exception {
        User user = createDefaultCustomer();

        Product product = createProduct(
                "Keyboard",
                "Mechanical keyboard",
                BigDecimal.valueOf(99.99),
                0,
                ProductStatus.ACTIVE
        );

        authenticateUser(user.getId());

        CartItemRequest request = createCartItemRequest(product.getId(), 1);

        mockMvc.perform(post(CART_URI + "/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateItem_whenItemExistsInCart_updatesQuantityInDatabase() throws Exception {
        User user = createDefaultCustomer();
        Product product = createProduct(
                "Keyboard",
                "Mechanical keyboard",
                BigDecimal.valueOf(99.99),
                20,
                ProductStatus.ACTIVE
        );

        int initialQuantity = 10;
        int quantity = 2;

        Cart cart = createCart(user);

        BigDecimal total = product.getPrice().multiply(BigDecimal.valueOf(quantity));

        authenticateUser(user.getId());

        createCartItem(cart, product, initialQuantity);

        mockMvc.perform(patch(CART_URI + "/items/" + product.getId())
                        .param("quantity", String.valueOf(quantity)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].productId").value(product.getId()))
                .andExpect(jsonPath("$.items[0].productName").value(product.getName()))
                .andExpect(jsonPath("$.items[0].unitPrice").value(product.getPrice().doubleValue()))
                .andExpect(jsonPath("$.items[0].quantity").value(quantity))
                .andExpect(jsonPath("$.items[0].subtotal").value(total.doubleValue()))
                .andExpect(jsonPath("$.total").value(total.doubleValue()));

        CartItem item = cartItemRepository.findByCartIdAndProductId(cart.getId(), product.getId()).orElseThrow();

        assertThat(item.getProduct().getId()).isEqualTo(product.getId());
        assertThat(item.getQuantity()).isEqualTo(2);
        assertThat(item.getCart().getId()).isEqualTo(cart.getId());
        assertThat(cart.getUser().getId()).isEqualTo(user.getId());
    }

    @Test
    void updateItem_whenQuantityExceedStock_returnsBadRequest() throws Exception {
        User user = createDefaultCustomer();
        Product product = createProduct(
                "Keyboard",
                "Mechanical keyboard",
                BigDecimal.valueOf(99.99),
                11,
                ProductStatus.ACTIVE
        );

        int initialQuantity = 10;
        int quantity = 12;

        Cart cart = createCart(user);

        authenticateUser(user.getId());

        createCartItem(cart, product, initialQuantity);

        mockMvc.perform(patch(CART_URI + "/items/" + product.getId())
                        .param("quantity", String.valueOf(quantity)))
                .andExpect(status().isBadRequest());

        CartItem savedItem = cartItemRepository
                .findByCartIdAndProductId(cart.getId(), product.getId())
                .orElseThrow();

        assertThat(savedItem.getQuantity()).isEqualTo(initialQuantity);
    }

    @Test
    void updateItem_whenQuantityIsNegative_returnsBadRequest() throws Exception {
        User user = createDefaultCustomer();
        Product product = createProduct(
                "Keyboard",
                "Mechanical keyboard",
                BigDecimal.valueOf(99.99),
                11,
                ProductStatus.ACTIVE
        );

        int initialQuantity = 10;
        int quantity = -1;

        Cart cart = createCart(user);

        authenticateUser(user.getId());

        createCartItem(cart, product, initialQuantity);

        mockMvc.perform(patch(CART_URI + "/items/" + product.getId())
                        .param("quantity", String.valueOf(quantity)))
                .andExpect(status().isBadRequest());

        CartItem savedItem = cartItemRepository
                .findByCartIdAndProductId(cart.getId(), product.getId())
                .orElseThrow();

        assertThat(savedItem.getQuantity()).isEqualTo(initialQuantity);
    }

    @Test
    void updateItem_whenUpdateQuantityToZero_updatesQuantityInDatabase() throws Exception {
        User user = createDefaultCustomer();
        Product product = createProduct(
                "Keyboard",
                "Mechanical keyboard",
                BigDecimal.valueOf(99.99),
                20,
                ProductStatus.ACTIVE
        );

        int initialQuantity = 10;
        int quantity = 0;

        Cart cart = createCart(user);

        authenticateUser(user.getId());

        createCartItem(cart, product, initialQuantity);

        mockMvc.perform(patch(CART_URI + "/items/" + product.getId())
                        .param("quantity", String.valueOf(quantity)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)))
                .andExpect(jsonPath("$.total").value(0));

        assertThat(cartItemRepository.findByCartIdAndProductId(cart.getId(), product.getId()))
                .isEmpty();
    }

    @Test
    void updateItem_whenProductNotFound_returnsNotFound() throws Exception {
        User user = createDefaultCustomer();
        createCart(user);

        Long productId = 999L;
        int quantity = 2;

        authenticateUser(user.getId());

        mockMvc.perform(patch(CART_URI + "/items/" + productId)
                        .param("quantity", String.valueOf(quantity)))
                .andExpect(status().isNotFound());
    }

    @Test
    void removeItem_whenItemExistsInCart_removesItemFromDatabase() throws Exception {
        User user = createDefaultCustomer();
        Product product = createDefaultProduct();
        int initialQuantity = 10;

        Cart cart = createCart(user);

        authenticateUser(user.getId());

        createCartItem(cart, product, initialQuantity);

        mockMvc.perform(delete(CART_URI + "/items/" + product.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)))
                .andExpect(jsonPath("$.total").value(0));

        CartItem item = cartItemRepository.findByCartIdAndProductId(cart.getId(), product.getId()).orElse(null);

        assertThat(item).isNull();
    }

    @Test
    void removeItem_whenProductExistsButItemNotInCart_returnsNotFound() throws Exception {
        User user = createDefaultCustomer();
        createCart(user);
        Product product = createDefaultProduct();

        authenticateUser(user.getId());

        mockMvc.perform(delete(CART_URI + "/items/" + product.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void clearCart_whenCartIsEmpty_returnsEmptyCartResponse() throws Exception {
        User user = createDefaultCustomer();
        createCart(user);

        authenticateUser(user.getId());

        mockMvc.perform(delete(CART_URI))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)))
                .andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void clearCart_whenCartHasItems_removesAllItemsFromDatabase() throws Exception {
        User user = createDefaultCustomer();
        Cart cart = createCart(user);
        int initialQuantity = 10;

        Product product = createDefaultProduct();

        createCartItem(cart, product, initialQuantity);

        authenticateUser(user.getId());

        mockMvc.perform(delete(CART_URI))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)))
                .andExpect(jsonPath("$.total").value(0));

        assertThat(cartItemRepository.findByCartIdAndProductId(cart.getId(), product.getId()))
                .isEmpty();
    }
}
