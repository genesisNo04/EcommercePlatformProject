package com.namnguyen.ecommerce_platform.order.controller;

import com.namnguyen.ecommerce_platform.common.exception.InvalidOrderStateException;
import com.namnguyen.ecommerce_platform.common.exception.NoResourceFoundException;
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

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

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
                .andExpect(jsonPath("$.items[0].price").value(VALID_PRODUCT_PRICE.doubleValue()))
                .andExpect(jsonPath("$.items[1].productId").value(productId1))
                .andExpect(jsonPath("$.items[1].productName").value(VALID_PRODUCT_NAME))
                .andExpect(jsonPath("$.items[1].quantity").value(quantity1))
                .andExpect(jsonPath("$.items[1].price").value((VALID_PRODUCT_PRICE.add(BigDecimal.TEN)).doubleValue()))
                .andExpect(jsonPath("$.total").value(total.doubleValue()))
                .andExpect(jsonPath("$.status").value(OrderStatus.PENDING_PAYMENT.name()))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());

        verify(orderService).createOrder(request, userId);
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
    void createOrder_whenQuantityIsNegative_returnsBadRequest() throws Exception {
        Long userId = 1L;
        int quantity = -1;

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
                .andExpect(jsonPath("$.fieldErrors['items[0].quantity']", containsInAnyOrder(invalidQuantity())));

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
    void createOrder_whenQuantityIsNull_returnsBadRequest() throws Exception {
        Long userId = 1L;
        Long productId = 2L;

        CreateOrderItemRequest orderItemRequest = new CreateOrderItemRequest(
                productId,
                null
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
                .andExpect(jsonPath("$.fieldErrors['items[0].quantity']").value(quantityIsRequired()));

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
                .andExpect(jsonPath("$.fieldErrors['items[0].quantity']").value(invalidQuantity()));

        verifyNoInteractions(orderService);
    }

    @Test
    void createOrder_whenOrderDoesNotHaveAnyItem_returnsBadRequest() throws Exception {
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
                .andExpect(jsonPath("$.fieldErrors.items",
                        containsInAnyOrder(orderHasAtLeastOneItem())));

        verifyNoInteractions(orderService);
    }

    @Test
    void createOrder_whenOrderItemsListIsNull_returnsBadRequest() throws Exception {
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
                .andExpect(jsonPath("$.fieldErrors.items",
                        containsInAnyOrder(orderHasAtLeastOneItem())));

        verifyNoInteractions(orderService);
    }

    @Test
    void getOrders_whenOrdersExists_returnsPageOfOrderResponse() throws Exception {
        Long userId = 1L;
        Long orderId1 = 2L;
        Long orderId2 = 3L;
        Long productId1 = 4L;
        Long productId2 = 5L;
        int quantity1 = 2;
        int quantity2 = 3;
        BigDecimal price1 = VALID_PRODUCT_PRICE;
        BigDecimal price2 = VALID_PRODUCT_PRICE.add(BigDecimal.TEN);
        BigDecimal total1 = price1.multiply(BigDecimal.valueOf(quantity1)).add(price2.multiply(BigDecimal.valueOf(quantity2)));
        BigDecimal total2 = price2.multiply(BigDecimal.valueOf(quantity2));

        OrderItemResponse itemResponse1 = new OrderItemResponse(
                productId1,
                VALID_PRODUCT_NAME,
                quantity1,
                price1
        );


        OrderItemResponse itemResponse2 = new OrderItemResponse(
                productId2,
                VALID_PRODUCT_NAME,
                quantity2,
                price2
        );

        OrderResponse orderResponse1 = new OrderResponse(
                orderId1,
                userId,
                List.of(itemResponse1, itemResponse2),
                total1,
                OrderStatus.PENDING_PAYMENT,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        OrderResponse orderResponse2 = new OrderResponse(
                orderId2,
                userId,
                List.of(itemResponse2),
                total2,
                OrderStatus.PENDING_PAYMENT,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        List<OrderResponse> responses = List.of(orderResponse1, orderResponse2);

        Pageable pageable = PageRequest.of(0, 10);
        Page<OrderResponse> pageOrder = new PageImpl<>(responses, pageable, responses.size());

        when(orderService.getOrders(eq(userId), any(OrderFilterRequest.class), any(Pageable.class))).thenReturn(pageOrder);

        authenticateUser(userId);

        mockMvc.perform(get(ORDER_URI))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].orderId").value(orderId1))
                .andExpect(jsonPath("$.content[0].userId").value(userId))

                .andExpect(jsonPath("$.content[0].items").isArray())
                .andExpect(jsonPath("$.content[0].items", hasSize(2)))
                .andExpect(jsonPath("$.content[0].items[0].productId").value(productId1))
                .andExpect(jsonPath("$.content[0].items[0].productName").value(VALID_PRODUCT_NAME))
                .andExpect(jsonPath("$.content[0].items[0].quantity").value(quantity1))
                .andExpect(jsonPath("$.content[0].items[0].price").value(price1.doubleValue()))
                .andExpect(jsonPath("$.content[0].items[1].productId").value(productId2))
                .andExpect(jsonPath("$.content[0].items[1].productName").value(VALID_PRODUCT_NAME))
                .andExpect(jsonPath("$.content[0].items[1].quantity").value(quantity2))
                .andExpect(jsonPath("$.content[0].items[1].price").value(price2.doubleValue()))

                .andExpect(jsonPath("$.content[1].items").isArray())
                .andExpect(jsonPath("$.content[1].items", hasSize(1)))
                .andExpect(jsonPath("$.content[1].items[0].productId").value(productId2))
                .andExpect(jsonPath("$.content[1].items[0].productName").value(VALID_PRODUCT_NAME))
                .andExpect(jsonPath("$.content[1].items[0].quantity").value(quantity2))
                .andExpect(jsonPath("$.content[1].items[0].price").value(price2.doubleValue()))

                .andExpect(jsonPath("$.content[0].total").value(total1))
                .andExpect(jsonPath("$.content[0].status").value(OrderStatus.PENDING_PAYMENT.name()))
                .andExpect(jsonPath("$.content[0].createdAt").exists())
                .andExpect(jsonPath("$.content[0].updatedAt").exists())

                .andExpect(jsonPath("$.numberOfElements").value(2))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(orderService).getOrders(eq(userId), any(OrderFilterRequest.class), captor.capture());

        Pageable capturePageable = captor.getValue();

        assertThat(capturePageable.getSort()).contains(Sort.Order.desc("id"));
        assertThat(capturePageable.getPageSize()).isEqualTo(10);
        assertThat(capturePageable.getPageNumber()).isEqualTo(0);

        verifyNoMoreInteractions(orderService);
    }

    @Test
    void getOrders_whenStatusFilterIsInvalid_returnsBadRequest() throws Exception {
        Long userId = 1L;

        authenticateUser(userId);

        LocalDateTime futureDate = LocalDateTime.now().minusDays(3);
        LocalDateTime pastDate = LocalDateTime.now().plusDays(3);

        mockMvc.perform(get(ORDER_URI)
                        .param("status", "TESTING")
                        .param("minTotal", "0.0")
                        .param("maxTotal", "100.0")
                        .param("createdAfter", String.valueOf(futureDate))
                        .param("createdBefore", String.valueOf(pastDate)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.uri").value(ORDER_URI))
                .andExpect(jsonPath("$.fieldErrors.status",
                        containsInAnyOrder(invalidParameter("status"))));

        verifyNoInteractions(orderService);
    }

    @Test
    void getOrders_whenMinTotalIsInvalid_returnsBadRequest() throws Exception {
        Long userId = 1L;

        authenticateUser(userId);

        LocalDateTime futureDate = LocalDateTime.now().minusDays(3);
        LocalDateTime pastDate = LocalDateTime.now().plusDays(3);

        mockMvc.perform(get(ORDER_URI)
                        .param("status", OrderStatus.PENDING_PAYMENT.name())
                        .param("minTotal", "abc")
                        .param("maxTotal", "100.0")
                        .param("createdAfter", String.valueOf(futureDate))
                        .param("createdBefore", String.valueOf(pastDate)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.uri").value(ORDER_URI))
                .andExpect(jsonPath("$.fieldErrors.minTotal",
                        containsInAnyOrder(invalidParameter("minTotal"))));

        verifyNoInteractions(orderService);
    }

    @Test
    void getOrders_whenMaxTotalIsInvalid_returnsBadRequest() throws Exception {
        Long userId = 1L;

        authenticateUser(userId);

        LocalDateTime futureDate = LocalDateTime.now().minusDays(3);
        LocalDateTime pastDate = LocalDateTime.now().plusDays(3);

        mockMvc.perform(get(ORDER_URI)
                        .param("status", OrderStatus.PENDING_PAYMENT.name())
                        .param("minTotal", "0.0")
                        .param("maxTotal", "abc")
                        .param("createdAfter", String.valueOf(futureDate))
                        .param("createdBefore", String.valueOf(pastDate)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.uri").value(ORDER_URI))
                .andExpect(jsonPath("$.fieldErrors.maxTotal",
                        containsInAnyOrder(invalidParameter("maxTotal"))));

        verifyNoInteractions(orderService);
    }

    @Test
    void getOrders_whenCreatedAfterIsInvalid_returnsBadRequest() throws Exception {
        Long userId = 1L;

        authenticateUser(userId);

        LocalDateTime pastDate = LocalDateTime.now().plusDays(3);

        mockMvc.perform(get(ORDER_URI)
                        .param("status", OrderStatus.PENDING_PAYMENT.name())
                        .param("minTotal", "0.0")
                        .param("maxTotal", "100.0")
                        .param("createdAfter", "abc")
                        .param("createdBefore", String.valueOf(pastDate)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.uri").value(ORDER_URI))
                .andExpect(jsonPath("$.fieldErrors.createdAfter",
                        containsInAnyOrder(invalidParameter("createdAfter"))));

        verifyNoInteractions(orderService);
    }

    @Test
    void getOrders_whenCreatedBeforeIsInvalid_returnsBadRequest() throws Exception {
        Long userId = 1L;

        authenticateUser(userId);

        LocalDateTime futureDate = LocalDateTime.now().minusDays(3);

        mockMvc.perform(get(ORDER_URI)
                        .param("status", OrderStatus.PENDING_PAYMENT.name())
                        .param("minTotal", "0.0")
                        .param("maxTotal", "100.0")
                        .param("createdAfter", String.valueOf(futureDate))
                        .param("createdBefore", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.uri").value(ORDER_URI))
                .andExpect(jsonPath("$.fieldErrors.createdBefore",
                        containsInAnyOrder(invalidParameter("createdBefore"))));

        verifyNoInteractions(orderService);
    }


    @Test
    void getOrders_whenOrdersPageIsEmpty_returnsPageOfOrderResponse() throws Exception {
        Long userId = 1L;
        List<OrderResponse> responses = List.of();

        Pageable pageable = PageRequest.of(0, 10);
        Page<OrderResponse> pageOrder = new PageImpl<>(responses, pageable, responses.size());

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

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(orderService).getOrders(eq(userId), any(OrderFilterRequest.class), captor.capture());

        Pageable capturePageable = captor.getValue();

        assertThat(capturePageable.getSort()).contains(Sort.Order.desc("id"));
        assertThat(capturePageable.getPageSize()).isEqualTo(10);
        assertThat(capturePageable.getPageNumber()).isEqualTo(0);

        verifyNoMoreInteractions(orderService);
    }

    @Test
    void getOrders_whenRequestHasFilters_returnsPageOfOrderResponse() throws Exception {
        Long userId = 1L;

        List<OrderResponse> responses = List.of();

        Pageable pageable = PageRequest.of(0, 10);
        Page<OrderResponse> pageOrder = new PageImpl<>(responses, pageable, responses.size());

        when(orderService.getOrders(eq(userId), any(OrderFilterRequest.class), any(Pageable.class))).thenReturn(pageOrder);

        authenticateUser(userId);

        LocalDateTime futureDate = LocalDateTime.now().minusDays(3);
        LocalDateTime pastDate = LocalDateTime.now().plusDays(3);

        mockMvc.perform(get(ORDER_URI)
                        .param("status", OrderStatus.PENDING_PAYMENT.name())
                        .param("minTotal", "0.0")
                        .param("maxTotal", "100.0")
                        .param("createdAfter", String.valueOf(futureDate))
                        .param("createdBefore", String.valueOf(pastDate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.numberOfElements").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        ArgumentCaptor<OrderFilterRequest> filterCaptor = ArgumentCaptor.forClass(OrderFilterRequest.class);

        verify(orderService).getOrders(eq(userId), filterCaptor.capture(), captor.capture());

        Pageable capturePageable = captor.getValue();

        assertThat(capturePageable.getSort()).contains(Sort.Order.desc("id"));
        assertThat(capturePageable.getPageSize()).isEqualTo(10);
        assertThat(capturePageable.getPageNumber()).isEqualTo(0);

        OrderFilterRequest requestFilter = filterCaptor.getValue();
        assertThat(requestFilter.status().name()).isEqualTo(OrderStatus.PENDING_PAYMENT.name());
        assertThat(requestFilter.minTotal()).isEqualTo("0.0");
        assertThat(requestFilter.maxTotal()).isEqualTo("100.0");
        assertThat(requestFilter.createdAfter()).isEqualTo(futureDate);
        assertThat(requestFilter.createdBefore()).isEqualTo(pastDate);

        verifyNoMoreInteractions(orderService);
    }

    @Test
    void getOrderById_whenRequestIsValid_returnsOrderResponse() throws Exception {
        Long userId = 1L;
        Long orderId = 2L;
        Long productId1 = 4L;
        Long productId2 = 5L;
        int quantity1 = 2;
        int quantity2 = 3;
        BigDecimal price1 = VALID_PRODUCT_PRICE;
        BigDecimal price2 = VALID_PRODUCT_PRICE.add(BigDecimal.TEN);
        BigDecimal total = price1.multiply(BigDecimal.valueOf(quantity1)).add(price2.multiply(BigDecimal.valueOf(quantity2)));

        OrderItemResponse itemResponse1 = new OrderItemResponse(
                productId1,
                VALID_PRODUCT_NAME,
                quantity1,
                price1
        );


        OrderItemResponse itemResponse2 = new OrderItemResponse(
                productId2,
                VALID_PRODUCT_NAME,
                quantity2,
                price2
        );

        OrderResponse orderResponse = new OrderResponse(
                orderId,
                userId,
                List.of(itemResponse1, itemResponse2),
                total,
                OrderStatus.PENDING_PAYMENT,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(orderService.getOrderById(orderId, userId)).thenReturn(orderResponse);

        authenticateUser(userId);

        mockMvc.perform(get(ORDER_URI + "/" + orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(orderId))
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[0].productId").value(productId1))
                .andExpect(jsonPath("$.items[0].productName").value(VALID_PRODUCT_NAME))
                .andExpect(jsonPath("$.items[0].quantity").value(quantity1))
                .andExpect(jsonPath("$.items[0].price").value(price1.doubleValue()))
                .andExpect(jsonPath("$.items[1].productId").value(productId2))
                .andExpect(jsonPath("$.items[1].productName").value(VALID_PRODUCT_NAME))
                .andExpect(jsonPath("$.items[1].quantity").value(quantity2))
                .andExpect(jsonPath("$.items[1].price").value(price2.doubleValue()))
                .andExpect(jsonPath("$.total").value(total))
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

        mockMvc.perform(get(ORDER_URI + "/" + orderId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(invalidParameter("orderId")))
                .andExpect(jsonPath("$.uri").value(ORDER_URI + "/" + orderId));


        verifyNoInteractions(orderService);
    }

    @Test
    void getOrderById_whenOrderNotFound_returnsNotFound() throws Exception {
        Long userId = 1L;
        Long orderId = 2L;

        when(orderService.getOrderById(orderId,userId))
                .thenThrow(new NoResourceFoundException(orderNotFound(orderId, userId)));

        authenticateUser(userId);

        mockMvc.perform(get(ORDER_URI + "/" + orderId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.NOT_FOUND.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(orderNotFound(orderId, userId)))
                .andExpect(jsonPath("$.uri").value(ORDER_URI + "/" + orderId));


        verify(orderService).getOrderById(orderId, userId);
        verifyNoMoreInteractions(orderService);
    }

    @Test
    void cancelOrder_whenRequestIsValid_returnsNoContent() throws Exception {
        Long userId = 1L;
        Long orderId = 2L;

        authenticateUser(userId);

        mockMvc.perform(patch(ORDER_URI + "/" + orderId + "/cancel"))
                .andExpect(status().isNoContent());


        verify(orderService).cancelOrder(orderId, userId);
        verifyNoMoreInteractions(orderService);
    }

    @Test
    void cancelOrder_whenOrderIdIsInvalid_returnsBadRequest() throws Exception {
        Long userId = 1L;
        String orderId = INVALID_ID;

        authenticateUser(userId);

        mockMvc.perform(patch(ORDER_URI + "/" + orderId + "/cancel"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(invalidParameter("orderId")))
                .andExpect(jsonPath("$.uri").value(ORDER_URI + "/" + orderId + "/cancel"));


        verifyNoInteractions(orderService);
    }

    @Test
    void cancelOrder_whenOrderNotFound_returnsNotFound() throws Exception {
        Long userId = 1L;
        Long orderId = 2L;

        doThrow(new NoResourceFoundException(orderNotFound(orderId, userId)))
                .when(orderService).cancelOrder(orderId, userId);

        authenticateUser(userId);

        mockMvc.perform(patch(ORDER_URI + "/" + orderId + "/cancel"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.NOT_FOUND.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(orderNotFound(orderId, userId)))
                .andExpect(jsonPath("$.uri").value(ORDER_URI + "/" + orderId + "/cancel"));


        verify(orderService).cancelOrder(orderId, userId);
        verifyNoMoreInteractions(orderService);
    }

    @Test
    void cancelById_whenOrderIsDelivered_returnsBadRequest() throws Exception {
        Long userId = 1L;
        Long orderId = 2L;

        doThrow(new InvalidOrderStateException(cannotCancelDeliveredOrder()))
                .when(orderService).cancelOrder(orderId, userId);

        authenticateUser(userId);

        mockMvc.perform(patch(ORDER_URI + "/" + orderId + "/cancel"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(cannotCancelDeliveredOrder()))
                .andExpect(jsonPath("$.uri").value(ORDER_URI + "/" + orderId + "/cancel"));


        verify(orderService).cancelOrder(orderId, userId);
        verifyNoMoreInteractions(orderService);
    }

    @Test
    void cancelById_whenOrderIsCancelled_returnsBadRequest() throws Exception {
        Long userId = 1L;
        Long orderId = 2L;

        doThrow(new InvalidOrderStateException(orderAlreadyCancelled()))
                .when(orderService).cancelOrder(orderId, userId);

        authenticateUser(userId);

        mockMvc.perform(patch(ORDER_URI + "/" + orderId + "/cancel"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(orderAlreadyCancelled()))
                .andExpect(jsonPath("$.uri").value(ORDER_URI + "/" + orderId + "/cancel"));


        verify(orderService).cancelOrder(orderId, userId);
        verifyNoMoreInteractions(orderService);
    }

    @Test
    void checkoutCart_whenRequestIsValid_returnsOrderResponse() throws Exception {
        Long userId = 1L;
        Long orderId = 2L;
        Long productId1 = 4L;
        Long productId2 = 5L;
        int quantity1 = 2;
        int quantity2 = 3;
        BigDecimal price1 = VALID_PRODUCT_PRICE;
        BigDecimal price2 = VALID_PRODUCT_PRICE.add(BigDecimal.TEN);
        BigDecimal total = price1.multiply(BigDecimal.valueOf(quantity1)).add(price2.multiply(BigDecimal.valueOf(quantity2)));

        OrderItemResponse itemResponse1 = new OrderItemResponse(
                productId1,
                VALID_PRODUCT_NAME,
                quantity1,
                price1
        );


        OrderItemResponse itemResponse2 = new OrderItemResponse(
                productId2,
                VALID_PRODUCT_NAME,
                quantity2,
                price2
        );

        OrderResponse orderResponse = new OrderResponse(
                orderId,
                userId,
                List.of(itemResponse1, itemResponse2),
                total,
                OrderStatus.PENDING_PAYMENT,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(orderService.checkoutCart(userId)).thenReturn(orderResponse);

        authenticateUser(userId);

        mockMvc.perform(post(ORDER_URI + "/checkout"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value(orderId))
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[0].productId").value(productId1))
                .andExpect(jsonPath("$.items[0].productName").value(VALID_PRODUCT_NAME))
                .andExpect(jsonPath("$.items[0].quantity").value(quantity1))
                .andExpect(jsonPath("$.items[0].price").value(price1.doubleValue()))
                .andExpect(jsonPath("$.items[1].productId").value(productId2))
                .andExpect(jsonPath("$.items[1].productName").value(VALID_PRODUCT_NAME))
                .andExpect(jsonPath("$.items[1].quantity").value(quantity2))
                .andExpect(jsonPath("$.items[1].price").value(price2.doubleValue()))
                .andExpect(jsonPath("$.total").value(total))
                .andExpect(jsonPath("$.status").value(OrderStatus.PENDING_PAYMENT.name()))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());

        verify(orderService).checkoutCart(userId);
        verifyNoMoreInteractions(orderService);
    }

    @Test
    void checkoutCart_whenCartIsEmpty_returnsBadRequest() throws Exception {
        Long userId = 1L;

        doThrow(new InvalidOrderStateException(emptyCart()))
                .when(orderService).checkoutCart(userId);

        authenticateUser(userId);

        mockMvc.perform(post(ORDER_URI + "/checkout"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(emptyCart()))
                .andExpect(jsonPath("$.uri").value(ORDER_URI + "/checkout"));


        verify(orderService).checkoutCart(userId);
        verifyNoMoreInteractions(orderService);
    }
}
