package com.namnguyen.ecommerce_platform.order.service;

import com.namnguyen.ecommerce_platform.cart.entity.Cart;
import com.namnguyen.ecommerce_platform.cart.entity.CartItem;
import com.namnguyen.ecommerce_platform.cart.exception.InvalidCartStateException;
import com.namnguyen.ecommerce_platform.cart.service.CartLookupService;
import com.namnguyen.ecommerce_platform.product.exception.InsufficientStockException;
import com.namnguyen.ecommerce_platform.order.exception.InvalidOrderException;
import com.namnguyen.ecommerce_platform.order.exception.InvalidOrderStateException;
import com.namnguyen.ecommerce_platform.common.exception.NoResourceFoundException;
import com.namnguyen.ecommerce_platform.order.dto.*;
import com.namnguyen.ecommerce_platform.order.entity.Order;
import com.namnguyen.ecommerce_platform.order.entity.OrderItem;
import com.namnguyen.ecommerce_platform.order.enums.OrderStatus;
import com.namnguyen.ecommerce_platform.order.repository.OrderRepository;
import com.namnguyen.ecommerce_platform.product.entity.Product;
import com.namnguyen.ecommerce_platform.product.enums.ProductStatus;
import com.namnguyen.ecommerce_platform.product.service.ProductLookupService;
import com.namnguyen.ecommerce_platform.user.entity.User;
import com.namnguyen.ecommerce_platform.user.service.UserLookupService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static com.namnguyen.ecommerce_platform.testutil.messages.OrderTestMessages.*;
import static com.namnguyen.ecommerce_platform.testutil.TestDataFactory.*;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CartLookupService cartLookupService;

    @Mock
    private UserLookupService userLookupService;

    @Mock
    private ProductLookupService productLookupService;

    @InjectMocks
    private OrderServiceImpl orderService;

    @Test
    void createOrder_whenRequestHasValidItems_createsPendingPaymentOrder() {
        Long userId = 1L;
        Long orderId = 2L;
        Long firstProductId = 3L;
        Long secondProductId = 4L;
        int firstStockQuantity = 10;
        int secondStockQuantity = 5;
        int firstOrderQuantity = 2;
        int secondOrderQuantity = 3;

        Product firstProduct = createProduct(
                firstProductId,
                VALID_PRODUCT_NAME,
                VALID_PRODUCT_PRICE,
                firstStockQuantity
        );

        Product secondProduct = createProduct(
                secondProductId,
                "XBOX",
                BigDecimal.valueOf(450.99),
                secondStockQuantity
        );

        BigDecimal total = firstProduct.getPrice().multiply(BigDecimal.valueOf(firstOrderQuantity))
                .add(secondProduct.getPrice().multiply(BigDecimal.valueOf(secondOrderQuantity)));

        CreateOrderItemRequest firstItemRequest = new CreateOrderItemRequest(
                firstProductId,
                firstOrderQuantity
        );

        CreateOrderItemRequest secondItemRequest = new CreateOrderItemRequest(
                secondProductId,
                secondOrderQuantity
        );

        CreateOrderRequest orderRequest = new CreateOrderRequest(
                List.of(firstItemRequest, secondItemRequest)
        );

        User user = createUser(userId);

        when(userLookupService.getUserById(userId)).thenReturn(user);
        when(productLookupService.getProductById(firstProductId)).thenReturn(firstProduct);
        when(productLookupService.getProductById(secondProductId)).thenReturn(secondProduct);
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
           Order order = inv.getArgument(0);
           order.setId(orderId);
           return order;
        });

        OrderResponse orderResponse = orderService.createOrder(orderRequest, userId);

        assertThat(orderResponse).isNotNull();
        assertThat(orderResponse.orderId()).isEqualTo(orderId);
        assertThat(orderResponse.userId()).isEqualTo(userId);
        assertThat(orderResponse.total()).isEqualByComparingTo(total);
        assertThat(orderResponse.items()).hasSize(2);
        assertThat(orderResponse.status()).isEqualTo(OrderStatus.PENDING_PAYMENT);

        assertThat(firstProduct.getQuantity()).isEqualTo(firstStockQuantity - firstOrderQuantity);
        assertThat(secondProduct.getQuantity()).isEqualTo(secondStockQuantity - secondOrderQuantity);
        assertThat(firstProduct.getStatus()).isEqualTo(ProductStatus.ACTIVE);
        assertThat(secondProduct.getStatus()).isEqualTo(ProductStatus.ACTIVE);

        OrderItemResponse firstItemResponse = orderResponse.items().getFirst();
        assertThat(firstItemResponse.productId()).isEqualTo(firstProductId);
        assertThat(firstItemResponse.productName()).isEqualTo(firstProduct.getName());
        assertThat(firstItemResponse.quantity()).isEqualTo(firstItemRequest.quantity());
        assertThat(firstItemResponse.price()).isEqualByComparingTo(firstProduct.getPrice());

        OrderItemResponse secondItemResponse = orderResponse.items().get(1);
        assertThat(secondItemResponse.productId()).isEqualTo(secondProductId);
        assertThat(secondItemResponse.productName()).isEqualTo(secondProduct.getName());
        assertThat(secondItemResponse.quantity()).isEqualTo(secondItemRequest.quantity());
        assertThat(secondItemResponse.price()).isEqualByComparingTo(secondProduct.getPrice());

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());

        Order savedOrder = orderCaptor.getValue();
        assertThat(savedOrder.getId()).isEqualTo(orderId);
        assertThat(savedOrder.getUser()).isEqualTo(user);
        assertThat(savedOrder.getUser().getId()).isEqualTo(userId);
        assertThat(savedOrder.getTotal()).isEqualByComparingTo(total);
        assertThat(savedOrder.getOrderItems()).hasSize(2);
        assertThat(savedOrder.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);

        OrderItem firstSavedOrderItem = savedOrder.getOrderItems().getFirst();
        assertThat(firstSavedOrderItem.getOrder()).isEqualTo(savedOrder);
        assertThat(firstSavedOrderItem.getProduct()).isEqualTo(firstProduct);
        assertThat(firstSavedOrderItem.getQuantity()).isEqualTo(firstOrderQuantity);
        assertThat(firstSavedOrderItem.getPrice()).isEqualByComparingTo(firstProduct.getPrice());

        OrderItem secondSavedOrderItem = savedOrder.getOrderItems().getLast();
        assertThat(secondSavedOrderItem.getOrder()).isEqualTo(savedOrder);
        assertThat(secondSavedOrderItem.getProduct()).isEqualTo(secondProduct);
        assertThat(secondSavedOrderItem.getQuantity()).isEqualTo(secondOrderQuantity);
        assertThat(secondSavedOrderItem.getPrice()).isEqualByComparingTo(secondProduct.getPrice());

        verify(userLookupService).getUserById(userId);
        verify(productLookupService).getProductById(firstProductId);
        verify(productLookupService).getProductById(secondProductId);
        verifyNoMoreInteractions(userLookupService);
        verifyNoMoreInteractions(productLookupService);
        verifyNoMoreInteractions(orderRepository);
    }

    @Test
    void createOrder_whenProductQuantityBecomesZero_marksProductOutOfStock() {
        Long userId = 1L;
        Long productId = 3L;
        Long orderId = 4L;
        int quantity = 10;

        User user = createUser(userId);

        Product product = createProduct(
                productId,
                VALID_PRODUCT_NAME,
                VALID_PRODUCT_PRICE,
                10
        );

        CreateOrderItemRequest orderItemRequest = new CreateOrderItemRequest(
                productId,
                quantity
        );

        CreateOrderRequest orderRequest = new CreateOrderRequest(
                List.of(orderItemRequest)
        );

        when(userLookupService.getUserById(userId)).thenReturn(user);
        when(productLookupService.getProductById(productId)).thenReturn(product);
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order order = inv.getArgument(0);
            order.setId(orderId);
            return order;
        });

        OrderResponse orderResponse = orderService.createOrder(orderRequest, userId);

        assertThat(orderResponse).isNotNull();
        assertThat(orderResponse.items()).hasSize(1);
        assertThat(product.getQuantity()).isEqualTo(0);
        assertThat(product.getStatus()).isEqualTo(ProductStatus.OUT_OF_STOCK);

        verify(userLookupService).getUserById(userId);
        verify(productLookupService).getProductById(productId);
        verify(orderRepository).save(any(Order.class));
        verifyNoMoreInteractions(userLookupService);
        verifyNoMoreInteractions(productLookupService);
        verifyNoMoreInteractions(orderRepository);
    }

    @Test
    void createOrder_whenUserNotFound_throwNoResourceFoundException() {
        Long userId = 999L;
        Long productId = 1L;
        int quantity = 2;

        CreateOrderItemRequest orderItemRequest = new CreateOrderItemRequest(
                productId,
                quantity
        );

        CreateOrderRequest orderRequest = new CreateOrderRequest(
                List.of(orderItemRequest)
        );

        when(userLookupService.getUserById(userId))
                .thenThrow(new NoResourceFoundException(userNotFoundWithId(userId)));

        NoResourceFoundException ex = assertThrows(
                NoResourceFoundException.class,
                () -> orderService.createOrder(orderRequest, userId)
        );

        assertThat(ex).isNotNull();
        assertThat(ex.getMessage()).isEqualTo(userNotFoundWithId(userId));

        verify(userLookupService).getUserById(userId);
        verifyNoMoreInteractions(userLookupService);
        verifyNoInteractions(productLookupService);
        verifyNoInteractions(orderRepository);
    }

    @Test
    void createOrder_whenProductNotFound_throwNoResourceFoundException() {
        Long userId = 1L;
        Long productId = 2L;
        int quantity = 2;

        User user = createUser(userId);

        CreateOrderItemRequest orderItemRequest = new CreateOrderItemRequest(
                productId,
                quantity
        );

        CreateOrderRequest orderRequest = new CreateOrderRequest(
                List.of(orderItemRequest)
        );

        when(userLookupService.getUserById(userId)).thenReturn(user);
        when(productLookupService.getProductById(productId))
                .thenThrow(new NoResourceFoundException(productNotFoundWithId(productId)));

        NoResourceFoundException ex = assertThrows(
                NoResourceFoundException.class,
                () -> orderService.createOrder(orderRequest, userId)
        );

        assertThat(ex).isNotNull();
        assertThat(ex.getMessage()).isEqualTo(productNotFoundWithId(productId));

        verify(userLookupService).getUserById(userId);
        verify(productLookupService).getProductById(productId);
        verifyNoMoreInteractions(userLookupService);
        verifyNoMoreInteractions(productLookupService);
        verifyNoInteractions(orderRepository);
    }

    @Test
    void createOrder_whenItemsIsEmpty_throwsInvalidOrderException() {
        Long userId = 999L;
        CreateOrderRequest orderRequest = new CreateOrderRequest(
                List.of()
        );

        InvalidOrderException ex = assertThrows(
                InvalidOrderException.class,
                () -> orderService.createOrder(orderRequest, userId)
        );

        assertThat(ex).isNotNull();
        assertThat(ex.getMessage()).isEqualTo(ORDER_IS_EMPTY);

        verifyNoInteractions(userLookupService);
        verifyNoInteractions(productLookupService);
        verifyNoInteractions(orderRepository);
    }

    @Test
    void createOrder_whenItemsIsNull_throwsInvalidOrderException() {
        Long userId = 999L;
        CreateOrderRequest orderRequest = new CreateOrderRequest(
                null
        );

        InvalidOrderException ex = assertThrows(
                InvalidOrderException.class,
                () -> orderService.createOrder(orderRequest, userId)
        );

        assertThat(ex).isNotNull();
        assertThat(ex.getMessage()).isEqualTo(ORDER_IS_EMPTY);

        verifyNoInteractions(userLookupService);
        verifyNoInteractions(productLookupService);
        verifyNoInteractions(orderRepository);
    }

    @Test
    void createOrder_whenRequestIsNull_throwsInvalidOrderException() {
        Long userId = 999L;

        InvalidOrderException ex = assertThrows(
                InvalidOrderException.class,
                () -> orderService.createOrder(null, userId)
        );

        assertThat(ex).isNotNull();
        assertThat(ex.getMessage()).isEqualTo(ORDER_IS_EMPTY);

        verifyNoInteractions(userLookupService);
        verifyNoInteractions(productLookupService);
        verifyNoInteractions(orderRepository);
    }

    @Test
    void createOrder_whenItemQuantityIsNegative_throwsInvalidOrderException() {
        Long userId = 1L;
        Long productId = 3L;
        int quantity = -1;

        User user = createUser(userId);

        CreateOrderItemRequest orderItemRequest = new CreateOrderItemRequest(
                productId,
                quantity
        );

        CreateOrderRequest orderRequest = new CreateOrderRequest(
                List.of(orderItemRequest)
        );

        when(userLookupService.getUserById(userId)).thenReturn(user);

        InvalidOrderException ex = assertThrows(
                InvalidOrderException.class,
                () -> orderService.createOrder(orderRequest, userId)
        );

        assertThat(ex).isNotNull();
        assertThat(ex.getMessage()).isEqualTo(ORDER_ITEM_QUANTITY_IS_INVALID);

        verify(userLookupService).getUserById(userId);
        verifyNoInteractions(productLookupService);
        verifyNoInteractions(orderRepository);
    }

    @Test
    void createOrder_whenItemQuantityIsZero_throwsInvalidOrderException() {
        Long userId = 1L;
        Long productId = 3L;
        int quantity = 0;

        User user = createUser(userId);

        CreateOrderItemRequest orderItemRequest = new CreateOrderItemRequest(
                productId,
                quantity
        );

        CreateOrderRequest orderRequest = new CreateOrderRequest(
                List.of(orderItemRequest)
        );

        when(userLookupService.getUserById(userId)).thenReturn(user);

        InvalidOrderException ex = assertThrows(
                InvalidOrderException.class,
                () -> orderService.createOrder(orderRequest, userId)
        );

        assertThat(ex).isNotNull();
        assertThat(ex.getMessage()).isEqualTo(ORDER_ITEM_QUANTITY_IS_INVALID);

        verify(userLookupService).getUserById(userId);
        verifyNoInteractions(productLookupService);
        verifyNoInteractions(orderRepository);
    }

    @Test
    void createOrder_whenOrderItemQuantityExceedStock_throwInsufficientStockException() {
        Long userId = 1L;
        Long productId = 3L;
        int requestQuantity = 11;
        int quantity = 10;

        User user = createUser(userId);

        Product product = createProduct(
                productId,
                VALID_PRODUCT_NAME,
                VALID_PRODUCT_PRICE,
                quantity
        );

        CreateOrderItemRequest orderItemRequest = new CreateOrderItemRequest(
                productId,
                requestQuantity
        );

        CreateOrderRequest orderRequest = new CreateOrderRequest(
                List.of(orderItemRequest)
        );

        when(userLookupService.getUserById(userId)).thenReturn(user);
        when(productLookupService.getProductById(productId)).thenReturn(product);

        InsufficientStockException ex = assertThrows(
                InsufficientStockException.class,
                () -> orderService.createOrder(orderRequest, userId)
        );

        assertThat(ex).isNotNull();
        assertThat(ex.getMessage()).isEqualTo(insufficientStock(product.getName()));
        assertThat(product.getQuantity()).isEqualTo(quantity);
        assertThat(product.getStatus()).isEqualTo(ProductStatus.ACTIVE);

        verify(userLookupService).getUserById(userId);
        verify(productLookupService).getProductById(productId);
        verifyNoMoreInteractions(userLookupService);
        verifyNoMoreInteractions(productLookupService);
        verifyNoInteractions(orderRepository);
    }

    @Test
    void getOrderById_whenOrderExists_returnOrderResponse() {
        Long userId = 1L;
        Long orderId = 2L;
        Long firstProductId = 3L;
        Long secondProductId = 4L;
        Long firstOrderItemId = 5L;
        Long secondOrderItemId = 6L;
        int firstProductStockQuantity = 10;
        int secondProductStockQuantity = 5;
        int firstOrderItemQuantity = 2;
        int secondOrderItemQuantity = 3;

        Product firstProduct = createProduct(
                firstProductId,
                VALID_PRODUCT_NAME,
                VALID_PRODUCT_PRICE,
                firstProductStockQuantity
        );

        Product secondProduct = createProduct(
                secondProductId,
                "XBOX",
                BigDecimal.valueOf(450.99),
                secondProductStockQuantity
        );

        User user = createUser(userId);
        BigDecimal total = firstProduct.getPrice().multiply(BigDecimal.valueOf(firstOrderItemQuantity))
                .add(secondProduct.getPrice().multiply(BigDecimal.valueOf(secondOrderItemQuantity)));

        Order order = createOrder(
                orderId,
                total,
                OrderStatus.PENDING_PAYMENT,
                user
        );

        OrderItem firstOrderItem = createOrderItem(
                firstOrderItemId,
                order,
                firstProduct,
                firstOrderItemQuantity,
                firstProduct.getPrice()
        );

        OrderItem secondOrderItem = createOrderItem(
                secondOrderItemId,
                order,
                secondProduct,
                secondOrderItemQuantity,
                secondProduct.getPrice()
        );

        order.addOrderItem(firstOrderItem);
        order.addOrderItem(secondOrderItem);

        when(orderRepository.findByIdAndUserId(orderId, userId)).thenReturn(Optional.of(order));

        OrderResponse orderResponse = orderService.getOrderById(orderId, userId);

        assertThat(orderResponse).isNotNull();
        assertThat(orderResponse.orderId()).isEqualTo(orderId);
        assertThat(orderResponse.userId()).isEqualTo(userId);
        assertThat(orderResponse.total()).isEqualByComparingTo(total);
        assertThat(orderResponse.status()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        assertThat(orderResponse.items()).hasSize(2);

        OrderItemResponse firstItemResponse = orderResponse.items().getFirst();
        assertThat(firstItemResponse.productId()).isEqualTo(firstProductId);
        assertThat(firstItemResponse.productName()).isEqualTo(firstProduct.getName());
        assertThat(firstItemResponse.quantity()).isEqualTo(firstOrderItemQuantity);
        assertThat(firstItemResponse.price()).isEqualByComparingTo(firstProduct.getPrice());

        OrderItemResponse secondItemResponse = orderResponse.items().getLast();
        assertThat(secondItemResponse.productId()).isEqualTo(secondProductId);
        assertThat(secondItemResponse.productName()).isEqualTo(secondProduct.getName());
        assertThat(secondItemResponse.quantity()).isEqualTo(secondOrderItemQuantity);
        assertThat(secondItemResponse.price()).isEqualByComparingTo(secondProduct.getPrice());

        verify(orderRepository).findByIdAndUserId(orderId, userId);
        verifyNoMoreInteractions(orderRepository);
    }

    @Test
    void getOrderById_whenOrderNotFoundForUser_throwsNoResourceFoundException() {
        Long userId = 1L;
        Long orderId = 2L;

        when(orderRepository.findByIdAndUserId(orderId, userId)).thenReturn(Optional.empty());

        NoResourceFoundException ex = assertThrows(
                NoResourceFoundException.class,
                () -> orderService.getOrderById(orderId, userId)
        );

        assertThat(ex).isNotNull();
        assertThat(ex.getMessage()).isEqualTo(orderNotFoundWithIdAndUserId(orderId, userId));

        verify(orderRepository).findByIdAndUserId(orderId, userId);
        verifyNoMoreInteractions(orderRepository);
    }

    @Test
    void getOrders_whenOrdersExists_returnPageOrderResponse() {
        Long userId = 1L;
        Long firstOrderId = 2L;
        Long secondOrderId = 3L;
        Long firstProductId = 4L;
        Long secondProductId = 5L;
        Long firstOrderItemId = 6L;
        Long secondOrderItemId = 7L;
        int firstProductStockQuantity = 10;
        int secondProductStockQuantity = 5;
        int firstItemQuantity = 2;
        int secondItemQuantity = 3;

        Product firstProduct = createProduct(
                firstProductId,
                VALID_PRODUCT_NAME,
                VALID_PRODUCT_PRICE,
                firstProductStockQuantity
        );

        Product secondProduct = createProduct(
                secondProductId,
                "XBOX",
                BigDecimal.valueOf(450.99),
                secondProductStockQuantity
        );

        User user = createUser(userId);
        BigDecimal firstOrderTotal = firstProduct.getPrice().multiply(BigDecimal.valueOf(firstItemQuantity));
        BigDecimal secondOrderTotal = secondProduct.getPrice().multiply(BigDecimal.valueOf(secondItemQuantity));

        Order firstOrder = createOrder(
                firstOrderId,
                firstOrderTotal,
                OrderStatus.PENDING_PAYMENT,
                user
        );

        Order secondOrder = createOrder(
                secondOrderId,
                secondOrderTotal,
                OrderStatus.PENDING_PAYMENT,
                user
        );

        OrderItem firstOrderItem = createOrderItem(
                firstOrderItemId,
                firstOrder,
                firstProduct,
                firstItemQuantity,
                firstProduct.getPrice()
        );

        firstOrder.addOrderItem(firstOrderItem);

        OrderItem secondOrderItem = createOrderItem(
                secondOrderItemId,
                secondOrder,
                secondProduct,
                secondItemQuantity,
                secondProduct.getPrice()
        );

        secondOrder.addOrderItem(secondOrderItem);
        List<Order> orders = List.of(firstOrder, secondOrder);

        OrderFilterRequest orderFilterRequest = new OrderFilterRequest(null, null, null, null, null);

        Pageable pageable = PageRequest.of(0, 2);
        Page<Order> orderPage = new PageImpl<>(orders, pageable, orders.size());

        when(orderRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(orderPage);

        Page<OrderResponse> orderResponses = orderService.getOrders(userId, orderFilterRequest, pageable);

        assertThat(orderResponses).isNotNull();
        assertThat(orderResponses.getContent()).hasSize(2);
        assertThat(orderResponses.getTotalElements()).isEqualTo(2);
        assertThat(orderResponses.getTotalPages()).isEqualTo(1);

        OrderResponse firstOrderResponse = orderResponses.getContent().getFirst();
        assertThat(firstOrderResponse.orderId()).isEqualTo(firstOrderId);
        assertThat(firstOrderResponse.userId()).isEqualTo(userId);
        assertThat(firstOrderResponse.items().getFirst().productId()).isEqualTo(firstProductId);

        OrderResponse secondOrderResponse = orderResponses.getContent().getLast();
        assertThat(secondOrderResponse.orderId()).isEqualTo(secondOrderId);
        assertThat(secondOrderResponse.userId()).isEqualTo(userId);
        assertThat(secondOrderResponse.items().getFirst().productId()).isEqualTo(secondProductId);

        verify(orderRepository).findAll(any(Specification.class), eq(pageable));
        verifyNoMoreInteractions(orderRepository);
    }

    @Test
    void getOrders_whenNoOrdersExists_returnPageOrderResponseWithEmptyList() {
        Long userId = 1L;

        OrderFilterRequest orderFilterRequest = new OrderFilterRequest(null, null, null, null, null);

        Pageable pageable = PageRequest.of(0, 2);
        Page<Order> orderPage = new PageImpl<>(List.of(), pageable, 0);

        when(orderRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(orderPage);

        Page<OrderResponse> orderResponses = orderService.getOrders(userId, orderFilterRequest, pageable);

        assertThat(orderResponses).isNotNull();
        assertThat(orderResponses.getContent()).hasSize(0);
        assertThat(orderResponses.getTotalElements()).isEqualTo(0);
        assertThat(orderResponses.getTotalPages()).isEqualTo(0);

        verify(orderRepository).findAll(any(Specification.class), eq(pageable));
        verifyNoMoreInteractions(orderRepository);
    }

    @Test
    void cancelOrder_whenOrderExists_orderSetStatusCancelledAndRestock() {
        Long userId = 1L;
        Long orderId = 2L;
        Long productId = 3L;
        Long orderItemId = 5L;
        int stockQuantity = 10;
        int orderItemQuantity = 2;

        Product product = createProduct(
                productId,
                VALID_PRODUCT_NAME,
                VALID_PRODUCT_PRICE,
                stockQuantity
        );

        User user = createUser(userId);
        BigDecimal total = product.getPrice().multiply(BigDecimal.valueOf(orderItemQuantity));

        Order order = createOrder(
                orderId,
                total,
                OrderStatus.PENDING_PAYMENT,
                user
        );

        OrderItem item = createOrderItem(
                orderItemId,
                order,
                product,
                orderItemQuantity,
                product.getPrice()
        );

        order.addOrderItem(item);

        when(orderRepository.findByIdAndUserId(orderId, userId)).thenReturn(Optional.of(order));

        orderService.cancelOrder(orderId, userId);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(product.getQuantity()).isEqualTo(stockQuantity + orderItemQuantity);

        verify(orderRepository).findByIdAndUserId(orderId, userId);
        verifyNoMoreInteractions(orderRepository);
    }

    @Test
    void cancelOrder_whenProductOutOfStockGotCancel_productUpdateToActive() {
        Long userId = 1L;
        Long orderId = 2L;
        Long productId = 3L;
        Long orderItemId = 5L;
        int stockQuantity = 0;
        int quantity = 2;

        Product product = createProduct(
                productId,
                VALID_PRODUCT_NAME,
                VALID_PRODUCT_PRICE,
                stockQuantity
        );

        product.updateStatusBasedOnQuantity();

        assertThat(product.getStatus()).isEqualTo(ProductStatus.OUT_OF_STOCK);

        User user = createUser(userId);
        BigDecimal total = product.getPrice().multiply(BigDecimal.valueOf(quantity));

        Order order = createOrder(
                orderId,
                total,
                OrderStatus.PENDING_PAYMENT,
                user
        );

        OrderItem item = createOrderItem(
                orderItemId,
                order,
                product,
                quantity,
                product.getPrice()
        );

        order.addOrderItem(item);

        when(orderRepository.findByIdAndUserId(orderId, userId)).thenReturn(Optional.of(order));

        orderService.cancelOrder(orderId, userId);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(product.getQuantity()).isEqualTo(stockQuantity + quantity);
        assertThat(product.getStatus()).isEqualTo(ProductStatus.ACTIVE);

        verify(orderRepository).findByIdAndUserId(orderId, userId);
        verifyNoMoreInteractions(orderRepository);
    }

    @Test
    void cancelOrder_whenOrderNotFoundForUser_throwsNoResourceFoundException() {
        Long userId = 1L;
        Long orderId = 2L;

        when(orderRepository.findByIdAndUserId(orderId, userId)).thenReturn(Optional.empty());

        NoResourceFoundException ex = assertThrows(
                NoResourceFoundException.class,
                () -> orderService.cancelOrder(orderId, userId)
        );

        assertThat(ex).isNotNull();
        assertThat(ex.getMessage()).isEqualTo(orderNotFoundWithIdAndUserId(orderId, userId));

        verify(orderRepository).findByIdAndUserId(orderId, userId);
        verifyNoMoreInteractions(orderRepository);
    }

    @Test
    void cancelOrder_whenOrderIsInDeliveredStatus_throwInvalidOrderStateException() {
        Long userId = 1L;
        Long orderId = 2L;
        Long productId = 3L;
        Long orderItemId = 5L;
        int initialQuantity = 10;
        int quantity = 2;

        Product product = createProduct(
                productId,
                VALID_PRODUCT_NAME,
                VALID_PRODUCT_PRICE,
                initialQuantity
        );

        User user = createUser(userId);
        BigDecimal total = product.getPrice().multiply(BigDecimal.valueOf(quantity));

        Order order = createOrder(
                orderId,
                total,
                OrderStatus.DELIVERED,
                user
        );

        OrderItem item = createOrderItem(
                orderItemId,
                order,
                product,
                quantity,
                product.getPrice()
        );

        order.addOrderItem(item);

        when(orderRepository.findByIdAndUserId(orderId, userId)).thenReturn(Optional.of(order));

        InvalidOrderStateException ex = assertThrows(
                InvalidOrderStateException.class,
                () -> orderService.cancelOrder(orderId, userId)
        );

        assertThat(ex).isNotNull();
        assertThat(ex.getMessage()).isEqualTo(DELIVERED_ORDER_CANNOT_BE_CANCELLED);
        assertThat(product.getQuantity()).isEqualTo(initialQuantity);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.DELIVERED);

        verify(orderRepository).findByIdAndUserId(orderId, userId);
        verifyNoMoreInteractions(orderRepository);
    }

    @Test
    void cancelOrder_whenOrderIsInCancelledStatus_throwInvalidOrderStateException() {
        Long userId = 1L;
        Long orderId = 2L;
        Long productId = 3L;
        Long orderItemId = 5L;
        int stockQuantity = 10;
        int quantity = 2;

        Product product = createProduct(
                productId,
                VALID_PRODUCT_NAME,
                VALID_PRODUCT_PRICE,
                stockQuantity
        );

        User user = createUser(userId);
        BigDecimal total = product.getPrice().multiply(BigDecimal.valueOf(quantity));

        Order order = createOrder(
                orderId,
                total,
                OrderStatus.CANCELLED,
                user
        );

        OrderItem orderItem = createOrderItem(
                orderItemId,
                order,
                product,
                quantity,
                product.getPrice()
        );

        order.addOrderItem(orderItem);

        when(orderRepository.findByIdAndUserId(orderId, userId)).thenReturn(Optional.of(order));

        InvalidOrderStateException ex = assertThrows(
                InvalidOrderStateException.class,
                () -> orderService.cancelOrder(orderId, userId)
        );

        assertThat(ex).isNotNull();
        assertThat(ex.getMessage()).isEqualTo(ORDER_ALREADY_CANCELLED);
        assertThat(product.getQuantity()).isEqualTo(stockQuantity);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);

        verify(orderRepository).findByIdAndUserId(orderId, userId);
        verifyNoMoreInteractions(orderRepository);
    }

    @Test
    void cancelOrder_whenOrderIsPaid_throwsInvalidOrderStateException() {
        Long userId = 1L;
        Long orderId = 2L;

        User user = createUser(userId);

        Order order = createOrder(
                orderId,
                BigDecimal.TEN,
                OrderStatus.PAID,
                user
        );

        when(orderRepository.findByIdAndUserId(orderId, userId))
                .thenReturn(Optional.of(order));

        InvalidOrderStateException ex = assertThrows(
                InvalidOrderStateException.class,
                () -> orderService.cancelOrder(orderId, userId)
        );

        assertThat(ex.getMessage())
                .isEqualTo(ORDER_CANNOT_BE_CANCELLED);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);

        verify(orderRepository).findByIdAndUserId(orderId, userId);
        verifyNoMoreInteractions(orderRepository);
    }

    @Test
    void checkoutCart_validRequest_returnOrderResponse() {
        Long userId = 1L;
        Long cartId = 2L;
        Long productId = 3L;
        Long cartItemId = 4L;
        Long orderId = 5L;
        int quantity = 2;
        int stockQuantity = 10;

        User user = createUser(userId);
        Product product = createProduct(
                productId,
                VALID_PRODUCT_NAME,
                VALID_PRODUCT_PRICE,
                stockQuantity
        );

        Cart cart = createCart(cartId, user);

        CartItem item = createCartItem(
                cartItemId,
                cart,
                product,
                quantity
        );

        cart.addItem(item);

        BigDecimal total = product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));

        when(cartLookupService.getCartByUserId(userId)).thenReturn(cart);
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order order = inv.getArgument(0);
            order.setId(orderId);
            return order;
        });

        OrderResponse orderResponse = orderService.checkoutCart(userId);

        assertThat(orderResponse).isNotNull();
        assertThat(orderResponse.orderId()).isEqualTo(orderId);
        assertThat(orderResponse.userId()).isEqualTo(userId);
        assertThat(orderResponse.total()).isEqualByComparingTo(total);
        assertThat(orderResponse.items()).hasSize(1);
        assertThat(orderResponse.status()).isEqualTo(OrderStatus.PENDING_PAYMENT);

        assertThat(product.getQuantity()).isEqualTo(stockQuantity - quantity);
        assertThat(product.getStatus()).isEqualTo(ProductStatus.ACTIVE);

        assertThat(item.getCart()).isNull();

        OrderItemResponse itemResponse = orderResponse.items().getFirst();
        assertThat(itemResponse.productId()).isEqualTo(productId);
        assertThat(itemResponse.productName()).isEqualTo(product.getName());
        assertThat(itemResponse.quantity()).isEqualTo(quantity);
        assertThat(itemResponse.price()).isEqualByComparingTo(product.getPrice());

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());

        Order savedOrder = orderCaptor.getValue();
        assertThat(savedOrder.getId()).isEqualTo(orderId);
        assertThat(savedOrder.getUser()).isEqualTo(user);
        assertThat(savedOrder.getUser().getId()).isEqualTo(userId);
        assertThat(savedOrder.getTotal()).isEqualByComparingTo(total);
        assertThat(savedOrder.getOrderItems()).hasSize(1);
        assertThat(savedOrder.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        assertThat(savedOrder.getTotal()).isEqualTo(total);
        assertThat(cart.getItems()).isEmpty();

        OrderItem savedOrderItem = savedOrder.getOrderItems().getFirst();
        assertThat(savedOrderItem.getOrder()).isEqualTo(savedOrder);
        assertThat(savedOrderItem.getProduct()).isEqualTo(product);
        assertThat(savedOrderItem.getQuantity()).isEqualTo(quantity);
        assertThat(savedOrderItem.getPrice()).isEqualByComparingTo(product.getPrice());

        verify(cartLookupService).getCartByUserId(userId);
        verifyNoMoreInteractions(cartLookupService);
        verifyNoMoreInteractions(orderRepository);
    }

    @Test
    void checkoutCart_whenCartNotFound_throwsNoResourceFoundException() {
        Long userId = 999L;

        when(cartLookupService.getCartByUserId(userId))
                .thenThrow(new NoResourceFoundException(cartNotFoundWithUserId(userId)));

        NoResourceFoundException ex = assertThrows(
                NoResourceFoundException.class,
                () -> orderService.checkoutCart(userId)
        );

        assertThat(ex).isNotNull();
        assertThat(ex.getMessage()).isEqualTo(cartNotFoundWithUserId(userId));

        verify(cartLookupService).getCartByUserId(userId);
        verifyNoInteractions(orderRepository);
    }

    @Test
    void checkoutCart_whenCartIsEmpty_throwsInvalidCartStateException() {
        Long userId = 1L;
        Long cartId = 2L;

        User user = createUser(userId);
        Cart cart = createCart(cartId, user);

        when(cartLookupService.getCartByUserId(userId)).thenReturn(cart);

        InvalidCartStateException ex = assertThrows(
                InvalidCartStateException.class,
                () -> orderService.checkoutCart(userId)
        );

        assertThat(ex).isNotNull();
        assertThat(ex.getMessage()).isEqualTo(EMPTY_CART);

        verify(cartLookupService).getCartByUserId(userId);
        verifyNoInteractions(orderRepository);
    }

    @Test
    void checkoutCart_whenQuantityExceedStocks_throwInsufficientStockException() {
        Long userId = 1L;
        Long cartId = 2L;
        Long productId = 3L;
        Long cartItemId = 4L;
        int quantity = 11;
        int stockQuantity = 10;

        User user = createUser(userId);
        Product product = createProduct(
                productId,
                VALID_PRODUCT_NAME,
                VALID_PRODUCT_PRICE,
                stockQuantity
        );

        Cart cart = createCart(cartId, user);

        CartItem item = createCartItem(
                cartItemId,
                cart,
                product,
                quantity
        );

        cart.addItem(item);

        when(cartLookupService.getCartByUserId(userId)).thenReturn(cart);

        InsufficientStockException ex = assertThrows(
                InsufficientStockException.class,
                () -> orderService.checkoutCart(userId)
        );

        assertThat(product.getQuantity()).isEqualTo(stockQuantity);
        assertThat(cart.getItems()).containsExactly(item);
        assertThat(item.getCart()).isEqualTo(cart);

        verify(cartLookupService).getCartByUserId(userId);
        verifyNoMoreInteractions(cartLookupService);
        verifyNoInteractions(orderRepository);
    }

    @Test
    void checkoutCart_whenQuantityIsNegative_throwsInvalidOrderException() {
        Long userId = 1L;
        Long cartId = 2L;
        Long productId = 3L;
        Long cartItemId = 4L;
        int quantity = -1;
        int stockQuantity = 10;

        User user = createUser(userId);
        Product product = createProduct(
                productId,
                VALID_PRODUCT_NAME,
                VALID_PRODUCT_PRICE,
                stockQuantity
        );

        Cart cart = createCart(cartId, user);

        CartItem item = createCartItem(
                cartItemId,
                cart,
                product,
                quantity
        );

        cart.addItem(item);

        when(cartLookupService.getCartByUserId(userId)).thenReturn(cart);

        InvalidOrderException ex = assertThrows(
                InvalidOrderException.class,
                () -> orderService.checkoutCart(userId)
        );

        assertThat(ex).isNotNull();
        assertThat(ex.getMessage()).isEqualTo(ORDER_ITEM_QUANTITY_IS_INVALID);

        assertThat(product.getQuantity()).isEqualTo(stockQuantity);
        assertThat(cart.getItems()).contains(item);
        assertThat(item.getCart()).isEqualTo(cart);

        verify(cartLookupService).getCartByUserId(userId);
        verifyNoMoreInteractions(cartLookupService);
        verifyNoInteractions(orderRepository);
    }

    @Test
    void checkoutCart_whenQuantityIsZero_throwsInvalidOrderException() {
        Long userId = 1L;
        Long cartId = 2L;
        Long productId = 3L;
        Long cartItemId = 4L;
        int quantity = 0;
        int stockQuantity = 10;

        User user = createUser(userId);
        Product product = createProduct(
                productId,
                VALID_PRODUCT_NAME,
                VALID_PRODUCT_PRICE,
                stockQuantity
        );

        Cart cart = createCart(cartId, user);

        CartItem item = createCartItem(
                cartItemId,
                cart,
                product,
                quantity
        );

        cart.addItem(item);

        when(cartLookupService.getCartByUserId(userId)).thenReturn(cart);

        InvalidOrderException ex = assertThrows(
                InvalidOrderException.class,
                () -> orderService.checkoutCart(userId)
        );

        assertThat(ex).isNotNull();
        assertThat(ex.getMessage()).isEqualTo(ORDER_ITEM_QUANTITY_IS_INVALID);

        assertThat(product.getQuantity()).isEqualTo(stockQuantity);
        assertThat(cart.getItems()).contains(item);
        assertThat(item.getCart()).isEqualTo(cart);

        verify(cartLookupService).getCartByUserId(userId);
        verifyNoMoreInteractions(cartLookupService);
        verifyNoInteractions(orderRepository);
    }

    @Test
    void checkoutCart_whenProductQuantityBecomesZero_marksProductOutOfStock() {
        Long userId = 1L;
        Long cartId = 2L;
        Long productId = 3L;
        Long cartItemId = 4L;
        Long orderId = 5L;
        int quantity = 10;
        int stockQuantity = 10;

        User user = createUser(userId);
        Product product = createProduct(
                productId,
                VALID_PRODUCT_NAME,
                VALID_PRODUCT_PRICE,
                stockQuantity
        );

        Cart cart = createCart(cartId, user);

        CartItem item = createCartItem(
                cartItemId,
                cart,
                product,
                quantity
        );

        cart.addItem(item);

        when(cartLookupService.getCartByUserId(userId)).thenReturn(cart);
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order order = inv.getArgument(0);
            order.setId(orderId);
            return order;
        });

        orderService.checkoutCart(userId);

        assertThat(product.getQuantity()).isEqualTo(0);
        assertThat(product.getStatus()).isEqualTo(ProductStatus.OUT_OF_STOCK);

        verify(cartLookupService).getCartByUserId(userId);
        verify(orderRepository).save(any(Order.class));
        verifyNoMoreInteractions(cartLookupService);
        verifyNoMoreInteractions(orderRepository);
    }
}
