package com.namnguyen.ecommerce_platform.order;

import com.namnguyen.ecommerce_platform.common.exception.InvalidOrderException;
import com.namnguyen.ecommerce_platform.common.exception.NoResourceFoundException;
import com.namnguyen.ecommerce_platform.order.dto.CreateOrderItemRequest;
import com.namnguyen.ecommerce_platform.order.dto.CreateOrderRequest;
import com.namnguyen.ecommerce_platform.order.dto.OrderItemResponse;
import com.namnguyen.ecommerce_platform.order.dto.OrderResponse;
import com.namnguyen.ecommerce_platform.order.entity.Order;
import com.namnguyen.ecommerce_platform.order.entity.OrderItem;
import com.namnguyen.ecommerce_platform.order.enums.OrderStatus;
import com.namnguyen.ecommerce_platform.order.repository.OrderRepository;
import com.namnguyen.ecommerce_platform.order.service.OrderLookupService;
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

import java.math.BigDecimal;
import java.util.List;

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
    private OrderLookupService orderLookupService;

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
    void createOrder_whenUserNotFound_throwNoResourceFoundException() {
        Long userId = 999L;
        CreateOrderRequest request = new CreateOrderRequest(
                List.of()
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
        verifyNoMoreInteractions(productLookupService);
        verifyNoInteractions(orderRepository);
    }

    @Test
    void createOrder_whenOrderItemsIsEmpty_throwInvalidOrderException() {
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
    void createOrder_whenOrderItemQuantitySmallerThanZero_throwInvalidOrderException() {
        Long userId = 1L;
        Long productId1 = 3L;
        int quantity1 = -1;

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

        InvalidOrderException ex = assertThrows(
                InvalidOrderException.class,
                () -> orderService.createOrder(request, userId)
        );

        assertThat(ex).isNotNull();
        assertThat(ex.getMessage()).isEqualTo(orderItemGreaterThanZero());

        verify(userLookupService).getUserById(userId);
        verify(productLookupService).getProductById(productId1);
        verifyNoMoreInteractions(productLookupService);
        verifyNoInteractions(orderRepository);
    }
}
