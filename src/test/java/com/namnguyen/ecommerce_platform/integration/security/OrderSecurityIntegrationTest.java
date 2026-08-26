package com.namnguyen.ecommerce_platform.integration.security;

import com.namnguyen.ecommerce_platform.cart.entity.Cart;
import com.namnguyen.ecommerce_platform.integration.BaseSecurityIntegrationTest;
import com.namnguyen.ecommerce_platform.order.dto.CreateOrderItemRequest;
import com.namnguyen.ecommerce_platform.order.dto.CreateOrderRequest;
import com.namnguyen.ecommerce_platform.order.entity.Order;
import com.namnguyen.ecommerce_platform.order.enums.OrderStatus;
import com.namnguyen.ecommerce_platform.product.entity.Product;
import com.namnguyen.ecommerce_platform.user.entity.User;
import com.namnguyen.ecommerce_platform.user.enums.Role;
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
    void createOrder_withJwt_returnsOrderResponse() throws Exception {
        Product product = createDefaultProduct();
        int boughtQuantity = 10;
        User user = createDefaultCustomer();
        CreateOrderItemRequest itemRequest = createCreateOrderItemRequest(product.getId(), boughtQuantity);

        CreateOrderRequest request = new CreateOrderRequest(
                List.of(itemRequest)
        );

        String token = loginAndGetToken(user.getEmail(), VALID_PASSWORD);

        mockMvc.perform(post(ORDER_URI)
                .header("Authorization", "Bearer "  + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").exists());
    }

    @Test
    void createOrder_withInvalidJwt_returnsUnauthorized() throws Exception {
        Product product = createDefaultProduct();
        int boughtQuantity = 10;
        User user = createDefaultCustomer();
        CreateOrderItemRequest itemRequest = createCreateOrderItemRequest(product.getId(), boughtQuantity);

        CreateOrderRequest request = new CreateOrderRequest(
                List.of(itemRequest)
        );

        String token = loginAndGetToken(user.getEmail(), VALID_PASSWORD) + "abc";

        mockMvc.perform(post(ORDER_URI)
                        .header("Authorization", "Bearer "  + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createOrder_withoutJwt_returnsUnauthorized() throws Exception {
        Product product = createDefaultProduct();
        int boughtQuantity = 10;
        CreateOrderItemRequest itemRequest = createCreateOrderItemRequest(product.getId(), boughtQuantity);

        CreateOrderRequest request = new CreateOrderRequest(
                List.of(itemRequest)
        );

        mockMvc.perform(post(ORDER_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getOrders_withJwt_returnsOk() throws Exception {
        User user = createDefaultCustomer();
        BigDecimal total = BigDecimal.valueOf(299.99);
        BigDecimal total1 = BigDecimal.valueOf(399.99);

        createOrder(
                total,
                OrderStatus.PENDING_PAYMENT,
                user,
                List.of(),
                null
        );

        createOrder(
                total1,
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
    void getOrderById_withJwt_returnsOk() throws Exception {
        User user = createDefaultCustomer();
        BigDecimal total = BigDecimal.valueOf(299.99);

        Order order = createOrder(
                total,
                OrderStatus.PENDING_PAYMENT,
                user,
                List.of(),
                null
        );

        String token = loginAndGetToken(user.getEmail(), VALID_PASSWORD);

        mockMvc.perform(get(ORDER_URI + "/" + order.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").exists());
    }


    @Test
    void getOrderById_withoutJwt_returnsUnauthorized() throws Exception {
        long orderId = 1L;

        mockMvc.perform(get(ORDER_URI + "/" + orderId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getOrderById_withDifferentUsersJwt_returnsNotFound()
            throws Exception {

        User owner = createDefaultCustomer();

        User otherUser = createUser(
                "otheruser@gmail.com",
                VALID_PASSWORD,
                "Other",
                "User",
                "1234567892",
                Role.CUSTOMER
        );

        Order order = createOrder(
                BigDecimal.valueOf(299.99),
                OrderStatus.PENDING_PAYMENT,
                owner,
                List.of(),
                null
        );

        String token = loginAndGetToken(
                otherUser.getEmail(),
                VALID_PASSWORD
        );

        mockMvc.perform(get(ORDER_URI + "/" + order.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void cancelOrder_withJwt_returnsNoContent() throws Exception {
        User user = createDefaultCustomer();
        BigDecimal total = BigDecimal.valueOf(299.99);

        Order order = createOrder(
                total,
                OrderStatus.PENDING_PAYMENT,
                user,
                List.of(),
                null
        );

        String token = loginAndGetToken(user.getEmail(), VALID_PASSWORD);

        mockMvc.perform(patch(ORDER_URI + "/" + order.getId() + "/cancel")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    @Test
    void cancelOrder_withoutJwt_returnsUnauthorized() throws Exception {
        Long orderId = 1L;

        mockMvc.perform(patch(ORDER_URI + "/" + orderId + "/cancel"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void checkoutCart_withJwt_returnsOrderResponse() throws Exception {
        User user = createDefaultCustomer();
        int quantity = 2;

        Product product = createDefaultProduct();

        Cart cart = createCart(user);
        createCartItem(cart, product, quantity);

        String token = loginAndGetToken(user.getEmail(), VALID_PASSWORD);

        mockMvc.perform(post(ORDER_URI + "/checkout")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").exists());
    }

    @Test
    void checkoutCart_withoutJwt_returnsUnauthorized() throws Exception {

        mockMvc.perform(post(ORDER_URI + "/checkout"))
                .andExpect(status().isUnauthorized());
    }
}
