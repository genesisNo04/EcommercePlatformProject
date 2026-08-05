package com.namnguyen.ecommerce_platform.cart.controller;

import com.namnguyen.ecommerce_platform.cart.dto.CartItemRequest;
import com.namnguyen.ecommerce_platform.cart.dto.CartItemResponse;
import com.namnguyen.ecommerce_platform.cart.service.CartService;
import com.namnguyen.ecommerce_platform.security.jwt.JwtService;
import com.namnguyen.ecommerce_platform.security.user.CustomUserDetails;
import com.namnguyen.ecommerce_platform.security.user.CustomUserDetailsService;
import com.namnguyen.ecommerce_platform.user.entity.User;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;

import static com.namnguyen.ecommerce_platform.testutil.TestDataFactory.*;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static com.namnguyen.ecommerce_platform.testutil.TestMessages.*;
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

    @Test
    void addItem_whenRequestIsValid_returnCartItemResponse() throws Exception {
        Long productId = 1L;
        Long userId = 2L;
        int quantity = 2;

        CartItemRequest request = new CartItemRequest(
                productId,
                quantity
        );

        User user = new User();

        CustomUserDetails userDetails = new CustomUserDetails(user);

        BigDecimal total = VALID_PRODUCT_PRICE.multiply(BigDecimal.valueOf(VALID_PRODUCT_QUANTITY));

        CartItemResponse response = new CartItemResponse(
                productId,
                VALID_PRODUCT_NAME,
                VALID_PRODUCT_PRICE,
                quantity,
                total
        );

        when(cartService.addItem(userId, request)).thenReturn(response);

        mockMvc.perform(post(CART_ITEM_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(productId))
                .andExpect(jsonPath("$.productName").value(VALID_PRODUCT_NAME))
                .andExpect(jsonPath("$.unitPrice").value(VALID_PRODUCT_PRICE))
                .andExpect(jsonPath("$.quantity").value(quantity))
                .andExpect(jsonPath("$.subtotal").value(total));

        ArgumentCaptor<CartItemRequest> captor = ArgumentCaptor.forClass(CartItemRequest.class);
        verify(cartService).addItem(eq(userId), captor.capture());

        CartItemRequest requestCaptured = captor.getValue();

        assertThat(requestCaptured.productId()).isEqualTo(productId);
        assertThat(requestCaptured.quantity()).isEqualTo(quantity);

        verifyNoMoreInteractions(cartService);
    }
}
