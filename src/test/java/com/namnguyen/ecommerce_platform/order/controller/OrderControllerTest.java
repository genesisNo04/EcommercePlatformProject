package com.namnguyen.ecommerce_platform.order.controller;

import com.namnguyen.ecommerce_platform.common.exception.NoResourceFoundException;
import com.namnguyen.ecommerce_platform.order.dto.CreateOrderItemRequest;
import com.namnguyen.ecommerce_platform.order.dto.CreateOrderRequest;
import com.namnguyen.ecommerce_platform.order.dto.OrderItemResponse;
import com.namnguyen.ecommerce_platform.order.dto.OrderResponse;
import com.namnguyen.ecommerce_platform.order.enums.OrderStatus;
import com.namnguyen.ecommerce_platform.order.service.OrderService;
import com.namnguyen.ecommerce_platform.security.jwt.JwtService;
import com.namnguyen.ecommerce_platform.security.user.CustomUserDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static com.namnguyen.ecommerce_platform.testutil.MockAuthentication.*;
import static com.namnguyen.ecommerce_platform.testutil.TestDataFactory.*;
import static com.namnguyen.ecommerce_platform.testutil.TestMessages.*;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.containsInAnyOrder;

@WebMvcTest(OrderController.class)
@AutoConfigureMockMvc(addFilters = false)
public class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void createOrder_whenRequestIsValid_returnOrderResponse() throws Exception {
        Long userId = 1L;
        Long productId = 2L;
        Long productId1 = 3L;
        Long orderId = 4L;
        int quantity = 1;
        int quantity1 = 2;

        CreateOrderItemRequest orderItemRequest = new CreateOrderItemRequest(
                productId,
                quantity
        );

        OrderItemResponse itemResponse = new OrderItemResponse(
                productId,
                VALID_PRODUCT_NAME,
                quantity,
                VALID_PRODUCT_PRICE
        );

        CreateOrderItemRequest orderItemRequest1 = new CreateOrderItemRequest(
                productId1,
                quantity1
        );

        OrderItemResponse itemResponse1 = new OrderItemResponse(
                productId1,
                VALID_PRODUCT_NAME,
                quantity1,
                VALID_PRODUCT_PRICE.add(BigDecimal.TEN)
        );

        BigDecimal totalItem = itemResponse.price().multiply(BigDecimal.valueOf(itemResponse.quantity()));
        BigDecimal totalItem1 = itemResponse1.price().multiply(BigDecimal.valueOf(itemResponse1.quantity()));
        BigDecimal total = totalItem.add(totalItem1);

        CreateOrderRequest request = new CreateOrderRequest(List.of(orderItemRequest, orderItemRequest1));

        OrderResponse response = new OrderResponse(
                orderId,
                userId,
                List.of(itemResponse, itemResponse1),
                total,
                OrderStatus.PENDING_PAYMENT,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        authenticateUser(userId);

        when(orderService.createOrder(request, userId)).thenReturn(response);

        mockMvc.perform(post(ORDER_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value(orderId))
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[0].productId").value(productId))
                .andExpect(jsonPath("$.items[0].productName").value(VALID_PRODUCT_NAME))
                .andExpect(jsonPath("$.items[0].quantity").value(quantity))
                .andExpect(jsonPath("$.items[0].price").value(VALID_PRODUCT_PRICE))
                .andExpect(jsonPath("$.items[1].productId").value(productId1))
                .andExpect(jsonPath("$.items[1].productName").value(VALID_PRODUCT_NAME))
                .andExpect(jsonPath("$.items[1].quantity").value(quantity1))
                .andExpect(jsonPath("$.items[1].price").value(VALID_PRODUCT_PRICE.add(BigDecimal.TEN)))
                .andExpect(jsonPath("$.total").value(total.doubleValue()))
                .andExpect(jsonPath("$.status").value(OrderStatus.PENDING_PAYMENT.name()))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());

        verify(orderService).createOrder(request, userId);
        verifyNoMoreInteractions(orderService);
    }

