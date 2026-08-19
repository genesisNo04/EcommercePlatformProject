package com.namnguyen.ecommerce_platform.integration.order;

import com.namnguyen.ecommerce_platform.integration.BaseIntegrationTest;
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

import static com.namnguyen.ecommerce_platform.testutil.MockAuthentication.*;
import static com.namnguyen.ecommerce_platform.testutil.TestDataFactory.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class OrderIntegrationTest extends BaseIntegrationTest {

    @Test
    void createOrder_withValidRequest_createOrderInDatabase() throws Exception {
        Product product = createDefaultProduct();
        User user = createDefaultCustomer();
        CreateOrderItemRequest itemRequest = createCreateOrderItemRequest(product.getId(), 10);

        BigDecimal total = product.getPrice().multiply(BigDecimal.valueOf(10));

        CreateOrderRequest request = new CreateOrderRequest(
                List.of(itemRequest)
        );

        authenticateUser(user.getId());

        mockMvc.perform(post(ORDER_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").exists())
                .andExpect(jsonPath("$.userId").value(user.getId()))
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].productId").value(product.getId()))
                .andExpect(jsonPath("$.items[0].productName").value(product.getName()))
                .andExpect(jsonPath("$.items[0].quantity").value(10))
                .andExpect(jsonPath("$.items[0].price").value(product.getPrice().doubleValue()))
                .andExpect(jsonPath("$.total").value(total.doubleValue()))
                .andExpect(jsonPath("$.status").value(OrderStatus.PENDING_PAYMENT.name()))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());

        Order order = orderRepository.findById(1L).orElseThrow();

        assertThat(order.getUser().getId()).isEqualTo(user.getId());
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        assertThat(order.getTotal()).isEqualByComparingTo(total);
    }

    @Test
    void createOrder_whenProductNotFound_returnsNotFounf() throws Exception {
        User user = createDefaultCustomer();
        CreateOrderItemRequest itemRequest = createCreateOrderItemRequest(1L, 10);

        CreateOrderRequest request = new CreateOrderRequest(
                List.of(itemRequest)
        );

        authenticateUser(user.getId());

        mockMvc.perform(post(ORDER_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void getOrders_whenOrdersExits_returnsOk() throws Exception {
        User user = createDefaultCustomer();
        BigDecimal total = BigDecimal.valueOf(299.99);
        BigDecimal total1 = BigDecimal.valueOf(399.99);

        Order order = createOrder(
                total,
                OrderStatus.PENDING_PAYMENT,
                user,
                List.of(),
                null
        );

        Order order1 = createOrder(
                total1,
                OrderStatus.PAID,
                user,
                List.of(),
                null
        );

        authenticateUser(user.getId());

        mockMvc.perform(get(ORDER_URI))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].orderId").value(order1.getId()))
                .andExpect(jsonPath("$.content[0].userId").value(user.getId()))
                .andExpect(jsonPath("$.content[0].total").value(order1.getTotal().doubleValue()))
                .andExpect(jsonPath("$.content[0].status").value(order1.getStatus().name()))
                .andExpect(jsonPath("$.content[1].orderId").value(order.getId()))
                .andExpect(jsonPath("$.content[1].userId").value(user.getId()))
                .andExpect(jsonPath("$.content[1].total").value(order.getTotal().doubleValue()))
                .andExpect(jsonPath("$.content[1].status").value(order.getStatus().name()));
    }

    @Test
    void getOrders_whenUserHasNoOrders_returnsOk() throws Exception {
        User user = createDefaultCustomer();

        authenticateUser(user.getId());

        mockMvc.perform(get(ORDER_URI))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test
    void getOrderById_whenOrdersExits_returnsOk() throws Exception {
        User user = createDefaultCustomer();
        BigDecimal total = BigDecimal.valueOf(299.99);

        Order order = createOrder(
                total,
                OrderStatus.PENDING_PAYMENT,
                user,
                List.of(),
                null
        );

        authenticateUser(user.getId());

        mockMvc.perform(get(ORDER_URI + "/" + order.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(order.getId()))
                .andExpect(jsonPath("$.userId").value(user.getId()))
                .andExpect(jsonPath("$.total").value(order.getTotal().doubleValue()))
                .andExpect(jsonPath("$.status").value(order.getStatus().name()));
    }

    @Test
    void getOrderById_whenOrdersNotFound_returnsNotFound() throws Exception {
        User user = createDefaultCustomer();
        authenticateUser(user.getId());

        mockMvc.perform(get(ORDER_URI + "/1"))
                .andExpect(status().isNotFound());
    }
}
