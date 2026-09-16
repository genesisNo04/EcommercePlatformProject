package com.namnguyen.ecommerce_platform.order.controller;

import com.namnguyen.ecommerce_platform.cart.exception.InvalidCartStateException;
import com.namnguyen.ecommerce_platform.order.exception.InvalidOrderStateException;
import com.namnguyen.ecommerce_platform.common.exception.NoResourceFoundException;
import com.namnguyen.ecommerce_platform.common.rate_limit.RateLimitService;
import com.namnguyen.ecommerce_platform.order.dto.*;
import com.namnguyen.ecommerce_platform.order.enums.OrderStatus;
import com.namnguyen.ecommerce_platform.order.service.OrderService;
import com.namnguyen.ecommerce_platform.security.jwt.JwtService;
import com.namnguyen.ecommerce_platform.security.user.CustomUserDetailsService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static com.namnguyen.ecommerce_platform.testutil.MockAuthentication.*;
import static com.namnguyen.ecommerce_platform.testutil.TestDataFactory.*;
import static com.namnguyen.ecommerce_platform.testutil.messages.CommonTestMessages.VALIDATION_FAILED;
import static com.namnguyen.ecommerce_platform.testutil.messages.CommonTestMessages.invalidParameter;
import static com.namnguyen.ecommerce_platform.testutil.messages.OrderTestMessages.*;
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

    @MockitoBean
    private RateLimitService rateLimitService;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createOrder_whenRequestIsValid_returnOrderResponse() throws Exception {
        Long userId = 1L;
        Long firstProductId = 2L;
        Long secondProductId = 3L;
        Long orderId = 4L;
        int firstProductQuantity = 1;
        int secondProductQuantity = 2;

        CreateOrderItemRequest firstOrderItemRequest = new CreateOrderItemRequest(
                firstProductId,
                firstProductQuantity
        );

        OrderItemResponse firstItemResponse = new OrderItemResponse(
                firstProductId,
                VALID_PRODUCT_NAME,
                firstProductQuantity,
                VALID_PRODUCT_PRICE
        );

        CreateOrderItemRequest secondOrderItemRequest = new CreateOrderItemRequest(
                secondProductId,
                secondProductQuantity
        );

        OrderItemResponse secondItemResponse = new OrderItemResponse(
                secondProductId,
                VALID_PRODUCT_NAME,
                secondProductQuantity,
                VALID_PRODUCT_PRICE.add(BigDecimal.TEN)
        );

        BigDecimal firstItemTotal = firstItemResponse.price().multiply(BigDecimal.valueOf(firstItemResponse.quantity()));
        BigDecimal secondItemTotal = secondItemResponse.price().multiply(BigDecimal.valueOf(secondItemResponse.quantity()));
        BigDecimal total = firstItemTotal.add(secondItemTotal);

        CreateOrderRequest orderRequest = new CreateOrderRequest(List.of(firstOrderItemRequest, secondOrderItemRequest));

        OrderResponse orderResponse = new OrderResponse(
                orderId,
                userId,
                List.of(firstItemResponse, secondItemResponse),
                total,
                OrderStatus.PENDING_PAYMENT,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        authenticateUser(userId);

        when(orderService.createOrder(orderRequest, userId)).thenReturn(orderResponse);

        mockMvc.perform(post(ORDER_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value(orderId))
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[0].productId").value(firstProductId))
                .andExpect(jsonPath("$.items[0].productName").value(VALID_PRODUCT_NAME))
                .andExpect(jsonPath("$.items[0].quantity").value(firstProductQuantity))
                .andExpect(jsonPath("$.items[0].price").value(VALID_PRODUCT_PRICE.doubleValue()))
                .andExpect(jsonPath("$.items[1].productId").value(secondProductId))
                .andExpect(jsonPath("$.items[1].productName").value(VALID_PRODUCT_NAME))
                .andExpect(jsonPath("$.items[1].quantity").value(secondProductQuantity))
                .andExpect(jsonPath("$.items[1].price").value((VALID_PRODUCT_PRICE.add(BigDecimal.TEN)).doubleValue()))
                .andExpect(jsonPath("$.total").value(total.doubleValue()))
                .andExpect(jsonPath("$.status").value(OrderStatus.PENDING_PAYMENT.name()))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());

        verify(orderService).createOrder(orderRequest, userId);
        verifyNoMoreInteractions(orderService);
    }

    @Test
    void createOrder_whenProductIdIsNullForItem_returnsBadRequest() throws Exception {
        Long userId = 1L;
        int quantity = 1;

        CreateOrderItemRequest orderItemRequest = new CreateOrderItemRequest(
                null,
                quantity
        );

        CreateOrderRequest orderRequest = new CreateOrderRequest(List.of(orderItemRequest));

        authenticateUser(userId);

        mockMvc.perform(post(ORDER_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                .andExpect(jsonPath("$.uri").value(ORDER_URI))
                .andExpect(jsonPath("$.fieldErrors['items[0].productId']", containsInAnyOrder(ORDER_ITEM_PRODUCT_ID_IS_REQUIRED)));

        verifyNoInteractions(orderService);
    }

    @Test
    void createOrder_whenQuantityIsNegative_returnsBadRequest() throws Exception {
        Long userId = 1L;
        Long productId = 2L;
        int quantity = -1;

        CreateOrderItemRequest orderItemRequest = new CreateOrderItemRequest(
                productId,
                quantity
        );

        CreateOrderRequest orderRequest = new CreateOrderRequest(List.of(orderItemRequest));

        authenticateUser(userId);

        mockMvc.perform(post(ORDER_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                .andExpect(jsonPath("$.uri").value(ORDER_URI))
                .andExpect(jsonPath("$.fieldErrors['items[0].quantity']",
                        containsInAnyOrder(ORDER_ITEM_QUANTITY_IS_INVALID)));

        verifyNoInteractions(orderService);
    }

    @Test
    void createOrder_whenProductIdNotFound_returnsNotFound() throws Exception {
        Long userId = 1L;
        Long productId = 2L;
        int quantity = 1;

        CreateOrderItemRequest orderItemRequest = new CreateOrderItemRequest(
                productId,
                quantity
        );

        CreateOrderRequest orderRequest = new CreateOrderRequest(List.of(orderItemRequest));

        when(orderService.createOrder(orderRequest, userId))
                .thenThrow(new NoResourceFoundException(productNotFoundWithId(productId)));

        authenticateUser(userId);

        mockMvc.perform(post(ORDER_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.NOT_FOUND.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(productNotFoundWithId(productId)))
                .andExpect(jsonPath("$.uri").value(ORDER_URI));

        verify(orderService).createOrder(any(CreateOrderRequest.class), eq(userId));
        verifyNoMoreInteractions(orderService);
    }

    @Test
    void createOrder_whenQuantityIsNull_returnsBadRequest() throws Exception {
        Long userId = 1L;
        Long productId = 2L;

        CreateOrderItemRequest orderItemRequest = new CreateOrderItemRequest(
                productId,
                null
        );

        CreateOrderRequest orderRequest = new CreateOrderRequest(List.of(orderItemRequest));

        authenticateUser(userId);

        mockMvc.perform(post(ORDER_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                .andExpect(jsonPath("$.uri").value(ORDER_URI))
                .andExpect(jsonPath("$.fieldErrors['items[0].quantity']",
                        containsInAnyOrder(ORDER_ITEM_QUANTITY_IS_REQUIRED)));

        verifyNoInteractions(orderService);
    }

    @Test
    void createOrder_whenQuantityIsZero_returnsBadRequest() throws Exception {
        Long userId = 1L;
        Long productId = 2L;
        int quantity = 0;

        CreateOrderItemRequest orderItemRequest = new CreateOrderItemRequest(
                productId,
                quantity
        );

        CreateOrderRequest orderRequest = new CreateOrderRequest(List.of(orderItemRequest));

        authenticateUser(userId);

        mockMvc.perform(post(ORDER_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                .andExpect(jsonPath("$.uri").value(ORDER_URI))
                .andExpect(jsonPath("$.fieldErrors['items[0].quantity']",
                        containsInAnyOrder(ORDER_ITEM_QUANTITY_IS_INVALID)));

        verifyNoInteractions(orderService);
    }

    @Test
    void createOrder_whenOrderDoesNotHaveAnyItem_returnsBadRequest() throws Exception {
        Long userId = 1L;

        CreateOrderRequest orderRequest = new CreateOrderRequest(List.of());

        authenticateUser(userId);

        mockMvc.perform(post(ORDER_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                .andExpect(jsonPath("$.uri").value(ORDER_URI))
                .andExpect(jsonPath("$.fieldErrors.items",
                        containsInAnyOrder(ORDER_IS_EMPTY)));

        verifyNoInteractions(orderService);
    }

    @Test
    void createOrder_whenOrderItemsListIsNull_returnsBadRequest() throws Exception {
        Long userId = 1L;

        CreateOrderRequest orderRequest = new CreateOrderRequest(null);

        authenticateUser(userId);

        mockMvc.perform(post(ORDER_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                .andExpect(jsonPath("$.uri").value(ORDER_URI))
                .andExpect(jsonPath("$.fieldErrors.items",
                        containsInAnyOrder(ORDER_IS_EMPTY)));

        verifyNoInteractions(orderService);
    }

    @Test
    void createOrder_whenProductIdIsZero_returnsBadRequest() throws Exception {
        Long userId = 1L;
        Long productId = 0L;

        CreateOrderItemRequest orderItemRequest =
                new CreateOrderItemRequest(productId, 1);

        CreateOrderRequest orderRequest =
                new CreateOrderRequest(List.of(orderItemRequest));

        authenticateUser(userId);

        mockMvc.perform(post(ORDER_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.uri").value(ORDER_URI))
                .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                .andExpect(jsonPath("$.fieldErrors['items[0].productId']",
                        containsInAnyOrder(ORDER_ITEM_PRODUCT_ID_IS_INVALID)

                ));

        verifyNoInteractions(orderService);
    }

    @Test
    void getOrders_whenOrdersExists_returnsPageOfOrderResponse() throws Exception {
        Long userId = 1L;
        Long firstOrderId = 2L;
        Long secondOrderId = 3L;
        Long firstProductId = 4L;
        Long secondProductId = 5L;
        int firstProductQuantity = 2;
        int secondProductQuantity = 3;
        BigDecimal firstProductPrice = VALID_PRODUCT_PRICE;
        BigDecimal secondProductPrice = VALID_PRODUCT_PRICE.add(BigDecimal.TEN);
        BigDecimal firstOrderTotal = firstProductPrice.multiply(BigDecimal.valueOf(firstProductQuantity)).add(secondProductPrice.multiply(BigDecimal.valueOf(secondProductQuantity)));
        BigDecimal secondOrderTotal = secondProductPrice.multiply(BigDecimal.valueOf(secondProductQuantity));

        OrderItemResponse firstItemResponse = new OrderItemResponse(
                firstProductId,
                VALID_PRODUCT_NAME,
                firstProductQuantity,
                firstProductPrice
        );


        OrderItemResponse secondItemResponse = new OrderItemResponse(
                secondProductId,
                VALID_PRODUCT_NAME,
                secondProductQuantity,
                secondProductPrice
        );

        OrderResponse firstOrderResponse = new OrderResponse(
                firstOrderId,
                userId,
                List.of(firstItemResponse, secondItemResponse),
                firstOrderTotal,
                OrderStatus.PENDING_PAYMENT,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        OrderResponse secondOrderResponse = new OrderResponse(
                secondOrderId,
                userId,
                List.of(secondItemResponse),
                secondOrderTotal,
                OrderStatus.PENDING_PAYMENT,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        List<OrderResponse> orderResponses = List.of(firstOrderResponse, secondOrderResponse);

        Pageable pageable = PageRequest.of(0, 10);
        Page<OrderResponse> pageOrder = new PageImpl<>(orderResponses, pageable, orderResponses.size());

        when(orderService.getOrders(eq(userId), any(OrderFilterRequest.class), any(Pageable.class))).thenReturn(pageOrder);

        authenticateUser(userId);

        mockMvc.perform(get(ORDER_URI))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].orderId").value(firstOrderId))
                .andExpect(jsonPath("$.content[0].userId").value(userId))

                .andExpect(jsonPath("$.content[0].items").isArray())
                .andExpect(jsonPath("$.content[0].items", hasSize(2)))
                .andExpect(jsonPath("$.content[0].items[0].productId").value(firstProductId))
                .andExpect(jsonPath("$.content[0].items[0].productName").value(VALID_PRODUCT_NAME))
                .andExpect(jsonPath("$.content[0].items[0].quantity").value(firstProductQuantity))
                .andExpect(jsonPath("$.content[0].items[0].price").value(firstProductPrice.doubleValue()))
                .andExpect(jsonPath("$.content[0].items[1].productId").value(secondProductId))
                .andExpect(jsonPath("$.content[0].items[1].productName").value(VALID_PRODUCT_NAME))
                .andExpect(jsonPath("$.content[0].items[1].quantity").value(secondProductQuantity))
                .andExpect(jsonPath("$.content[0].items[1].price").value(secondProductPrice.doubleValue()))

                .andExpect(jsonPath("$.content[1].items").isArray())
                .andExpect(jsonPath("$.content[1].items", hasSize(1)))
                .andExpect(jsonPath("$.content[1].items[0].productId").value(secondProductId))
                .andExpect(jsonPath("$.content[1].items[0].productName").value(VALID_PRODUCT_NAME))
                .andExpect(jsonPath("$.content[1].items[0].quantity").value(secondProductQuantity))
                .andExpect(jsonPath("$.content[1].items[0].price").value(secondProductPrice.doubleValue()))

                .andExpect(jsonPath("$.content[0].total").value(firstOrderTotal.doubleValue()))
                .andExpect(jsonPath("$.content[0].status").value(OrderStatus.PENDING_PAYMENT.name()))
                .andExpect(jsonPath("$.content[0].createdAt").exists())
                .andExpect(jsonPath("$.content[0].updatedAt").exists())

                .andExpect(jsonPath("$.numberOfElements").value(2))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1));

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(orderService).getOrders(eq(userId), any(OrderFilterRequest.class), pageableCaptor.capture());

        Pageable capturedPageable = pageableCaptor.getValue();

        assertThat(capturedPageable.getSort()).contains(Sort.Order.desc("id"));
        assertThat(capturedPageable.getPageSize()).isEqualTo(10);
        assertThat(capturedPageable.getPageNumber()).isEqualTo(0);

        verifyNoMoreInteractions(orderService);
    }

    @Test
    void getOrders_whenStatusFilterIsInvalid_returnsBadRequest() throws Exception {
        Long userId = 1L;

        authenticateUser(userId);

        LocalDateTime createdAfter = LocalDateTime.now().minusDays(3);
        LocalDateTime createdBefore = LocalDateTime.now().plusDays(3);

        mockMvc.perform(get(ORDER_URI)
                        .param("status", "TESTING")
                        .param("minTotal", "0.0")
                        .param("maxTotal", "100.0")
                        .param("createdAfter", String.valueOf(createdAfter))
                        .param("createdBefore", String.valueOf(createdBefore)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                .andExpect(jsonPath("$.uri").value(ORDER_URI))
                .andExpect(jsonPath("$.fieldErrors.status",
                        containsInAnyOrder(invalidParameter("status"))));

        verifyNoInteractions(orderService);
    }

    @Test
    void getOrders_whenMinTotalIsInvalid_returnsBadRequest() throws Exception {
        Long userId = 1L;

        authenticateUser(userId);

        LocalDateTime createdAfter = LocalDateTime.now().minusDays(3);
        LocalDateTime createdBefore = LocalDateTime.now().plusDays(3);

        mockMvc.perform(get(ORDER_URI)
                        .param("status", OrderStatus.PENDING_PAYMENT.name())
                        .param("minTotal", "abc")
                        .param("maxTotal", "100.0")
                        .param("createdAfter", String.valueOf(createdAfter))
                        .param("createdBefore", String.valueOf(createdBefore)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                .andExpect(jsonPath("$.uri").value(ORDER_URI))
                .andExpect(jsonPath("$.fieldErrors.minTotal",
                        containsInAnyOrder(invalidParameter("minTotal"))));

        verifyNoInteractions(orderService);
    }

    @Test
    void getOrders_whenMaxTotalIsInvalid_returnsBadRequest() throws Exception {
        Long userId = 1L;

        authenticateUser(userId);

        LocalDateTime createdAfter = LocalDateTime.now().minusDays(3);
        LocalDateTime createdBefore = LocalDateTime.now().plusDays(3);

        mockMvc.perform(get(ORDER_URI)
                        .param("status", OrderStatus.PENDING_PAYMENT.name())
                        .param("minTotal", "0.0")
                        .param("maxTotal", "abc")
                        .param("createdAfter", String.valueOf(createdAfter))
                        .param("createdBefore", String.valueOf(createdBefore)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                .andExpect(jsonPath("$.uri").value(ORDER_URI))
                .andExpect(jsonPath("$.fieldErrors.maxTotal",
                        containsInAnyOrder(invalidParameter("maxTotal"))));

        verifyNoInteractions(orderService);
    }

    @Test
    void getOrders_whenCreatedAfterIsInvalid_returnsBadRequest() throws Exception {
        Long userId = 1L;

        authenticateUser(userId);

        LocalDateTime createdBefore = LocalDateTime.now().plusDays(3);

        mockMvc.perform(get(ORDER_URI)
                        .param("status", OrderStatus.PENDING_PAYMENT.name())
                        .param("minTotal", "0.0")
                        .param("maxTotal", "100.0")
                        .param("createdAfter", "abc")
                        .param("createdBefore", String.valueOf(createdBefore)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                .andExpect(jsonPath("$.uri").value(ORDER_URI))
                .andExpect(jsonPath("$.fieldErrors.createdAfter",
                        containsInAnyOrder(invalidParameter("createdAfter"))));

        verifyNoInteractions(orderService);
    }

    @Test
    void getOrders_whenCreatedBeforeIsInvalid_returnsBadRequest() throws Exception {
        Long userId = 1L;

        authenticateUser(userId);

        LocalDateTime createdAfter = LocalDateTime.now().minusDays(3);

        mockMvc.perform(get(ORDER_URI)
                        .param("status", OrderStatus.PENDING_PAYMENT.name())
                        .param("minTotal", "0.0")
                        .param("maxTotal", "100.0")
                        .param("createdAfter", String.valueOf(createdAfter))
                        .param("createdBefore", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                .andExpect(jsonPath("$.uri").value(ORDER_URI))
                .andExpect(jsonPath("$.fieldErrors.createdBefore",
                        containsInAnyOrder(invalidParameter("createdBefore"))));

        verifyNoInteractions(orderService);
    }


    @Test
    void getOrders_whenOrdersPageIsEmpty_returnsPageOfOrderResponse() throws Exception {
        Long userId = 1L;
        List<OrderResponse> orderResponses = List.of();

        Pageable pageable = PageRequest.of(0, 10);
        Page<OrderResponse> pageOrder = new PageImpl<>(orderResponses, pageable, orderResponses.size());

        when(orderService.getOrders(eq(userId), any(OrderFilterRequest.class), any(Pageable.class))).thenReturn(pageOrder);

        authenticateUser(userId);

        mockMvc.perform(get(ORDER_URI))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.numberOfElements").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0));

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(orderService).getOrders(eq(userId), any(OrderFilterRequest.class), pageableCaptor.capture());

        Pageable capturedPageable = pageableCaptor.getValue();

        assertThat(capturedPageable.getSort()).contains(Sort.Order.desc("id"));
        assertThat(capturedPageable.getPageSize()).isEqualTo(10);
        assertThat(capturedPageable.getPageNumber()).isEqualTo(0);

        verifyNoMoreInteractions(orderService);
    }

    @Test
    void getOrders_whenRequestHasFilters_returnsPageOfOrderResponse() throws Exception {
        Long userId = 1L;

        List<OrderResponse> orderResponses = List.of();

        Pageable pageable = PageRequest.of(0, 10);
        Page<OrderResponse> pageOrder = new PageImpl<>(orderResponses, pageable, orderResponses.size());

        when(orderService.getOrders(eq(userId), any(OrderFilterRequest.class), any(Pageable.class))).thenReturn(pageOrder);

        authenticateUser(userId);

        LocalDateTime createdAfter = LocalDateTime.now().minusDays(3);
        LocalDateTime createdBefore = LocalDateTime.now().plusDays(3);

        mockMvc.perform(get(ORDER_URI)
                        .param("status", OrderStatus.PENDING_PAYMENT.name())
                        .param("minTotal", "0.0")
                        .param("maxTotal", "100.0")
                        .param("createdAfter", String.valueOf(createdAfter))
                        .param("createdBefore", String.valueOf(createdBefore)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.numberOfElements").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0));

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        ArgumentCaptor<OrderFilterRequest> filterCaptor = ArgumentCaptor.forClass(OrderFilterRequest.class);

        verify(orderService).getOrders(eq(userId), filterCaptor.capture(), pageableCaptor.capture());

        Pageable capturedPageable = pageableCaptor.getValue();

        assertThat(capturedPageable.getSort()).contains(Sort.Order.desc("id"));
        assertThat(capturedPageable.getPageSize()).isEqualTo(10);
        assertThat(capturedPageable.getPageNumber()).isEqualTo(0);

        OrderFilterRequest orderFilterRequest = filterCaptor.getValue();
        assertThat(orderFilterRequest.status().name()).isEqualTo(OrderStatus.PENDING_PAYMENT.name());
        assertThat(orderFilterRequest.minTotal()).isEqualByComparingTo("0.0");
        assertThat(orderFilterRequest.maxTotal()).isEqualByComparingTo("100.0");
        assertThat(orderFilterRequest.createdAfter()).isEqualTo(createdAfter);
        assertThat(orderFilterRequest.createdBefore()).isEqualTo(createdBefore);

        verifyNoMoreInteractions(orderService);
    }

    @Test
    void getOrderById_whenRequestIsValid_returnsOrderResponse() throws Exception {
        Long userId = 1L;
        Long orderId = 2L;
        Long firstProductId = 4L;
        Long secondProductId = 5L;
        int firstProductQuantity = 2;
        int secondProductQuantity = 3;
        BigDecimal firstProductPrice = VALID_PRODUCT_PRICE;
        BigDecimal secondProductPrice = VALID_PRODUCT_PRICE.add(BigDecimal.TEN);
        BigDecimal total = firstProductPrice.multiply(BigDecimal.valueOf(firstProductQuantity)).add(secondProductPrice.multiply(BigDecimal.valueOf(secondProductQuantity)));

        OrderItemResponse firstItemResponse = new OrderItemResponse(
                firstProductId,
                VALID_PRODUCT_NAME,
                firstProductQuantity,
                firstProductPrice
        );


        OrderItemResponse secondItemResponse = new OrderItemResponse(
                secondProductId,
                VALID_PRODUCT_NAME,
                secondProductQuantity,
                secondProductPrice
        );

        OrderResponse orderResponse = new OrderResponse(
                orderId,
                userId,
                List.of(firstItemResponse, secondItemResponse),
                total,
                OrderStatus.PENDING_PAYMENT,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(orderService.getOrderById(orderId, userId)).thenReturn(orderResponse);

        authenticateUser(userId);

        mockMvc.perform(get(orderUri(orderId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(orderId))
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[0].productId").value(firstProductId))
                .andExpect(jsonPath("$.items[0].productName").value(VALID_PRODUCT_NAME))
                .andExpect(jsonPath("$.items[0].quantity").value(firstProductQuantity))
                .andExpect(jsonPath("$.items[0].price").value(firstProductPrice.doubleValue()))
                .andExpect(jsonPath("$.items[1].productId").value(secondProductId))
                .andExpect(jsonPath("$.items[1].productName").value(VALID_PRODUCT_NAME))
                .andExpect(jsonPath("$.items[1].quantity").value(secondProductQuantity))
                .andExpect(jsonPath("$.items[1].price").value(secondProductPrice.doubleValue()))
                .andExpect(jsonPath("$.total").value(total.doubleValue()))
                .andExpect(jsonPath("$.status").value(OrderStatus.PENDING_PAYMENT.name()))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());

        verify(orderService).getOrderById(orderId, userId);
        verifyNoMoreInteractions(orderService);
    }

    @Test
    void getOrderById_whenOrderIdIsInvalid_returnsBadRequest() throws Exception {
        Long userId = 1L;
        String orderId = INVALID_ID;

        authenticateUser(userId);

        mockMvc.perform(get(orderUri(orderId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(invalidParameter("orderId")))
                .andExpect(jsonPath("$.uri").value(orderUri(orderId)));


        verifyNoInteractions(orderService);
    }

    @Test
    void getOrderById_whenOrderNotFound_returnsNotFound() throws Exception {
        Long userId = 1L;
        Long orderId = 2L;

        when(orderService.getOrderById(orderId,userId))
                .thenThrow(new NoResourceFoundException(orderNotFoundWithIdAndUserId(orderId, userId)));

        authenticateUser(userId);

        mockMvc.perform(get(orderUri(orderId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.NOT_FOUND.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(orderNotFoundWithIdAndUserId(orderId, userId)))
                .andExpect(jsonPath("$.uri").value(orderUri(orderId)));


        verify(orderService).getOrderById(orderId, userId);
        verifyNoMoreInteractions(orderService);
    }

    @Test
    void cancelOrder_whenRequestIsValid_returnsNoContent() throws Exception {
        Long userId = 1L;
        Long orderId = 2L;

        authenticateUser(userId);

        mockMvc.perform(patch(cancelOrderUri(orderId)))
                .andExpect(status().isNoContent());

        verify(orderService).cancelOrder(orderId, userId);
        verifyNoMoreInteractions(orderService);
    }

    @Test
    void cancelOrder_whenOrderIdIsInvalid_returnsBadRequest() throws Exception {
        Long userId = 1L;
        String orderId = INVALID_ID;

        authenticateUser(userId);

        mockMvc.perform(patch(cancelOrderUri(orderId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(invalidParameter("orderId")))
                .andExpect(jsonPath("$.uri").value(cancelOrderUri(orderId)));


        verifyNoInteractions(orderService);
    }

    @Test
    void cancelOrder_whenOrderNotFound_returnsNotFound() throws Exception {
        Long userId = 1L;
        Long orderId = 2L;

        doThrow(new NoResourceFoundException(orderNotFoundWithIdAndUserId(orderId, userId)))
                .when(orderService).cancelOrder(orderId, userId);

        authenticateUser(userId);

        mockMvc.perform(patch(cancelOrderUri(orderId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.NOT_FOUND.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(orderNotFoundWithIdAndUserId(orderId, userId)))
                .andExpect(jsonPath("$.uri").value(cancelOrderUri(orderId)));


        verify(orderService).cancelOrder(orderId, userId);
        verifyNoMoreInteractions(orderService);
    }

    @Test
    void cancelOrder_whenOrderIsDelivered_returnsBadRequest() throws Exception {
        Long userId = 1L;
        Long orderId = 2L;

        doThrow(new InvalidOrderStateException(DELIVERED_ORDER_CANNOT_BE_CANCELLED))
                .when(orderService).cancelOrder(orderId, userId);

        authenticateUser(userId);

        mockMvc.perform(patch(cancelOrderUri(orderId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(DELIVERED_ORDER_CANNOT_BE_CANCELLED))
                .andExpect(jsonPath("$.uri").value(cancelOrderUri(orderId)));


        verify(orderService).cancelOrder(orderId, userId);
        verifyNoMoreInteractions(orderService);
    }

    @Test
    void cancelOrder_whenOrderIsCancelled_returnsBadRequest() throws Exception {
        Long userId = 1L;
        Long orderId = 2L;

        doThrow(new InvalidOrderStateException(ORDER_ALREADY_CANCELLED))
                .when(orderService).cancelOrder(orderId, userId);

        authenticateUser(userId);

        mockMvc.perform(patch(cancelOrderUri(orderId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(ORDER_ALREADY_CANCELLED))
                .andExpect(jsonPath("$.uri").value(cancelOrderUri(orderId)));


        verify(orderService).cancelOrder(orderId, userId);
        verifyNoMoreInteractions(orderService);
    }

    @Test
    void checkoutCart_whenRequestIsValid_returnsOrderResponse() throws Exception {
        Long userId = 1L;
        Long orderId = 2L;
        Long firstProductId = 4L;
        Long secondProductId = 5L;
        int firstProductQuantity = 2;
        int secondProductQuantity = 3;
        BigDecimal firstProductPrice = VALID_PRODUCT_PRICE;
        BigDecimal secondProductPrice = VALID_PRODUCT_PRICE.add(BigDecimal.TEN);
        BigDecimal total = firstProductPrice.multiply(BigDecimal.valueOf(firstProductQuantity)).add(secondProductPrice.multiply(BigDecimal.valueOf(secondProductQuantity)));

        OrderItemResponse firstItemResponse = new OrderItemResponse(
                firstProductId,
                VALID_PRODUCT_NAME,
                firstProductQuantity,
                firstProductPrice
        );


        OrderItemResponse secondItemResponse = new OrderItemResponse(
                secondProductId,
                VALID_PRODUCT_NAME,
                secondProductQuantity,
                secondProductPrice
        );

        OrderResponse orderResponse = new OrderResponse(
                orderId,
                userId,
                List.of(firstItemResponse, secondItemResponse),
                total,
                OrderStatus.PENDING_PAYMENT,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(orderService.checkoutCart(userId)).thenReturn(orderResponse);

        authenticateUser(userId);

        mockMvc.perform(post(CHECKOUT_URI))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value(orderId))
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[0].productId").value(firstProductId))
                .andExpect(jsonPath("$.items[0].productName").value(VALID_PRODUCT_NAME))
                .andExpect(jsonPath("$.items[0].quantity").value(firstProductQuantity))
                .andExpect(jsonPath("$.items[0].price").value(firstProductPrice.doubleValue()))
                .andExpect(jsonPath("$.items[1].productId").value(secondProductId))
                .andExpect(jsonPath("$.items[1].productName").value(VALID_PRODUCT_NAME))
                .andExpect(jsonPath("$.items[1].quantity").value(secondProductQuantity))
                .andExpect(jsonPath("$.items[1].price").value(secondProductPrice.doubleValue()))
                .andExpect(jsonPath("$.total").value(total.doubleValue()))
                .andExpect(jsonPath("$.status").value(OrderStatus.PENDING_PAYMENT.name()))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());

        verify(orderService).checkoutCart(userId);
        verifyNoMoreInteractions(orderService);
    }

    @Test
    void checkoutCart_whenCartIsEmpty_returnsBadRequest() throws Exception {
        Long userId = 1L;

        doThrow(new InvalidCartStateException(EMPTY_CART))
                .when(orderService).checkoutCart(userId);

        authenticateUser(userId);

        mockMvc.perform(post(CHECKOUT_URI))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(EMPTY_CART))
                .andExpect(jsonPath("$.uri").value(CHECKOUT_URI));


        verify(orderService).checkoutCart(userId);
        verifyNoMoreInteractions(orderService);
    }
}
