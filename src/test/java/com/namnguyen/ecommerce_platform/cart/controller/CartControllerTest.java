package com.namnguyen.ecommerce_platform.cart.controller;

import com.namnguyen.ecommerce_platform.cart.dto.CartItemRequest;
import com.namnguyen.ecommerce_platform.cart.dto.CartItemResponse;
import com.namnguyen.ecommerce_platform.cart.dto.CartResponse;
import com.namnguyen.ecommerce_platform.cart.service.CartService;
import com.namnguyen.ecommerce_platform.security.jwt.JwtService;
import com.namnguyen.ecommerce_platform.security.user.CustomUserDetailsService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;

import static com.namnguyen.ecommerce_platform.testutil.MockAuthentication.*;
import static com.namnguyen.ecommerce_platform.testutil.TestDataFactory.*;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static com.namnguyen.ecommerce_platform.testutil.TestMessages.*;
import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.containsInAnyOrder;

import org.junit.jupiter.api.AfterEach;

@WebMvcTest(CartController.class)
@AutoConfigureMockMvc(addFilters = false)
public class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CartService cartService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @AfterEach
    public void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getCart_whenRequestValidAndCartHasItems_returnCartResponse() throws Exception {
        Long userId = 1L;
        Long productId = 1L;
        Long productId1 = 2L;
        int quantity = 2;

        BigDecimal total = VALID_PRODUCT_PRICE.multiply(BigDecimal.valueOf(quantity));
        BigDecimal totalCart = total.multiply(BigDecimal.TWO);

        authenticateUser(userId);

        CartItemResponse itemResponse = new CartItemResponse(
                productId,
                VALID_PRODUCT_NAME,
                VALID_PRODUCT_PRICE,
                quantity,
                total
        );

        CartItemResponse itemResponse1 = new CartItemResponse(
                productId1,
                VALID_PRODUCT_NAME,
                VALID_PRODUCT_PRICE,
                quantity,
                total
        );

        CartResponse cartResponse = new CartResponse(List.of(itemResponse, itemResponse1), totalCart);

        when(cartService.getCart(userId)).thenReturn(cartResponse);

        mockMvc.perform(get(CART_URI))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[0].productId").value(productId))
                .andExpect(jsonPath("$.items[0].productName").value(VALID_PRODUCT_NAME))
                .andExpect(jsonPath("$.items[0].unitPrice").value(VALID_PRODUCT_PRICE.doubleValue()))
                .andExpect(jsonPath("$.items[0].quantity").value(quantity))
                .andExpect(jsonPath("$.items[0].subtotal").value(total.doubleValue()))
                .andExpect(jsonPath("$.items[1].productId").value(productId1))
                .andExpect(jsonPath("$.items[1].productName").value(VALID_PRODUCT_NAME))
                .andExpect(jsonPath("$.items[1].unitPrice").value(VALID_PRODUCT_PRICE.doubleValue()))
                .andExpect(jsonPath("$.items[1].quantity").value(quantity))
                .andExpect(jsonPath("$.items[1].subtotal").value(total.doubleValue()))
                .andExpect(jsonPath("$.total").value(totalCart.doubleValue()));

        verify(cartService).getCart(userId);
        verifyNoMoreInteractions(cartService);
    }

    @Test
    void getCart_whenRequestValidAndCartIsEmpty_returnCartResponse() throws Exception {
        Long userId = 1L;

        CartResponse cartResponse = new CartResponse(List.of(), BigDecimal.ZERO);

        when(cartService.getCart(userId)).thenReturn(cartResponse);

        authenticateUser(userId);

        mockMvc.perform(get(CART_URI))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items", hasSize(0)))
                .andExpect(jsonPath("$.total").value(BigDecimal.ZERO));

        verify(cartService).getCart(userId);
        verifyNoMoreInteractions(cartService);
    }

    @Test
    void addItem_whenRequestIsValid_returnCartItemResponse() throws Exception {
        Long productId = 1L;
        Long userId = 2L;
        int quantity = 2;

        CartItemRequest request = new CartItemRequest(
                productId,
                quantity
        );

        BigDecimal total = VALID_PRODUCT_PRICE.multiply(BigDecimal.valueOf(quantity));

        CartItemResponse response = new CartItemResponse(
                productId,
                VALID_PRODUCT_NAME,
                VALID_PRODUCT_PRICE,
                quantity,
                total
        );

        when(cartService.addItem(userId, request)).thenReturn(response);

        authenticateUser(userId);

        mockMvc.perform(post(CART_ITEM_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.productId").value(productId))
                .andExpect(jsonPath("$.productName").value(VALID_PRODUCT_NAME))
                .andExpect(jsonPath("$.unitPrice").value(VALID_PRODUCT_PRICE.doubleValue()))
                .andExpect(jsonPath("$.quantity").value(quantity))
                .andExpect(jsonPath("$.subtotal").value(total.doubleValue()));

        ArgumentCaptor<CartItemRequest> captor = ArgumentCaptor.forClass(CartItemRequest.class);
        verify(cartService).addItem(eq(userId), captor.capture());

        CartItemRequest requestCaptured = captor.getValue();

        assertThat(requestCaptured.productId()).isEqualTo(productId);
        assertThat(requestCaptured.quantity()).isEqualTo(quantity);

        verifyNoMoreInteractions(cartService);
    }

    @Test
    void addItem_whenProductIdIsNull_returnsBadRequest() throws Exception {
        Long userId = 2L;
        int quantity = 2;

        CartItemRequest request = new CartItemRequest(
                null,
                quantity
        );

        authenticateUser(userId);

        mockMvc.perform(post(CART_ITEM_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.uri").value(CART_ITEM_URI))
                .andExpect(jsonPath("$.fieldErrors.productId", containsInAnyOrder(productIdIsRequired())));

        verifyNoInteractions(cartService);
    }

    @Test
    void addItem_whenQuantityIsNull_returnsBadRequest() throws Exception {
        Long productId = 1L;
        Long userId = 2L;

        CartItemRequest request = new CartItemRequest(
                productId,
                null
        );

        authenticateUser(userId);

        mockMvc.perform(post(CART_ITEM_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.uri").value(CART_ITEM_URI))
                .andExpect(jsonPath("$.fieldErrors.quantity", containsInAnyOrder(quantityIsRequired())));

        verifyNoInteractions(cartService);
    }

    @Test
    void addItem_whenQuantityIsLessThan1_returnsBadRequest() throws Exception {
        Long productId = 1L;
        Long userId = 2L;

        CartItemRequest request = new CartItemRequest(
                productId,
                0
        );

        authenticateUser(userId);

        mockMvc.perform(post(CART_ITEM_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.uri").value(CART_ITEM_URI))
                .andExpect(jsonPath("$.fieldErrors.quantity", containsInAnyOrder(quantityIsZero())));

        verifyNoInteractions(cartService);
    }

    @Test
    void updateItem_whenRequestIsValid_returnsCartResponse() throws Exception {
        Long userId = 1L;
        Long productId = 1L;
        int updatedQuantity = 4;

        BigDecimal updatedTotal = VALID_PRODUCT_PRICE.multiply(BigDecimal.valueOf(updatedQuantity));

        authenticateUser(userId);

        CartItemResponse itemResponse = new CartItemResponse(
                productId,
                VALID_PRODUCT_NAME,
                VALID_PRODUCT_PRICE,
                updatedQuantity,
                updatedTotal
        );

        authenticateUser(userId);

        CartResponse cartResponse = new CartResponse(List.of(itemResponse), updatedTotal);

        when(cartService.updateItemQuantity(userId, productId, updatedQuantity)).thenReturn(cartResponse);

        mockMvc.perform(patch(CART_ITEM_URI + "/" + productId)
                        .param("quantity", String.valueOf(updatedQuantity)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].productId").value(productId))
                .andExpect(jsonPath("$.items[0].productName").value(VALID_PRODUCT_NAME))
                .andExpect(jsonPath("$.items[0].unitPrice").value(VALID_PRODUCT_PRICE.doubleValue()))
                .andExpect(jsonPath("$.items[0].quantity").value(updatedQuantity))
                .andExpect(jsonPath("$.items[0].subtotal").value(updatedTotal.doubleValue()));

        verify(cartService).updateItemQuantity(userId, productId, updatedQuantity);
        verifyNoMoreInteractions(cartService);
    }

    @Test
    void updateItem_whenProductIdIsInvalid_returnsBadRequest() throws Exception {
        Long userId = 1L;
        String productId = INVALID_ID;
        int quantity = 1;

        authenticateUser(userId);

        mockMvc.perform(patch(CART_ITEM_URI + "/" + productId)
                        .param("quantity", String.valueOf(quantity)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(invalidParameter("productId")))
                .andExpect(jsonPath("$.uri").value(CART_ITEM_URI + "/" + productId));

        verifyNoInteractions(cartService);
    }

    @Test
    void updateItem_whenQuantityIsNegative_returnsBadRequest() throws Exception {
        Long userId = 1L;
        Long productId = 1L;
        int quantity = -1;

        authenticateUser(userId);

        mockMvc.perform(patch(CART_ITEM_URI + "/" + productId)
                        .param("quantity", String.valueOf(quantity)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.uri").value(CART_ITEM_URI + "/" + productId))
                .andExpect(jsonPath("$.fieldErrors.quantity", containsInAnyOrder(invalidParameter("quantity"))));

        verifyNoInteractions(cartService);
    }

    @Test
    void deleteCart_whenRequestIsValid_returnsCartResponse() throws Exception {
        Long userId = 1L;
        Long productId = 1L;

        authenticateUser(userId);

        CartResponse cartResponse = new CartResponse(List.of(), BigDecimal.ZERO);

        when(cartService.removeItem(userId, productId)).thenReturn(cartResponse);

        mockMvc.perform(delete(CART_ITEM_URI + "/" + productId)
                        .param("productId", String.valueOf(productId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items", hasSize(0)))
                .andExpect(jsonPath("$.total").value(BigDecimal.ZERO.doubleValue()));

        verify(cartService).removeItem(userId, productId);
        verifyNoMoreInteractions(cartService);
    }

    @Test
    void deleteCart_whenProductIdIsInvalid_returnsBadRequest() throws Exception {
        Long userId = 1L;
        String productId = INVALID_ID;

        authenticateUser(userId);

        mockMvc.perform(delete(CART_ITEM_URI + "/" + productId)
                        .param("productId", productId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(invalidParameter("productId")))
                .andExpect(jsonPath("$.uri").value(CART_ITEM_URI + "/" + productId));

        verifyNoInteractions(cartService);
    }

    @Test
    void clearCart_whenRequestIsValid_returnsCartResponse() throws Exception {
        Long userId = 1L;

        authenticateUser(userId);

        CartResponse cartResponse = new CartResponse(List.of(), BigDecimal.ZERO);

        when(cartService.clearCart(userId)).thenReturn(cartResponse);

        mockMvc.perform(delete(CART_URI))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items", hasSize(0)))
                .andExpect(jsonPath("$.total").value(BigDecimal.ZERO.doubleValue()));

        verify(cartService).clearCart(userId);
        verifyNoMoreInteractions(cartService);
    }
}
