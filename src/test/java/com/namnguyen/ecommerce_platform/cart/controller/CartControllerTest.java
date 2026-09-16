package com.namnguyen.ecommerce_platform.cart.controller;

import com.namnguyen.ecommerce_platform.cart.dto.CartItemRequest;
import com.namnguyen.ecommerce_platform.cart.dto.CartItemResponse;
import com.namnguyen.ecommerce_platform.cart.dto.CartResponse;
import com.namnguyen.ecommerce_platform.cart.service.CartService;
import com.namnguyen.ecommerce_platform.common.exception.NoResourceFoundException;
import com.namnguyen.ecommerce_platform.common.rate_limit.RateLimitService;
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
import static com.namnguyen.ecommerce_platform.testutil.messages.CartTestMessages.*;
import static com.namnguyen.ecommerce_platform.testutil.messages.CommonTestMessages.VALIDATION_FAILED;
import static com.namnguyen.ecommerce_platform.testutil.messages.CommonTestMessages.invalidParameter;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.containsInAnyOrder;

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

    @MockitoBean
    private RateLimitService rateLimitService;

    @AfterEach
    public void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getCart_whenRequestValidAndCartHasItems_returnCartResponse() throws Exception {
        Long userId = 1L;
        Long firstProductId = 1L;
        Long secondProductId = 2L;
        int quantity = 2;

        BigDecimal total = VALID_PRODUCT_PRICE.multiply(BigDecimal.valueOf(quantity));
        BigDecimal totalCart = total.multiply(BigDecimal.TWO);

        authenticateUser(userId);

        CartItemResponse firstItemResponse = new CartItemResponse(
                firstProductId,
                VALID_PRODUCT_NAME,
                VALID_PRODUCT_PRICE,
                quantity,
                total
        );

        CartItemResponse secondItemResponse = new CartItemResponse(
                secondProductId,
                VALID_PRODUCT_NAME,
                VALID_PRODUCT_PRICE,
                quantity,
                total
        );

        CartResponse cartResponse = new CartResponse(List.of(firstItemResponse, secondItemResponse), totalCart);

        when(cartService.getCart(userId)).thenReturn(cartResponse);

        mockMvc.perform(get(CART_URI))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[0].productId").value(firstProductId))
                .andExpect(jsonPath("$.items[0].productName").value(VALID_PRODUCT_NAME))
                .andExpect(jsonPath("$.items[0].unitPrice").value(VALID_PRODUCT_PRICE.doubleValue()))
                .andExpect(jsonPath("$.items[0].quantity").value(quantity))
                .andExpect(jsonPath("$.items[0].subtotal").value(total.doubleValue()))
                .andExpect(jsonPath("$.items[1].productId").value(secondProductId))
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
                .andExpect(jsonPath("$.total").value(BigDecimal.ZERO.doubleValue()));

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

        CartItemResponse itemResponse = new CartItemResponse(
                productId,
                VALID_PRODUCT_NAME,
                VALID_PRODUCT_PRICE,
                quantity,
                total
        );

        when(cartService.addItem(userId, request)).thenReturn(itemResponse);

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

        CartItemRequest capturedRequest = captor.getValue();

        assertThat(capturedRequest.productId()).isEqualTo(productId);
        assertThat(capturedRequest.quantity()).isEqualTo(quantity);

        verifyNoMoreInteractions(cartService);
    }

    @Test
    void addItem_whenProductIdIsNull_returnsBadRequest() throws Exception {
        Long userId = 2L;
        int quantity = 2;

        CartItemRequest itemRequest = new CartItemRequest(
                null,
                quantity
        );

        authenticateUser(userId);

        mockMvc.perform(post(CART_ITEM_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(itemRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                .andExpect(jsonPath("$.uri").value(CART_ITEM_URI))
                .andExpect(jsonPath("$.fieldErrors.productId", containsInAnyOrder(CART_ITEM_PRODUCT_ID_IS_REQUIRED)));

        verifyNoInteractions(cartService);
    }

    @Test
    void addItem_whenProductIdIsZero_returnsBadRequest() throws Exception {
        Long userId = 2L;
        Long productId = 0L;
        int quantity = 2;

        CartItemRequest itemRequest = new CartItemRequest(
                productId,
                quantity
        );

        authenticateUser(userId);

        mockMvc.perform(post(CART_ITEM_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(itemRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                .andExpect(jsonPath("$.uri").value(CART_ITEM_URI))
                .andExpect(jsonPath("$.fieldErrors.productId",
                        containsInAnyOrder(CART_ITEM_PRODUCT_ID_IS_INVALID)
                ));

        verifyNoInteractions(cartService);
    }

    @Test
    void addItem_whenQuantityIsNull_returnsBadRequest() throws Exception {
        Long productId = 1L;
        Long userId = 2L;

        CartItemRequest itemRequest = new CartItemRequest(
                productId,
                null
        );

        authenticateUser(userId);

        mockMvc.perform(post(CART_ITEM_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(itemRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                .andExpect(jsonPath("$.uri").value(CART_ITEM_URI))
                .andExpect(jsonPath("$.fieldErrors.quantity", containsInAnyOrder(CART_ITEM_QUANTITY_IS_REQUIRED)));

        verifyNoInteractions(cartService);
    }

    @Test
    void addItem_whenQuantityIsLessThan1_returnsBadRequest() throws Exception {
        Long productId = 1L;
        Long userId = 2L;

        CartItemRequest itemRequest = new CartItemRequest(
                productId,
                0
        );

        authenticateUser(userId);

        mockMvc.perform(post(CART_ITEM_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(itemRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                .andExpect(jsonPath("$.uri").value(CART_ITEM_URI))
                .andExpect(jsonPath("$.fieldErrors.quantity", containsInAnyOrder(CART_ITEM_QUANTITY_IS_INVALID)));

        verifyNoInteractions(cartService);
    }

    @Test
    void updateItem_whenRequestIsValid_returnsCartResponse() throws Exception {
        Long userId = 1L;
        Long productId = 1L;
        int updatedQuantity = 4;

        BigDecimal updatedTotal = VALID_PRODUCT_PRICE.multiply(BigDecimal.valueOf(updatedQuantity));

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

        mockMvc.perform(patch(cartItemUri(productId))
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

        mockMvc.perform(patch(cartItemUri(productId))
                        .param("quantity", String.valueOf(quantity)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(invalidParameter("productId")))
                .andExpect(jsonPath("$.uri").value(cartItemUri(productId)));

        verifyNoInteractions(cartService);
    }

    @Test
    void updateItem_whenQuantityIsNegative_returnsBadRequest() throws Exception {
        Long userId = 1L;
        Long productId = 1L;
        int quantity = -1;

        authenticateUser(userId);

        mockMvc.perform(patch(cartItemUri(productId))
                        .param("quantity", String.valueOf(quantity)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                .andExpect(jsonPath("$.uri").value(cartItemUri(productId)))
                .andExpect(jsonPath("$.fieldErrors.quantity", containsInAnyOrder(invalidParameter("quantity"))));

        verifyNoInteractions(cartService);
    }

    @Test
    void updateItem_whenQuantityIsZero_returnsCartResponse() throws Exception {
        Long userId = 1L;
        Long productId = 1L;
        int quantity = 0;
        BigDecimal total = BigDecimal.ZERO;

        CartItemResponse itemResponse = new CartItemResponse(
                productId,
                VALID_PRODUCT_NAME,
                VALID_PRODUCT_PRICE,
                quantity,
                total
        );

        authenticateUser(userId);

        CartResponse cartResponse = new CartResponse(List.of(itemResponse), total);

        when(cartService.updateItemQuantity(userId, productId, quantity)).thenReturn(cartResponse);

        mockMvc.perform(patch(cartItemUri(productId))
                        .param("quantity", String.valueOf(quantity)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].productId").value(productId))
                .andExpect(jsonPath("$.items[0].productName").value(VALID_PRODUCT_NAME))
                .andExpect(jsonPath("$.items[0].unitPrice").value(VALID_PRODUCT_PRICE.doubleValue()))
                .andExpect(jsonPath("$.items[0].quantity").value(quantity))
                .andExpect(jsonPath("$.items[0].subtotal").value(total.doubleValue()));

        verify(cartService).updateItemQuantity(userId, productId, quantity);
        verifyNoMoreInteractions(cartService);
    }

    @Test
    void updateItem_whenQuantityIsMissing_returnsBadRequest() throws Exception {
        Long userId = 1L;
        Long productId = 1L;

        authenticateUser(userId);

        mockMvc.perform(patch(cartItemUri(productId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                .andExpect(jsonPath("$.uri").value(cartItemUri(productId)))
                .andExpect(jsonPath("$.fieldErrors.quantity", containsInAnyOrder(
                        invalidParameter("quantity"))));

        verifyNoInteractions(cartService);
    }

    @Test
    void updateItem_whenQuantityIsNotNumber_returnsBadRequest() throws Exception {
        Long userId = 1L;
        Long productId = 1L;
        String badQuantity = "abc";

        authenticateUser(userId);

        mockMvc.perform(patch(cartItemUri(productId))
                        .param("quantity", badQuantity))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(invalidParameter("quantity")))
                .andExpect(jsonPath("$.uri").value(cartItemUri(productId)));

        verifyNoInteractions(cartService);
    }

    @Test
    void updateItem_whenProductNotFound_returnsNotFound() throws Exception {
        Long userId = 1L;
        Long productId = 1L;
        int quantity = 2;

        authenticateUser(userId);

        when(cartService.updateItemQuantity(userId, productId, quantity))
                .thenThrow(new NoResourceFoundException(productNotFoundWithId(productId)));

        mockMvc.perform(patch(cartItemUri(productId))
                        .param("quantity", String.valueOf(quantity)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.NOT_FOUND.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(productNotFoundWithId(productId)))
                .andExpect(jsonPath("$.uri").value(cartItemUri(productId)));

        verify(cartService).updateItemQuantity(userId, productId, quantity);
        verifyNoMoreInteractions(cartService);
    }

    @Test
    void removeItem_whenRequestIsValid_returnsCartResponse() throws Exception {
        Long userId = 1L;
        Long productId = 1L;

        authenticateUser(userId);

        CartResponse cartResponse = new CartResponse(List.of(), BigDecimal.ZERO);

        when(cartService.removeItem(userId, productId)).thenReturn(cartResponse);

        mockMvc.perform(delete(cartItemUri(productId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items", hasSize(0)))
                .andExpect(jsonPath("$.total").value(BigDecimal.ZERO.doubleValue()));

        verify(cartService).removeItem(userId, productId);
        verifyNoMoreInteractions(cartService);
    }

    @Test
    void removeItem_whenProductIdIsInvalid_returnsBadRequest() throws Exception {
        Long userId = 1L;
        String productId = INVALID_ID;

        authenticateUser(userId);

        mockMvc.perform(delete(cartItemUri(productId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(invalidParameter("productId")))
                .andExpect(jsonPath("$.uri").value(cartItemUri(productId)));

        verifyNoInteractions(cartService);
    }

    @Test
    void removeItem_whenProductNotFound_returnsNotFound() throws Exception {
        Long userId = 1L;
        Long productId = 1L;

        when(cartService.removeItem(userId, productId))
                .thenThrow(new NoResourceFoundException(productNotFoundWithId(productId)));

        authenticateUser(userId);

        mockMvc.perform(delete(cartItemUri(productId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.NOT_FOUND.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(productNotFoundWithId(productId)))
                .andExpect(jsonPath("$.uri").value(cartItemUri(productId)));

        verify(cartService).removeItem(userId, productId);
        verifyNoMoreInteractions(cartService);
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
