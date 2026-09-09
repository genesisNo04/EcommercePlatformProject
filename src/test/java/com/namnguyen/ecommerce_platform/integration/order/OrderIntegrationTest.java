package com.namnguyen.ecommerce_platform.integration.order;

import com.namnguyen.ecommerce_platform.cart.entity.Cart;
import com.namnguyen.ecommerce_platform.integration.BaseIntegrationTest;
import com.namnguyen.ecommerce_platform.order.dto.CreateOrderItemRequest;
import com.namnguyen.ecommerce_platform.order.dto.CreateOrderRequest;
import com.namnguyen.ecommerce_platform.order.dto.OrderResponse;
import com.namnguyen.ecommerce_platform.order.entity.Order;
import com.namnguyen.ecommerce_platform.order.enums.OrderStatus;
import com.namnguyen.ecommerce_platform.product.entity.Product;
import com.namnguyen.ecommerce_platform.product.enums.ProductStatus;
import com.namnguyen.ecommerce_platform.user.entity.User;
import com.namnguyen.ecommerce_platform.user.enums.Role;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.List;

import static com.namnguyen.ecommerce_platform.testutil.MockAuthentication.*;
import static com.namnguyen.ecommerce_platform.testutil.TestDataFactory.*;
import static com.namnguyen.ecommerce_platform.testutil.messages.CommonTestMessages.VALIDATION_FAILED;
import static com.namnguyen.ecommerce_platform.testutil.messages.CommonTestMessages.invalidParameter;
import static com.namnguyen.ecommerce_platform.testutil.messages.OrderTestMessages.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class OrderIntegrationTest extends BaseIntegrationTest {

    @Test
    void createOrder_withValidRequest_createOrderInDatabase() throws Exception {
        User user = persistDefaultCustomer();
        Product product = persistDefaultProduct();
        int originalQuantity = product.getQuantity();
        int boughtQuantity = 10;
        
        CreateOrderItemRequest itemRequest = createOrderItemRequest(product.getId(), boughtQuantity);
        BigDecimal subtotal = product.getPrice().multiply(BigDecimal.valueOf(boughtQuantity));

        CreateOrderRequest createOrderRequest = new CreateOrderRequest(
                List.of(itemRequest)
        );

        authenticateUser(user.getId());

        MvcResult result = mockMvc.perform(post(ORDER_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createOrderRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").exists())
                .andExpect(jsonPath("$.userId").value(user.getId()))
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].productId").value(product.getId()))
                .andExpect(jsonPath("$.items[0].productName").value(product.getName()))
                .andExpect(jsonPath("$.items[0].quantity").value(boughtQuantity))
                .andExpect(jsonPath("$.items[0].price").value(product.getPrice().doubleValue()))
                .andExpect(jsonPath("$.total").value(subtotal.doubleValue()))
                .andExpect(jsonPath("$.status").value(OrderStatus.PENDING_PAYMENT.name()))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists())
                .andReturn();

        OrderResponse orderResponse = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                OrderResponse.class
        );

        Order savedOrder = orderRepository.findById(orderResponse.orderId()).orElseThrow();

        assertThat(savedOrder.getUser().getId()).isEqualTo(user.getId());
        assertThat(savedOrder.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        assertThat(savedOrder.getTotal()).isEqualByComparingTo(subtotal);

        Product savedProduct = productRepository.findById(product.getId()).orElseThrow();

        assertThat(savedProduct.getQuantity()).isEqualTo(originalQuantity - boughtQuantity);
    }

    @Test
    void createOrder_whenItemsAreNull_returnsBadRequest() throws Exception {
        User user = persistDefaultCustomer();
        authenticateUser(user.getId());

        CreateOrderRequest request = new CreateOrderRequest(
                null
        );

        mockMvc.perform(post(ORDER_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                .andExpect(jsonPath("$.fieldErrors.items").value(ORDER_IS_EMPTY));

        assertThat(orderRepository.count()).isZero();
    }

    @Test
    void createOrder_whenItemQuantityIsNegative_returnsBadRequest() throws Exception {
        User user = persistDefaultCustomer();
        Product product = persistDefaultProduct();
        int boughtQuantity = -1;
        
        CreateOrderItemRequest itemRequest = createOrderItemRequest(product.getId(), boughtQuantity);

        authenticateUser(user.getId());

        CreateOrderRequest createOrderRequest = new CreateOrderRequest(
                List.of(itemRequest)
        );

        mockMvc.perform(post(ORDER_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createOrderRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                .andExpect(jsonPath("$.fieldErrors['items[0].quantity']").value(ORDER_ITEM_QUANTITY_IS_INVALID));

        assertThat(orderRepository.count()).isZero();
    }

    @Test
    void createOrder_whenItemsAreEmpty_returnsBadRequest() throws Exception {
        User user = persistDefaultCustomer();
        authenticateUser(user.getId());

        CreateOrderRequest request = new CreateOrderRequest(
                List.of()
        );

        mockMvc.perform(post(ORDER_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                .andExpect(jsonPath("$.fieldErrors.items").value(ORDER_IS_EMPTY));

        assertThat(orderRepository.count()).isZero();
    }

    @Test
    void createOrder_withoutRequestBody_returnsBadRequest() throws Exception {
        User user = persistDefaultCustomer();
        authenticateUser(user.getId());

        mockMvc.perform(post(ORDER_URI))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                .andExpect(jsonPath("$.fieldErrors.requestBody").value(invalidParameter("requestBody")));

        assertThat(orderRepository.count()).isZero();
    }

    @Test
    void createOrder_whenOneProductHasInsufficientStock_returnsBadRequest() throws Exception {
        Product firstProduct = persistDefaultProduct();

        Product secondProduct = persistProduct(
                "testproduct",
                "testing product",
                BigDecimal.valueOf(15.99),
                5,
                ProductStatus.ACTIVE
        );

        int originalFirstProductQuantity = firstProduct.getQuantity();
        int originalSecondProductQuantity = secondProduct.getQuantity();

        int boughtQuantity = 10;

        User user = persistDefaultCustomer();

        CreateOrderRequest request = new CreateOrderRequest(
                List.of(
                        createOrderItemRequest(
                                firstProduct.getId(),
                                boughtQuantity
                        ),
                        createOrderItemRequest(
                                secondProduct.getId(),
                                boughtQuantity
                        )
                )
        );

        authenticateUser(user.getId());

        mockMvc.perform(post(ORDER_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value(insufficientStock(secondProduct.getName())));

        assertThat(orderRepository.count()).isZero();

        Product firstSavedProduct = productRepository
                .findById(firstProduct.getId())
                .orElseThrow();

        Product secondSavedProduct = productRepository
                .findById(secondProduct.getId())
                .orElseThrow();

        assertThat(firstSavedProduct.getQuantity())
                .isEqualTo(originalFirstProductQuantity);

        assertThat(secondSavedProduct.getQuantity())
                .isEqualTo(originalSecondProductQuantity);
    }

    @Test
    void createOrder_whenProductNotFound_returnsNotFound() throws Exception {
        User user = persistDefaultCustomer();
        long nonExistingProductId = 999_999L;

        CreateOrderItemRequest orderItemRequest =
                createOrderItemRequest(nonExistingProductId, 10);

        CreateOrderRequest orderRequest = new CreateOrderRequest(
                List.of(orderItemRequest)
        );

        authenticateUser(user.getId());

        mockMvc.perform(post(ORDER_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isNotFound());

        assertThat(orderRepository.count()).isZero();
    }

    @Test
    void getOrders_whenOrdersExist_returnsOk() throws Exception {
        User user = persistDefaultCustomer();
        BigDecimal firstOrderTotal = BigDecimal.valueOf(299.99);
        BigDecimal secondOrderTotal = BigDecimal.valueOf(399.99);

        Order firstOrder = persistOrder(
                firstOrderTotal,
                OrderStatus.PENDING_PAYMENT,
                user,
                List.of(),
                null
        );

        Order secondOrder = persistOrder(
                secondOrderTotal,
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
                .andExpect(jsonPath("$.content[0].orderId").value(secondOrder.getId()))
                .andExpect(jsonPath("$.content[0].userId").value(user.getId()))
                .andExpect(jsonPath("$.content[0].total").value(secondOrder.getTotal().doubleValue()))
                .andExpect(jsonPath("$.content[0].status").value(secondOrder.getStatus().name()))
                .andExpect(jsonPath("$.content[1].orderId").value(firstOrder.getId()))
                .andExpect(jsonPath("$.content[1].userId").value(user.getId()))
                .andExpect(jsonPath("$.content[1].total").value(firstOrder.getTotal().doubleValue()))
                .andExpect(jsonPath("$.content[1].status").value(firstOrder.getStatus().name()));
    }

    @Test
    void getOrders_withFilter_returnsOk() throws Exception {
        User user = persistDefaultCustomer();
        BigDecimal firstOrderTotal = BigDecimal.valueOf(299.99);
        BigDecimal secondOrderTotal = BigDecimal.valueOf(399.99);

        Order firstOrder = persistOrder(
                firstOrderTotal,
                OrderStatus.PENDING_PAYMENT,
                user,
                List.of(),
                null
        );

        persistOrder(
                secondOrderTotal,
                OrderStatus.PAID,
                user,
                List.of(),
                null
        );

        authenticateUser(user.getId());

        mockMvc.perform(get(ORDER_URI)
                        .param("status", OrderStatus.PENDING_PAYMENT.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].orderId").value(firstOrder.getId()))
                .andExpect(jsonPath("$.content[0].userId").value(user.getId()))
                .andExpect(jsonPath("$.content[0].total").value(firstOrder.getTotal().doubleValue()))
                .andExpect(jsonPath("$.content[0].status").value(firstOrder.getStatus().name()));
    }

    @Test
    void getOrders_withCombineFilter_returnsOk() throws Exception {
        User user = persistDefaultCustomer();

        BigDecimal firstOrderTotal = BigDecimal.valueOf(299.99);
        BigDecimal secondOrderTotal = BigDecimal.valueOf(399.99);
        BigDecimal thirdOrderTotal = BigDecimal.valueOf(499.99);

        Order firstOrder = persistOrder(
                firstOrderTotal,
                OrderStatus.PENDING_PAYMENT,
                user,
                List.of(),
                null
        );

        Order secondOrder = persistOrder(
                secondOrderTotal,
                OrderStatus.PAID,
                user,
                List.of(),
                null
        );

        persistOrder(
                thirdOrderTotal,
                OrderStatus.PAID,
                user,
                List.of(),
                null
        );

        authenticateUser(user.getId());

        mockMvc.perform(get(ORDER_URI)
                        .param("minTotal", "200.00")
                        .param("maxTotal", "400.00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].orderId").value(secondOrder.getId()))
                .andExpect(jsonPath("$.content[0].userId").value(user.getId()))
                .andExpect(jsonPath("$.content[0].total").value(secondOrder.getTotal().doubleValue()))
                .andExpect(jsonPath("$.content[0].status").value(secondOrder.getStatus().name()))
                .andExpect(jsonPath("$.content[1].orderId").value(firstOrder.getId()))
                .andExpect(jsonPath("$.content[1].userId").value(user.getId()))
                .andExpect(jsonPath("$.content[1].total").value(firstOrder.getTotal().doubleValue()))
                .andExpect(jsonPath("$.content[1].status").value(firstOrder.getStatus().name()));
    }

    @Test
    void getOrders_onlyReturnsOrderOfLoginUser_returnsOk() throws Exception {
        User user = persistDefaultCustomer();
        User otherUser = persistUser(
                "otheruser@gmail.com",
                VALID_PASSWORD,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                "1234567892",
                Role.CUSTOMER
        );

        BigDecimal firstOrderTotal = BigDecimal.valueOf(299.99);
        BigDecimal secondOrderTotal = BigDecimal.valueOf(399.99);

        Order firstOrder = persistOrder(
                firstOrderTotal,
                OrderStatus.PENDING_PAYMENT,
                user,
                List.of(),
                null
        );

        Order secondOrder = persistOrder(
                secondOrderTotal,
                OrderStatus.PAID,
                user,
                List.of(),
                null
        );

        persistOrder(
                secondOrderTotal,
                OrderStatus.PAID,
                otherUser,
                List.of(),
                null
        );

        authenticateUser(user.getId());

        mockMvc.perform(get(ORDER_URI))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].orderId").value(secondOrder.getId()))
                .andExpect(jsonPath("$.content[0].userId").value(user.getId()))
                .andExpect(jsonPath("$.content[0].total").value(secondOrder.getTotal().doubleValue()))
                .andExpect(jsonPath("$.content[0].status").value(secondOrder.getStatus().name()))
                .andExpect(jsonPath("$.content[1].orderId").value(firstOrder.getId()))
                .andExpect(jsonPath("$.content[1].userId").value(user.getId()))
                .andExpect(jsonPath("$.content[1].total").value(firstOrder.getTotal().doubleValue()))
                .andExpect(jsonPath("$.content[1].status").value(firstOrder.getStatus().name()));
    }

    @Test
    void getOrders_whenUserHasNoOrders_returnsOk() throws Exception {
        User user = persistDefaultCustomer();

        authenticateUser(user.getId());

        mockMvc.perform(get(ORDER_URI))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test
    void getOrderById_whenOrderExists_returnsOk() throws Exception {
        User user = persistDefaultCustomer();
        BigDecimal total = BigDecimal.valueOf(299.99);

        Order order = persistOrder(
                total,
                OrderStatus.PENDING_PAYMENT,
                user,
                List.of(),
                null
        );

        authenticateUser(user.getId());

        mockMvc.perform(get(orderUri(order.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(order.getId()))
                .andExpect(jsonPath("$.userId").value(user.getId()))
                .andExpect(jsonPath("$.total").value(order.getTotal().doubleValue()))
                .andExpect(jsonPath("$.status").value(order.getStatus().name()));
    }

    @Test
    void getOrderById_whenOrdersNotFound_returnsNotFound() throws Exception {
        User user = persistDefaultCustomer();
        authenticateUser(user.getId());
        long nonExistingOrderId = 999_999L;

        mockMvc.perform(get(orderUri(nonExistingOrderId)))
                .andExpect(status().isNotFound());
    }

    @Test
    void getOrderById_whenOrderBelongsToDifferentUser_returnsNotFound()
            throws Exception {

        User owner = persistDefaultCustomer();
        User otherUser = persistUser(
                "otheruser@gmail.com",
                VALID_PASSWORD,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                "1234567892",
                Role.CUSTOMER
        );

        Order order = persistOrder(
                BigDecimal.valueOf(299.99),
                OrderStatus.PENDING_PAYMENT,
                owner,
                List.of(),
                null
        );

        authenticateUser(otherUser.getId());

        mockMvc.perform(get(orderUri(order.getId())))
                .andExpect(status().isNotFound());
    }

    @Test
    void cancelOrder_whenValid_restoresStockAndCancelsOrder()
            throws Exception {

        User user = persistDefaultCustomer();
        Product product = persistDefaultProduct();

        int originalStock = product.getQuantity();

        authenticateUser(user.getId());

        CreateOrderRequest request =
                new CreateOrderRequest(
                        List.of(
                                createOrderItemRequest(
                                        product.getId(),
                                        10
                                )
                        )
                );

        MvcResult result = mockMvc.perform(
                        post(ORDER_URI)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isCreated())
                .andReturn();

        OrderResponse orderResponse = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                OrderResponse.class
        );

        mockMvc.perform(patch(cancelOrderUri(orderResponse.orderId())))
                .andExpect(status().isNoContent());

        Order order = orderRepository
                .findById(orderResponse.orderId())
                .orElseThrow();

        Product updatedProduct = productRepository
                .findById(product.getId())
                .orElseThrow();

        assertThat(order.getStatus())
                .isEqualTo(OrderStatus.CANCELLED);

        assertThat(updatedProduct.getQuantity())
                .isEqualTo(originalStock);
    }

    @Test
    void cancelOrder_whenOrderNotFound_returnsNotFound() throws Exception {
        User user = persistDefaultCustomer();
        long nonExistingOrderId = 999_999L;

        authenticateUser(user.getId());

        mockMvc.perform(patch(cancelOrderUri(nonExistingOrderId)))
                .andExpect(status().isNotFound());
    }

    @Test
    void cancelOrder_whenOrderIsDelivered_returnsBadRequest() throws Exception {
        User user = persistDefaultCustomer();
        BigDecimal total = BigDecimal.valueOf(299.99);

        Order order = persistOrder(
                total,
                OrderStatus.DELIVERED,
                user,
                List.of(),
                null
        );

        authenticateUser(user.getId());

        mockMvc.perform(patch(cancelOrderUri(order.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(DELIVERED_ORDER_CANNOT_BE_CANCELLED));

        Order savedOrder = orderRepository.findById(order.getId())
                .orElseThrow();

        assertThat(savedOrder.getStatus())
                .isEqualTo(OrderStatus.DELIVERED);
    }

    @Test
    void cancelOrder_whenOrderAlreadyCancelled_returnsBadRequest() throws Exception {
        User user = persistDefaultCustomer();
        BigDecimal total = BigDecimal.valueOf(299.99);

        Order order = persistOrder(
                total,
                OrderStatus.CANCELLED,
                user,
                List.of(),
                null
        );

        authenticateUser(user.getId());

        mockMvc.perform(patch(cancelOrderUri(order.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(ORDER_ALREADY_CANCELLED));

        Order savedOrder = orderRepository.findById(order.getId())
                .orElseThrow();

        assertThat(savedOrder.getStatus())
                .isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void cancelOrder_whenOrderIsPaid_returnsBadRequest() throws Exception {
        User user = persistDefaultCustomer();
        BigDecimal total = BigDecimal.valueOf(299.99);

        Order order = persistOrder(
                total,
                OrderStatus.PAID,
                user,
                List.of(),
                null
        );

        authenticateUser(user.getId());

        mockMvc.perform(patch(cancelOrderUri(order.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(ORDER_CANNOT_BE_CANCELLED));

        Order savedOrder = orderRepository.findById(order.getId())
                .orElseThrow();

        assertThat(savedOrder.getStatus())
                .isEqualTo(OrderStatus.PAID);
    }

    @Test
    void cancelOrder_whenOrderIsShipped_returnsBadRequest() throws Exception {
        User user = persistDefaultCustomer();
        BigDecimal total = BigDecimal.valueOf(299.99);

        Order order = persistOrder(
                total,
                OrderStatus.SHIPPED,
                user,
                List.of(),
                null
        );

        authenticateUser(user.getId());

        mockMvc.perform(patch(cancelOrderUri(order.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(ORDER_CANNOT_BE_CANCELLED));

        Order savedOrder = orderRepository.findById(order.getId())
                .orElseThrow();

        assertThat(savedOrder.getStatus())
                .isEqualTo(OrderStatus.SHIPPED);
    }

    @Test
    void cancelOrder_whenOrderIsProcessing_returnsBadRequest() throws Exception {
        User user = persistDefaultCustomer();
        BigDecimal total = BigDecimal.valueOf(299.99);

        Order order = persistOrder(
                total,
                OrderStatus.PROCESSING,
                user,
                List.of(),
                null
        );

        authenticateUser(user.getId());

        mockMvc.perform(patch(cancelOrderUri(order.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(ORDER_CANNOT_BE_CANCELLED));

        Order savedOrder = orderRepository.findById(order.getId())
                .orElseThrow();

        assertThat(savedOrder.getStatus())
                .isEqualTo(OrderStatus.PROCESSING);
    }

    @Test
    void cancelOrder_whenOrderBelongsToDifferentUser_returnsNotFound()
            throws Exception {

        User owner = persistDefaultCustomer();

        User otherUser = persistUser(
                "otheruser@gmail.com",
                VALID_PASSWORD,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                "1234567892",
                Role.CUSTOMER
        );

        Order order = persistOrder(
                BigDecimal.valueOf(299.99),
                OrderStatus.PENDING_PAYMENT,
                owner,
                List.of(),
                null
        );

        authenticateUser(otherUser.getId());

        mockMvc.perform(patch(cancelOrderUri(order.getId())))
                .andExpect(status().isNotFound());

        Order savedOrder = orderRepository.findById(order.getId())
                .orElseThrow();

        assertThat(savedOrder.getStatus())
                .isEqualTo(OrderStatus.PENDING_PAYMENT);
    }

    @Test
    void checkoutCart_whenCartHasItem_returnsOrderResponse() throws Exception {
        User user = persistDefaultCustomer();
        int quantity = 2;

        Product product = persistDefaultProduct();
        int originalProductQuantity = product.getQuantity();

        Cart cart = persistCart(user);
        persistCartItem(cart, product, quantity);

        BigDecimal expectedSubtotal = product.getPrice().multiply(BigDecimal.valueOf(quantity));

        authenticateUser(user.getId());

        MvcResult result = mockMvc.perform(post(CHECKOUT_URI))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").exists())
                .andExpect(jsonPath("$.userId").value(user.getId()))
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].productId").value(product.getId()))
                .andExpect(jsonPath("$.items[0].productName").value(product.getName()))
                .andExpect(jsonPath("$.items[0].quantity").value(quantity))
                .andExpect(jsonPath("$.items[0].price").value(product.getPrice().doubleValue()))
                .andExpect(jsonPath("$.total").value(expectedSubtotal.doubleValue()))
                .andExpect(jsonPath("$.status").value(OrderStatus.PENDING_PAYMENT.name()))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists())
                .andReturn();

        OrderResponse orderResponse = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                OrderResponse.class
        );

        Order savedOrder = orderRepository.findById(orderResponse.orderId()).orElseThrow();

        assertThat(savedOrder.getUser().getId()).isEqualTo(user.getId());
        assertThat(savedOrder.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        assertThat(savedOrder.getTotal()).isEqualByComparingTo(expectedSubtotal);

        mockMvc.perform(get(CART_URI))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)));

        Product savedProduct = productRepository.findById(product.getId())
                .orElseThrow();

        assertThat(savedProduct.getQuantity())
                .isEqualTo(originalProductQuantity - quantity);

        assertThat(cartItemRepository.findAll()).isEmpty();
    }

    @Test
    void checkoutCart_whenCheckoutFailed_returnsBadRequest() throws Exception {
        User user = persistDefaultCustomer();

        int quantity = 2;
        int exceedQuantity = 10;

        Product firstProduct = persistDefaultProduct();

        Product secondProduct = persistProduct(
                "testproduct",
                "testing product",
                BigDecimal.valueOf(15.99),
                5,
                ProductStatus.ACTIVE
        );

        int firstOriginalProductQuantity = firstProduct.getQuantity();
        int secondOriginalProductQuantity = secondProduct.getQuantity();

        Cart cart = persistCart(user);

        persistCartItem(cart, firstProduct, quantity);
        persistCartItem(cart, secondProduct, exceedQuantity);

        authenticateUser(user.getId());

        mockMvc.perform(post(CHECKOUT_URI))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value(insufficientStock(secondProduct.getName())));

        assertThat(orderRepository.count()).isZero();

        mockMvc.perform(get(CART_URI))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(2)));

        Product firstSavedProduct = productRepository.findById(firstProduct.getId())
                .orElseThrow();

        Product secondSavedProduct = productRepository.findById(secondProduct.getId())
                .orElseThrow();

        assertThat(firstSavedProduct.getQuantity())
                .isEqualTo(firstOriginalProductQuantity);

        assertThat(secondSavedProduct.getQuantity())
                .isEqualTo(secondOriginalProductQuantity);

        assertThat(cartItemRepository.findAll()).hasSize(2);
    }

    @Test
    void checkoutCart_whenCartIsEmpty_returnsBadRequest() throws Exception {
        User user = persistDefaultCustomer();
        persistCart(user);

        authenticateUser(user.getId());

        mockMvc.perform(post(CHECKOUT_URI))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(EMPTY_CART));

        assertThat(orderRepository.count()).isZero();
    }
}
