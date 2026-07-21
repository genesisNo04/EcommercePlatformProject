package com.namnguyen.ecommerce_platform.order;

import com.namnguyen.ecommerce_platform.cart.entity.Cart;
import com.namnguyen.ecommerce_platform.cart.entity.CartItem;
import com.namnguyen.ecommerce_platform.cart.service.CartLookupService;
import com.namnguyen.ecommerce_platform.common.exception.InsufficientStockException;
import com.namnguyen.ecommerce_platform.common.exception.InvalidOrderException;
import com.namnguyen.ecommerce_platform.common.exception.InvalidOrderStateException;
import com.namnguyen.ecommerce_platform.common.exception.NoResourceFoundException;
import com.namnguyen.ecommerce_platform.order.dto.*;
import com.namnguyen.ecommerce_platform.order.entity.Order;
import com.namnguyen.ecommerce_platform.order.entity.OrderItem;
import com.namnguyen.ecommerce_platform.order.enums.OrderStatus;
import com.namnguyen.ecommerce_platform.order.repository.OrderRepository;
import com.namnguyen.ecommerce_platform.order.service.OrderServiceImpl;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.namnguyen.ecommerce_platform.testutil.TestMessages.*;
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
        Long productId1 = 3L;
        Long productId2 = 4L;
        int initialQuantity1 = 10;
        int initialQuantity2 = 5;
        int quantity1 = 2;
        int quantity2 = 3;

        Product product1 = createProduct(
                productId1,
                "PS5",
                BigDecimal.valueOf(499.99),
                initialQuantity1
        );

        Product product2 = createProduct(
                productId2,
                "XBOX",
                BigDecimal.valueOf(450.99),
                initialQuantity2
        );

        BigDecimal total = product1.getPrice().multiply(BigDecimal.valueOf(quantity1))
                .add(product2.getPrice().multiply(BigDecimal.valueOf(quantity2)));

        CreateOrderItemRequest item1 = new CreateOrderItemRequest(
                productId1,
                quantity1
        );

        CreateOrderItemRequest item2 = new CreateOrderItemRequest(
                productId2,
                quantity2
        );

        CreateOrderRequest request = new CreateOrderRequest(
                List.of(item1, item2)
        );

        User user = createUser(userId);

        when(userLookupService.getUserById(userId)).thenReturn(user);
        when(productLookupService.getProductById(productId1)).thenReturn(product1);
        when(productLookupService.getProductById(productId2)).thenReturn(product2);
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
           Order order = inv.getArgument(0);
           order.setId(orderId);
           return order;
        });

        OrderResponse response = orderService.createOrder(request, userId);

        assertThat(response).isNotNull();
        assertThat(response.orderId()).isEqualTo(orderId);
        assertThat(response.userId()).isEqualTo(userId);
        assertThat(response.total()).isEqualByComparingTo(total);
        assertThat(response.items()).hasSize(2);
        assertThat(response.status()).isEqualTo(OrderStatus.PENDING_PAYMENT);

        assertThat(product1.getQuantity()).isEqualTo(initialQuantity1 - quantity1);
        assertThat(product2.getQuantity()).isEqualTo(initialQuantity2 - quantity2);
        assertThat(product1.getStatus()).isEqualTo(ProductStatus.ACTIVE);
        assertThat(product2.getStatus()).isEqualTo(ProductStatus.ACTIVE);

        OrderItemResponse itemResponse1 = response.items().getFirst();
        assertThat(itemResponse1.productId()).isEqualTo(productId1);
        assertThat(itemResponse1.productName()).isEqualTo(product1.getName());
        assertThat(itemResponse1.quantity()).isEqualTo(item1.quantity());
        assertThat(itemResponse1.price()).isEqualByComparingTo(product1.getPrice());

        OrderItemResponse itemResponse2 = response.items().get(1);
        assertThat(itemResponse2.productId()).isEqualTo(productId2);
        assertThat(itemResponse2.productName()).isEqualTo(product2.getName());
        assertThat(itemResponse2.quantity()).isEqualTo(item2.quantity());
        assertThat(itemResponse2.price()).isEqualByComparingTo(product2.getPrice());

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());

        Order savedOrder = captor.getValue();
        assertThat(savedOrder.getId()).isEqualTo(orderId);
        assertThat(savedOrder.getUser()).isEqualTo(user);
        assertThat(savedOrder.getUser().getId()).isEqualTo(userId);
        assertThat(savedOrder.getTotal()).isEqualByComparingTo(total);
        assertThat(savedOrder.getOrderItems()).hasSize(2);
        assertThat(savedOrder.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);

        OrderItem savedOrderItem1 = savedOrder.getOrderItems().getFirst();
        assertThat(savedOrderItem1.getOrder()).isEqualTo(savedOrder);
        assertThat(savedOrderItem1.getProduct()).isEqualTo(product1);
        assertThat(savedOrderItem1.getQuantity()).isEqualTo(quantity1);
        assertThat(savedOrderItem1.getPrice()).isEqualByComparingTo(product1.getPrice());

        OrderItem savedOrderItem2 = savedOrder.getOrderItems().getLast();
        assertThat(savedOrderItem2.getOrder()).isEqualTo(savedOrder);
        assertThat(savedOrderItem2.getProduct()).isEqualTo(product2);
        assertThat(savedOrderItem2.getQuantity()).isEqualTo(quantity2);
        assertThat(savedOrderItem2.getPrice()).isEqualByComparingTo(product2.getPrice());

        verify(userLookupService).getUserById(userId);
        verify(productLookupService).getProductById(productId1);
        verify(productLookupService).getProductById(productId2);
        verifyNoMoreInteractions(userLookupService);
        verifyNoMoreInteractions(productLookupService);
        verifyNoMoreInteractions(orderRepository);
    }

    @Test
    void createOrder_whenProductQuantityBecomesZero_marksProductOutOfStock() {
        Long userId = 1L;
        Long productId1 = 3L;
        Long orderId = 4L;
        int quantity1 = 10;

        User user = createUser(userId);

        Product product1 = createProduct(
                productId1,
                "PS5",
                BigDecimal.valueOf(499.99),
                10
        );

        CreateOrderItemRequest item1 = new CreateOrderItemRequest(
                productId1,
                quantity1
        );

        CreateOrderRequest request = new CreateOrderRequest(
                List.of(item1)
        );

        when(userLookupService.getUserById(userId)).thenReturn(user);
        when(productLookupService.getProductById(productId1)).thenReturn(product1);
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order order = inv.getArgument(0);
            order.setId(orderId);
            return order;
        });

        OrderResponse response = orderService.createOrder(request, userId);

        assertThat(response).isNotNull();
        assertThat(response.items()).hasSize(1);
        assertThat(product1.getQuantity()).isEqualTo(0);
        assertThat(product1.getStatus()).isEqualTo(ProductStatus.OUT_OF_STOCK);

        verify(userLookupService).getUserById(userId);
        verify(productLookupService).getProductById(productId1);
        verify(orderRepository).save(any(Order.class));
        verifyNoMoreInteractions(userLookupService);
        verifyNoMoreInteractions(productLookupService);
        verifyNoMoreInteractions(orderRepository);
    }

    @Test
    void createOrder_whenUserNotFound_throwNoResourceFoundException() {
        Long userId = 999L;
        Long productId = 1L;
        int quantity1 = 2;

        CreateOrderItemRequest item1 = new CreateOrderItemRequest(
                productId,
                quantity1
        );

        CreateOrderRequest request = new CreateOrderRequest(
                List.of(item1)
        );

        when(userLookupService.getUserById(userId))
                .thenThrow(new NoResourceFoundException(userNotFound(userId)));

        NoResourceFoundException ex = assertThrows(
                NoResourceFoundException.class,
                () -> orderService.createOrder(request, userId)
        );

        assertThat(ex).isNotNull();
        assertThat(ex.getMessage()).isEqualTo(userNotFound(userId));

        verify(userLookupService).getUserById(userId);
        verifyNoInteractions(userLookupService);
        verifyNoInteractions(productLookupService);
        verifyNoInteractions(orderRepository);
    }

    @Test
    void createOrder_whenProductNotFound_throwNoResourceFoundException() {
        Long userId = 1L;
        Long productId = 2L;
        int quantity = 2;

        User user = createUser(userId);

        CreateOrderItemRequest requestItem = new CreateOrderItemRequest(
                productId,
                quantity
        );

        CreateOrderRequest request = new CreateOrderRequest(
                List.of(requestItem)
        );

        when(userLookupService.getUserById(userId)).thenReturn(user);
        when(productLookupService.getProductById(productId))
                .thenThrow(new NoResourceFoundException(productNotFound(productId)));

        NoResourceFoundException ex = assertThrows(
                NoResourceFoundException.class,
                () -> orderService.createOrder(request, userId)
        );

        assertThat(ex).isNotNull();
        assertThat(ex.getMessage()).isEqualTo(productNotFound(productId));

        verify(userLookupService).getUserById(userId);
        verify(productLookupService).getProductById(productId);
        verifyNoMoreInteractions(userLookupService);
        verifyNoMoreInteractions(productLookupService);
        verifyNoInteractions(orderRepository);
    }

    @Test
    void createOrder_whenItemsIsEmpty_throwsInvalidOrderException() {
        Long userId = 999L;
        CreateOrderRequest request = new CreateOrderRequest(
                List.of()
        );

        InvalidOrderException ex = assertThrows(
                InvalidOrderException.class,
                () -> orderService.createOrder(request, userId)
        );

        assertThat(ex).isNotNull();
        assertThat(ex.getMessage()).isEqualTo(orderHasAtLeastOneItem());

        verifyNoInteractions(userLookupService);
        verifyNoInteractions(productLookupService);
        verifyNoInteractions(orderRepository);
    }

    @Test
    void createOrder_whenItemsIsNull_throwsInvalidOrderException() {
        Long userId = 999L;
        CreateOrderRequest request = new CreateOrderRequest(
                null
        );

        InvalidOrderException ex = assertThrows(
                InvalidOrderException.class,
                () -> orderService.createOrder(request, userId)
        );

        assertThat(ex).isNotNull();
        assertThat(ex.getMessage()).isEqualTo(orderHasAtLeastOneItem());

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
        assertThat(ex.getMessage()).isEqualTo(orderHasAtLeastOneItem());

        verifyNoInteractions(userLookupService);
        verifyNoInteractions(productLookupService);
        verifyNoInteractions(orderRepository);
    }

    @Test
    void createOrder_whenItemQuantityIsNegative_throwsInvalidOrderException() {
        Long userId = 1L;
        Long productId1 = 3L;
        int quantity1 = -1;

        User user = createUser(userId);

        CreateOrderItemRequest item1 = new CreateOrderItemRequest(
                productId1,
                quantity1
        );

        CreateOrderRequest request = new CreateOrderRequest(
                List.of(item1)
        );

        when(userLookupService.getUserById(userId)).thenReturn(user);

        InvalidOrderException ex = assertThrows(
                InvalidOrderException.class,
                () -> orderService.createOrder(request, userId)
        );

        assertThat(ex).isNotNull();
        assertThat(ex.getMessage()).isEqualTo(orderItemGreaterThanZero());

        verify(userLookupService).getUserById(userId);
        verifyNoInteractions(productLookupService);
        verifyNoInteractions(orderRepository);
    }

    @Test
    void createOrder_whenItemQuantityIsZero_throwsInvalidOrderException() {
        Long userId = 1L;
        Long productId1 = 3L;
        int quantity1 = 0;

        User user = createUser(userId);

        CreateOrderItemRequest item1 = new CreateOrderItemRequest(
                productId1,
                quantity1
        );

        CreateOrderRequest request = new CreateOrderRequest(
                List.of(item1)
        );

        when(userLookupService.getUserById(userId)).thenReturn(user);

        InvalidOrderException ex = assertThrows(
                InvalidOrderException.class,
                () -> orderService.createOrder(request, userId)
        );

        assertThat(ex).isNotNull();
        assertThat(ex.getMessage()).isEqualTo(orderItemGreaterThanZero());

        verify(userLookupService).getUserById(userId);
        verifyNoInteractions(productLookupService);
        verifyNoInteractions(orderRepository);
    }

    @Test
    void createOrder_whenOrderItemQuantityExceedStock_throwInsufficientStockException() {
        Long userId = 1L;
        Long productId1 = 3L;
        int requestQuantity = 11;
        int quantity = 10;

        User user = createUser(userId);

        Product product1 = createProduct(
                productId1,
                "PS5",
                BigDecimal.valueOf(499.99),
                quantity
        );

        CreateOrderItemRequest item1 = new CreateOrderItemRequest(
                productId1,
                requestQuantity
        );

        CreateOrderRequest request = new CreateOrderRequest(
                List.of(item1)
        );

        when(userLookupService.getUserById(userId)).thenReturn(user);
        when(productLookupService.getProductById(productId1)).thenReturn(product1);

        InsufficientStockException ex = assertThrows(
                InsufficientStockException.class,
                () -> orderService.createOrder(request, userId)
        );

        assertThat(ex).isNotNull();
        assertThat(ex.getMessage()).isEqualTo(insufficientStock(product1.getName()));
        assertThat(product1.getQuantity()).isEqualTo(quantity);
        assertThat(product1.getStatus()).isEqualTo(ProductStatus.ACTIVE);

        verify(userLookupService).getUserById(userId);
        verify(productLookupService).getProductById(productId1);
        verifyNoMoreInteractions(userLookupService);
        verifyNoMoreInteractions(productLookupService);
        verifyNoInteractions(orderRepository);
    }

    @Test
    void getOrderById_whenOrderExists_returnOrderResponse() {
        Long userId = 1L;
        Long orderId = 2L;
        Long productId1 = 3L;
        Long productId2 = 4L;
        Long orderItemId1 = 5L;
        Long orderItemId2 = 6L;
        int initialQuantity1 = 10;
        int initialQuantity2 = 5;
        int quantity1 = 2;
        int quantity2 = 3;

        Product product1 = createProduct(
                productId1,
                "PS5",
                BigDecimal.valueOf(499.99),
                initialQuantity1
        );

        Product product2 = createProduct(
                productId2,
                "XBOX",
                BigDecimal.valueOf(450.99),
                initialQuantity2
        );

        User user = createUser(userId);
        BigDecimal total = product1.getPrice().multiply(BigDecimal.valueOf(quantity1))
                .add(product2.getPrice().multiply(BigDecimal.valueOf(quantity2)));

        Order order = createOrder(
                orderId,
                total,
                OrderStatus.PENDING_PAYMENT,
                user
        );

        OrderItem item1 = createOrderItem(
                orderItemId1,
                order,
                product1,
                quantity1,
                product1.getPrice()
        );

        OrderItem item2 = createOrderItem(
                orderItemId2,
                order,
                product2,
                quantity2,
                product2.getPrice()
        );

        order.addOrderItem(item1);
        order.addOrderItem(item2);

        when(orderRepository.findByIdAndUserId(orderId, userId)).thenReturn(Optional.of(order));

        OrderResponse response = orderService.getOrderById(orderId, userId);

        assertThat(response).isNotNull();
        assertThat(response.orderId()).isEqualTo(orderId);
        assertThat(response.userId()).isEqualTo(userId);
        assertThat(response.total()).isEqualByComparingTo(total);
        assertThat(response.status()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        assertThat(response.items()).hasSize(2);

        OrderItemResponse firstItem = response.items().getFirst();
        assertThat(firstItem.productId()).isEqualTo(productId1);
        assertThat(firstItem.productName()).isEqualTo(product1.getName());
        assertThat(firstItem.quantity()).isEqualTo(quantity1);
        assertThat(firstItem.price()).isEqualByComparingTo(product1.getPrice());

        OrderItemResponse secondItem = response.items().getLast();
        assertThat(secondItem.productId()).isEqualTo(productId2);
        assertThat(secondItem.productName()).isEqualTo(product2.getName());
        assertThat(secondItem.quantity()).isEqualTo(quantity2);
        assertThat(secondItem.price()).isEqualByComparingTo(product2.getPrice());

        verify(orderRepository).findByIdAndUserId(orderId, userId);
        verifyNoMoreInteractions(orderRepository);
    }

    @Test
    void getOrderById_whenOrderOrUserNotExists_returnNoResourceFoundException() {
        Long userId = 1L;
        Long orderId = 2L;

        when(orderRepository.findByIdAndUserId(orderId, userId)).thenReturn(Optional.empty());

        NoResourceFoundException ex = assertThrows(
                NoResourceFoundException.class,
                () -> orderService.getOrderById(orderId, userId)
        );

        assertThat(ex).isNotNull();
        assertThat(ex.getMessage()).isEqualTo(orderNotFound(orderId, userId));

        verify(orderRepository).findByIdAndUserId(orderId, userId);
        verifyNoMoreInteractions(orderRepository);
    }

    @Test
    void getOrders_whenOrdersExists_returnPageOrderResponse() {
        Long userId = 1L;
        Long orderId1 = 2L;
        Long orderId2 = 3L;
        Long productId1 = 4L;
        Long productId2 = 5L;
        Long orderItemId1 = 6L;
        Long orderItemId2 = 7L;
        int initialQuantity1 = 10;
        int initialQuantity2 = 5;
        int quantity1 = 2;
        int quantity2 = 3;

        Product product1 = createProduct(
                productId1,
                "PS5",
                BigDecimal.valueOf(499.99),
                initialQuantity1
        );

        Product product2 = createProduct(
                productId2,
                "XBOX",
                BigDecimal.valueOf(450.99),
                initialQuantity2
        );

        User user = createUser(userId);
        BigDecimal total1 = product1.getPrice().multiply(BigDecimal.valueOf(quantity1));
        BigDecimal total2 = product2.getPrice().multiply(BigDecimal.valueOf(quantity2));

        Order order1 = createOrder(
                orderId1,
                total1,
                OrderStatus.PENDING_PAYMENT,
                user
        );

        Order order2 = createOrder(
                orderId2,
                total2,
                OrderStatus.PENDING_PAYMENT,
                user
        );

        OrderItem item1 = createOrderItem(
                orderItemId1,
                order1,
                product1,
                quantity1,
                product1.getPrice()
        );

        order1.addOrderItem(item1);

        OrderItem item2 = createOrderItem(
                orderItemId2,
                order2,
                product2,
                quantity2,
                product2.getPrice()
        );

        order2.addOrderItem(item2);
        List<Order> orders = List.of(order1, order2);

        OrderFilterRequest request = new OrderFilterRequest(null, null, null, null, null);

        Pageable pageable = PageRequest.of(0, 2);
        Page<Order> orderPage = new PageImpl(orders, pageable, orders.size());

        when(orderRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(orderPage);

        Page<OrderResponse> response = orderService.getOrders(userId, request, pageable);

        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(2);
        assertThat(response.getTotalElements()).isEqualTo(2);
        assertThat(response.getTotalPages()).isEqualTo(1);

        OrderResponse firstOrder = response.getContent().getFirst();
        assertThat(firstOrder.orderId()).isEqualTo(orderId1);
        assertThat(firstOrder.userId()).isEqualTo(userId);
        assertThat(firstOrder.items().getFirst().productId()).isEqualTo(productId1);

        OrderResponse secondOrder = response.getContent().getLast();
        assertThat(secondOrder.orderId()).isEqualTo(orderId2);
        assertThat(secondOrder.userId()).isEqualTo(userId);
        assertThat(secondOrder.items().getFirst().productId()).isEqualTo(productId2);

        verify(orderRepository).findAll(any(Specification.class), eq(pageable));
        verifyNoMoreInteractions(orderRepository);
    }

    @Test
    void getOrders_whenNoOrdersExists_returnPageOrderResponseWithEmptyList() {
        Long userId = 1L;
        List<Order> orders = new ArrayList<>();

        OrderFilterRequest request = new OrderFilterRequest(null, null, null, null, null);

        Pageable pageable = PageRequest.of(0, 2);
        Page<Order> orderPage = new PageImpl(orders, pageable, orders.size());

        when(orderRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(orderPage);

        Page<OrderResponse> response = orderService.getOrders(userId, request, pageable);

        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(0);
        assertThat(response.getTotalElements()).isEqualTo(0);
        assertThat(response.getTotalPages()).isEqualTo(0);

        verify(orderRepository).findAll(any(Specification.class), eq(pageable));
        verifyNoMoreInteractions(orderRepository);
    }

    @Test
    void cancelOrder_whenOrderExists_orderSetStatusCancelledAndRestock() {
        Long userId = 1L;
        Long orderId = 2L;
        Long productId1 = 3L;
        Long orderItemId1 = 5L;
        int initialQuantity1 = 10;
        int quantity1 = 2;

        Product product1 = createProduct(
                productId1,
                "PS5",
                BigDecimal.valueOf(499.99),
                initialQuantity1
        );

        User user = createUser(userId);
        BigDecimal total = product1.getPrice().multiply(BigDecimal.valueOf(quantity1));

        Order order = createOrder(
                orderId,
                total,
                OrderStatus.PENDING_PAYMENT,
                user
        );

        OrderItem item1 = createOrderItem(
                orderItemId1,
                order,
                product1,
                quantity1,
                product1.getPrice()
        );

        order.addOrderItem(item1);

        when(orderRepository.findByIdAndUserId(orderId, userId)).thenReturn(Optional.of(order));

        orderService.cancelOrder(orderId, userId);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(product1.getQuantity()).isEqualTo(initialQuantity1 + quantity1);

        verify(orderRepository).findByIdAndUserId(orderId, userId);
        verifyNoMoreInteractions(orderRepository);
    }

    @Test
    void cancelOrder_whenOrderOrUserNotExists_throwNoResourceFoundException() {
        Long userId = 1L;
        Long orderId = 2L;

        when(orderRepository.findByIdAndUserId(orderId, userId)).thenReturn(Optional.empty());

        NoResourceFoundException ex = assertThrows(
                NoResourceFoundException.class,
                () -> orderService.cancelOrder(orderId, userId)
        );

        assertThat(ex).isNotNull();
        assertThat(ex.getMessage()).isEqualTo(orderNotFound(orderId, userId));

        verify(orderRepository).findByIdAndUserId(orderId, userId);
        verifyNoMoreInteractions(orderRepository);
    }

    @Test
    void cancelOrder_whenOrderIsInDeliveredStatus_throwInvalidOrderStateException() {
        Long userId = 1L;
        Long orderId = 2L;
        Long productId1 = 3L;
        Long orderItemId1 = 5L;
        int initialQuantity1 = 10;
        int quantity1 = 2;

        Product product1 = createProduct(
                productId1,
                "PS5",
                BigDecimal.valueOf(499.99),
                initialQuantity1
        );

        User user = createUser(userId);
        BigDecimal total = product1.getPrice().multiply(BigDecimal.valueOf(quantity1));

        Order order = createOrder(
                orderId,
                total,
                OrderStatus.DELIVERED,
                user
        );

        OrderItem item1 = createOrderItem(
                orderItemId1,
                order,
                product1,
                quantity1,
                product1.getPrice()
        );

        order.addOrderItem(item1);

        when(orderRepository.findByIdAndUserId(orderId, userId)).thenReturn(Optional.of(order));

        InvalidOrderStateException ex = assertThrows(
                InvalidOrderStateException.class,
                () -> orderService.cancelOrder(orderId, userId)
        );

        assertThat(ex).isNotNull();
        assertThat(ex.getMessage()).isEqualTo(cannotCancelDeliveredOrder());

        verify(orderRepository).findByIdAndUserId(orderId, userId);
        verifyNoMoreInteractions(orderRepository);
    }

    @Test
    void cancelOrder_whenOrderIsInCancelledStatus_throwInvalidOrderStateException() {
        Long userId = 1L;
        Long orderId = 2L;
        Long productId1 = 3L;
        Long orderItemId1 = 5L;
        int initialQuantity1 = 10;
        int quantity1 = 2;

        Product product1 = createProduct(
                productId1,
                "PS5",
                BigDecimal.valueOf(499.99),
                initialQuantity1
        );

        User user = createUser(userId);
        BigDecimal total = product1.getPrice().multiply(BigDecimal.valueOf(quantity1));

        Order order = createOrder(
                orderId,
                total,
                OrderStatus.CANCELLED,
                user
        );

        OrderItem item1 = createOrderItem(
                orderItemId1,
                order,
                product1,
                quantity1,
                product1.getPrice()
        );

        order.addOrderItem(item1);

        when(orderRepository.findByIdAndUserId(orderId, userId)).thenReturn(Optional.of(order));

        InvalidOrderStateException ex = assertThrows(
                InvalidOrderStateException.class,
                () -> orderService.cancelOrder(orderId, userId)
        );

        assertThat(ex).isNotNull();
        assertThat(ex.getMessage()).isEqualTo(orderAlreadyCancelled());

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
        int initialQuantity = 10;

        User user = createUser(userId);
        Product product = createProduct(
                productId,
                "PS5",
                BigDecimal.valueOf(499.99),
                initialQuantity
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

        OrderResponse response = orderService.checkoutCart(userId);

        assertThat(response).isNotNull();
        assertThat(response.orderId()).isEqualTo(orderId);
        assertThat(response.userId()).isEqualTo(userId);
        assertThat(response.total()).isEqualByComparingTo(total);
        assertThat(response.items()).hasSize(1);
        assertThat(response.status()).isEqualTo(OrderStatus.PENDING_PAYMENT);

        assertThat(product.getQuantity()).isEqualTo(initialQuantity - quantity);
        assertThat(product.getStatus()).isEqualTo(ProductStatus.ACTIVE);

        OrderItemResponse itemResponse1 = response.items().getFirst();
        assertThat(itemResponse1.productId()).isEqualTo(productId);
        assertThat(itemResponse1.productName()).isEqualTo(product.getName());
        assertThat(itemResponse1.quantity()).isEqualTo(quantity);
        assertThat(itemResponse1.price()).isEqualByComparingTo(product.getPrice());

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());

        Order savedOrder = captor.getValue();
        assertThat(savedOrder.getId()).isEqualTo(orderId);
        assertThat(savedOrder.getUser()).isEqualTo(user);
        assertThat(savedOrder.getUser().getId()).isEqualTo(userId);
        assertThat(savedOrder.getTotal()).isEqualByComparingTo(total);
        assertThat(savedOrder.getOrderItems()).hasSize(1);
        assertThat(savedOrder.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);

        OrderItem savedOrderItem1 = savedOrder.getOrderItems().getFirst();
        assertThat(savedOrderItem1.getOrder()).isEqualTo(savedOrder);
        assertThat(savedOrderItem1.getProduct()).isEqualTo(product);
        assertThat(savedOrderItem1.getQuantity()).isEqualTo(quantity);
        assertThat(savedOrderItem1.getPrice()).isEqualByComparingTo(product.getPrice());

        verify(cartLookupService).getCartByUserId(userId);
        verify(orderRepository).save(any(Order.class));
        verifyNoMoreInteractions(cartLookupService);
        verifyNoMoreInteractions(orderRepository);
    }
}
