package com.namnguyen.ecommerce_platform.order.service;

import com.namnguyen.ecommerce_platform.cart.entity.*;
import com.namnguyen.ecommerce_platform.cart.exception.InvalidCartStateException;
import com.namnguyen.ecommerce_platform.cart.service.CartLookupService;
import com.namnguyen.ecommerce_platform.common.response.PageResponse;
import com.namnguyen.ecommerce_platform.order.exception.InvalidOrderException;
import com.namnguyen.ecommerce_platform.order.exception.InvalidOrderStateException;
import com.namnguyen.ecommerce_platform.order.specifications.OrderSpecification;
import com.namnguyen.ecommerce_platform.order.dto.*;
import com.namnguyen.ecommerce_platform.order.entity.*;
import com.namnguyen.ecommerce_platform.order.enums.OrderStatus;
import com.namnguyen.ecommerce_platform.order.mapper.OrderMapper;
import com.namnguyen.ecommerce_platform.order.repository.OrderRepository;
import com.namnguyen.ecommerce_platform.product.entity.Product;
import com.namnguyen.ecommerce_platform.product.exception.InsufficientStockException;
import com.namnguyen.ecommerce_platform.product.service.ProductLookupService;
import com.namnguyen.ecommerce_platform.user.entity.User;
import com.namnguyen.ecommerce_platform.user.service.UserLookupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static com.namnguyen.ecommerce_platform.cart.error.CartErrorMessages.EMPTY_CART;
import static com.namnguyen.ecommerce_platform.order.error.OrderErrorMessages.*;
import static com.namnguyen.ecommerce_platform.product.error.ProductErrorMessages.insufficientStockForProduct;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final UserLookupService userLookupService;
    private final CartLookupService cartLookupService;
    private final ProductLookupService productLookupService;
    private final OrderLookupService orderLookupService;

    private void validateOrderItemQuantity(int quantity) {
        if (quantity <= 0) {
            throw new InvalidOrderException(ORDER_ITEM_QUANTITY_IS_INVALID);
        }
    }

    private OrderItem createOrderItem(CreateOrderItemRequest request, Order order) {
        Product product = productLookupService.getProductById(request.productId());
        return createOrderItem(product, request.quantity(), order);
    }

    private OrderItem createOrderItem(CartItem item, Order order) {

        return createOrderItem(item.getProduct(), item.getQuantity(), order);
    }

    private OrderItem createOrderItem(Product product, int quantity, Order order) {
        validateOrderItemQuantity(quantity);

        log.debug(
                "Creating order item productId={} quantity={}",
                product.getId(),
                quantity
        );

        if (product.getQuantity() < quantity) {
            throw new InsufficientStockException(insufficientStockForProduct(product.getName()));
        }

        product.setQuantity(product.getQuantity() - quantity);
        product.updateStatusBasedOnQuantity();

        OrderItem item = OrderItem.builder()
                .order(order)
                .product(product)
                .quantity(quantity)
                .price(product.getPrice())
                .build();

        return item;
    }

    private BigDecimal calculateTotal(List<OrderItem> items) {
        return items.stream().map(item ->
                item.getPrice()
                        .multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void restoreStock(Order order) {
        for (OrderItem item : order.getOrderItems()) {
            Product product = item.getProduct();
            product.setQuantity(product.getQuantity() + item.getQuantity());
            product.updateStatusBasedOnQuantity();
        }
    }

    private void validateOrderCanBeCancelled(Order order) {
        if (order.getStatus() == OrderStatus.DELIVERED) {
            throw new InvalidOrderStateException(DELIVERED_ORDER_CANNOT_BE_CANCELLED);
        }

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new InvalidOrderStateException(ORDER_ALREADY_CANCELLED);
        }

        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new InvalidOrderStateException(ORDER_CANNOT_BE_CANCELLED);
        }
    }

    private void validateCreateOrderRequest(CreateOrderRequest request) {
        if (request == null || request.items() == null || request.items().isEmpty()) {
            throw new InvalidOrderException(ORDER_IS_EMPTY);
        }

        boolean hasInvalidQuantity = request.items()
                .stream()
                .anyMatch(item -> item.quantity() <= 0);

        if (hasInvalidQuantity) {
            throw new InvalidOrderException(ORDER_ITEM_QUANTITY_IS_INVALID);
        }
    }

    @Override
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request, Long userId) {
        validateCreateOrderRequest(request);

        log.info(
                "Creating order userId={} itemCount={}",
                userId,
                request.items().size()
        );

        User user = userLookupService.getUserById(userId);

        Order order = new Order();
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setUser(user);

        List<OrderItem> items = request.items()
                .stream()
                .map(item -> createOrderItem(item, order))
                .toList();

        order.getOrderItems().addAll(items);
        order.setTotal(calculateTotal(items));

        Order savedOrder = orderRepository.save(order);

        log.info(
                "Order created orderId={} userId={} itemCount={}",
                savedOrder.getId(),
                userId,
                savedOrder.getOrderItems().size()
        );

        return OrderMapper.toResponse(savedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long orderId, Long userId) {
        log.debug("Fetching order orderId={} userId={}", orderId, userId);
        Order order = orderLookupService.getOrderByIdAndUserId(orderId, userId);
        return OrderMapper.toResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getOrders(Long userId, OrderFilterRequest request, Pageable pageable) {
        Specification<Order> spec = Specification
                .where(OrderSpecification.hasUserId(userId))
                .and(OrderSpecification.hasStatus(request.status()))
                .and(OrderSpecification.createdAfter(request.createdAfter()))
                .and(OrderSpecification.createdBefore(request.createdBefore()))
                .and(OrderSpecification.totalGreaterThanOrEqual(request.minTotal()))
                .and(OrderSpecification.totalLessThanOrEqual(request.maxTotal()));

        log.debug(
                "Fetching orders userId={} page={} size={} sort={}",
                userId,
                pageable.getPageNumber(),
                pageable.getPageSize(),
                pageable.getSort()
        );

        PageResponse<OrderResponse> orderResponsePage = PageResponse.from(orderRepository.findAll(spec, pageable)
                .map(OrderMapper::toResponse));

        log.debug(
                "Fetched orders page={} returned={} totalElements={}",
                orderResponsePage.page(),
                orderResponsePage.content().size(),
                orderResponsePage.totalElements()
        );

        return orderResponsePage;
    }

    @Override
    @Transactional
    public void cancelOrder(Long orderId, Long userId) {
        Order order = orderLookupService.getOrderByIdAndUserId(orderId, userId);

        log.info("Cancelling order orderId={} userId={}", orderId, userId);
        validateOrderCanBeCancelled(order);

        restoreStock(order);

        order.setStatus(OrderStatus.CANCELLED);

        log.info("Order cancelled orderId={}", orderId);
    }

    @Override
    @Transactional
    public OrderResponse checkoutCart(Long userId) {
        Cart cart = cartLookupService.getCartByUserId(userId);

        log.info("Checking out user cart userId={}", userId);

        if (cart.getItems().isEmpty()) {
            throw new InvalidCartStateException(EMPTY_CART);
        }

        Order order = new Order();
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setUser(cart.getUser());

        List<OrderItem> items = cart.getItems()
                .stream()
                .map(item -> createOrderItem(item, order))
                .toList();

        order.getOrderItems().addAll(items);
        order.setTotal(calculateTotal(items));

        Order savedOrder = orderRepository.save(order);

        cart.clearItems();

        log.info("Cart checked out userId={} orderId={}", userId, savedOrder.getId());

        return OrderMapper.toResponse(savedOrder);
    }
}