    @Test
    void createOrder_whenProductIdIsNullForItem_returnOrderResponse() throws Exception {
        Long userId = 1L;
        int quantity = 1;

        CreateOrderItemRequest orderItemRequest = new CreateOrderItemRequest(
                null,
                quantity
        );

        CreateOrderRequest request = new CreateOrderRequest(List.of(orderItemRequest));

        authenticateUser(userId);

        mockMvc.perform(post(ORDER_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.uri").value(ORDER_URI))
                .andExpect(jsonPath("$.fieldErrors['items[0].productId']", containsInAnyOrder(productIdIsRequired())));

        verifyNoInteractions(orderService);
    }

    @Test
    void createOrder_whenProductIdNotFound_returnOrderResponse() throws Exception {
        Long userId = 1L;
        Long productId = 2L;
        int quantity = 1;

        CreateOrderItemRequest orderItemRequest = new CreateOrderItemRequest(
                productId,
                quantity
        );

        CreateOrderRequest request = new CreateOrderRequest(List.of(orderItemRequest));

        when(orderService.createOrder(request, userId))
                .thenThrow(new NoResourceFoundException(productNotFound(productId)));

        authenticateUser(userId);

        mockMvc.perform(post(ORDER_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.NOT_FOUND.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(productNotFound(productId)))
                .andExpect(jsonPath("$.uri").value(ORDER_URI));

        verify(orderService).createOrder(any(CreateOrderRequest.class), eq(userId));
        verifyNoMoreInteractions(orderService);
    }

    @Test
    void createOrder_whenQuantityIsNull_returnOrderResponse() throws Exception {
        Long userId = 1L;
        Long productId = 2L;

        CreateOrderItemRequest orderItemRequest = new CreateOrderItemRequest(
                productId,
                null
        );

        CreateOrderRequest request = new CreateOrderRequest(List.of(orderItemRequest));

        when(orderService.createOrder(request, userId))
                .thenThrow(new NoResourceFoundException(productNotFound(productId)));

        authenticateUser(userId);

        mockMvc.perform(post(ORDER_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.uri").value(ORDER_URI))
                .andExpect(jsonPath("$.fieldErrors['items[0].quantity']").value(quantityIsRequired()));

        verifyNoInteractions(orderService);
    }

    @Test
    void createOrder_whenQuantityIsZero_returnOrderResponse() throws Exception {
        Long userId = 1L;
        Long productId = 2L;
        int quantity = 0;

        CreateOrderItemRequest orderItemRequest = new CreateOrderItemRequest(
                productId,
                quantity
        );

        CreateOrderRequest request = new CreateOrderRequest(List.of(orderItemRequest));

        when(orderService.createOrder(request, userId))
                .thenThrow(new NoResourceFoundException(productNotFound(productId)));

        authenticateUser(userId);

        mockMvc.perform(post(ORDER_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.uri").value(ORDER_URI))
                .andExpect(jsonPath("$.fieldErrors['items[0].quantity']").value(quantityIsZero()));

        verifyNoInteractions(orderService);
    }

    @Test
    void createOrder_whenOrderDoesNotHaveAnyItem_returnOrderResponse() throws Exception {
        Long userId = 1L;

        CreateOrderRequest request = new CreateOrderRequest(List.of());

        authenticateUser(userId);

        mockMvc.perform(post(ORDER_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.uri").value(ORDER_URI))
                .andExpect(jsonPath("fieldErrors.items", containsInAnyOrder(orderHasAtLeastOneItem())));

        verifyNoInteractions(orderService);
    }

    @Test
    void createOrder_whenOrderItemsListIsNull_returnOrderResponse() throws Exception {
        Long userId = 1L;

        CreateOrderRequest request = new CreateOrderRequest(null);

        authenticateUser(userId);

        mockMvc.perform(post(ORDER_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.uri").value(ORDER_URI))
                .andExpect(jsonPath("fieldErrors.items", containsInAnyOrder(orderHasAtLeastOneItem())));

        verifyNoInteractions(orderService);
    }
}
