package com.namnguyen.ecommerce_platform.integration.security;

import com.namnguyen.ecommerce_platform.cart.entity.Cart;
import com.namnguyen.ecommerce_platform.integration.BaseSecurityIntegrationTest;
import com.namnguyen.ecommerce_platform.order.dto.CreateOrderItemRequest;
import com.namnguyen.ecommerce_platform.order.dto.CreateOrderRequest;
import com.namnguyen.ecommerce_platform.order.entity.Order;
import com.namnguyen.ecommerce_platform.order.enums.OrderStatus;
import com.namnguyen.ecommerce_platform.product.entity.Product;
import com.namnguyen.ecommerce_platform.user.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.util.List;

import static com.namnguyen.ecommerce_platform.testutil.TestDataFactory.*;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class OrderSecurityIntegrationTest extends BaseSecurityIntegrationTest {

    @Test
    void createOrder_withCustomerJwt_returnsCreated() throws Exception {
        Product product = persistDefaultProduct();
        int boughtQuantity = 10;
        User user = persistDefaultCustomer();
        CreateOrderItemRequest orderItemRequest = createOrderItemRequest(product.getId(), boughtQuantity);

        CreateOrderRequest orderRequest = new CreateOrderRequest(
                List.of(orderItemRequest)
        );

        String token = loginAndGetToken(user.getEmail(), VALID_PASSWORD);

        mockMvc.perform(post(ORDER_URI)
                .header("Authorization", "Bearer "  + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").exists());
    }

    @Test
    void createOrder_withoutJwt_returnsUnauthorized() throws Exception {
        CreateOrderRequest orderRequest = new CreateOrderRequest(
                List.of(createOrderItemRequest(1L, 10))
        );

        mockMvc.perform(post(ORDER_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getOrders_withCustomerJwt_returnsOk() throws Exception {
        User user = persistDefaultCustomer();
        BigDecimal firstTotal = BigDecimal.valueOf(299.99);
        BigDecimal secondTotal = BigDecimal.valueOf(399.99);

        persistOrder(
                firstTotal,
                OrderStatus.PENDING_PAYMENT,
                user,
                List.of(),
                null
        );

        persistOrder(
                secondTotal,
                OrderStatus.PAID,
                user,
                List.of(),
                null
        );

        String token = loginAndGetToken(user.getEmail(), VALID_PASSWORD);

        mockMvc.perform(get(ORDER_URI)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(2)));
    }

    @Test
    void getOrders_withoutJwt_returnsUnauthorized() throws Exception {
        mockMvc.perform(get(ORDER_URI))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getOrderById_withCustomerJwt_returnsOk() throws Exception {
        User user = persistDefaultCustomer();
        BigDecimal total = BigDecimal.valueOf(299.99);

        Order order = persistOrder(
                total,
                OrderStatus.PENDING_PAYMENT,
                user,
                List.of(),
                null
        );

        String token = loginAndGetToken(user.getEmail(), VALID_PASSWORD);

        mockMvc.perform(get(orderUri(order.getId()))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").exists());
    }


    @Test
    void getOrderById_withoutJwt_returnsUnauthorized() throws Exception {
        long orderId = 1L;

        mockMvc.perform(get(orderUri(orderId)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void cancelOrder_withCustomerJwt_returnsNoContent() throws Exception {
        User user = persistDefaultCustomer();
        BigDecimal total = BigDecimal.valueOf(299.99);

        Order order = persistOrder(
                total,
                OrderStatus.PENDING_PAYMENT,
                user,
                List.of(),
                null
        );

        String token = loginAndGetToken(user.getEmail(), VALID_PASSWORD);

        mockMvc.perform(patch(cancelOrderUri(order.getId()))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    @Test
    void cancelOrder_withoutJwt_returnsUnauthorized() throws Exception {
        Long orderId = 1L;

        mockMvc.perform(patch(cancelOrderUri(orderId)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void checkoutCart_withCustomerJwt_returnsCreated() throws Exception {
        User user = persistDefaultCustomer();
        int quantity = 2;

        Product product = persistDefaultProduct();

        Cart cart = persistCart(user);
        persistCartItem(cart, product, quantity);

        String token = loginAndGetToken(user.getEmail(), VALID_PASSWORD);

        mockMvc.perform(post(CHECKOUT_URI)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").exists());
    }

    @Test
    void checkoutCart_withoutJwt_returnsUnauthorized() throws Exception {

        mockMvc.perform(post(CHECKOUT_URI))
                .andExpect(status().isUnauthorized());
    }
}
