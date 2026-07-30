package com.namnguyen.ecommerce_platform.order.service;

import com.namnguyen.ecommerce_platform.cart.entity.*;
import com.namnguyen.ecommerce_platform.cart.service.CartLookupService;
import com.namnguyen.ecommerce_platform.common.exception.*;
import com.namnguyen.ecommerce_platform.order.specifications.OrderSpecification;
import com.namnguyen.ecommerce_platform.order.dto.*;
import com.namnguyen.ecommerce_platform.order.entity.*;
import com.namnguyen.ecommerce_platform.order.enums.OrderStatus;
import com.namnguyen.ecommerce_platform.order.mapper.OrderMapper;
import com.namnguyen.ecommerce_platform.order.repository.OrderRepository;
import com.namnguyen.ecommerce_platform.product.entity.Product;
import com.namnguyen.ecommerce_platform.product.repository.ProductRepository;
import com.namnguyen.ecommerce_platform.product.service.ProductLookupService;
import com.namnguyen.ecommerce_platform.user.entity.User;
import com.namnguyen.ecommerce_platform.user.service.UserLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final UserLookupService userLookupService;
    private final CartLookupService cartLookupService;
    private final ProductLookupService productLookupService;

    private void validateOrderItemQuantity(int quantity) {
        if (quantity <= 0) {
            throw new InvalidOrderException("Order item quantity must be greater than zero");
        }
    }

    private OrderItem createOrderItem(CreateOrderItemRequest request, Order order) {
        validateOrderItemQuantity(request.quantity());
        Product product = productLookupService.getProductById(request.productId());
        return createOrderItem(product, request.quantity(), order);
    }

    private OrderItem createOrderItem(CartItem item, Order order) {

        return createOrderItem(item.getProduct(), item.getQuantity(), order);
    }

    private OrderItem createOrderItem(Product product, int quantity, Order order) {
        validateOrderItemQuantity(quantity);

        if (product.getQuantity() < quantity) {
            throw new InsufficientStockException("Not enough stock for product: " + product.getName());
        }

        product.setQuantity(product.getQuantity() - quantity);
        product.updateStatusBasedOnQuantity();

        return OrderItem.builder()
                .order(order)
                .product(product)
                .quantity(quantity)
                .price(product.getPrice())
                .build();
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
            throw new InvalidOrderStateException("Delivered order cannot be cancelled");
        }

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new InvalidOrderStateException("Order is already cancelled");
        }
    }

    private void validateCreateOrderRequests(CreateOrderRequest request) {
        if (request == null || request.items() == null || request.items().isEmpty()) {
            throw new InvalidOrderException("Order must contain at least one item");
        }
    }

    @Override
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request, Long userId) {
        validateCreateOrderRequests(request);

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

        return OrderMapper.toResponse(orderRepository.save(order));
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long orderId, Long userId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() ->
                        new NoResourceFoundException("No order with id: " + orderId +  " found for this user id: " + userId));
        return OrderMapper.toResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getOrders(Long userId, OrderFilterRequest request, Pageable pageable) {
        Specification<Order> spec = Specification
                .where(OrderSpecification.hasUserId(userId))
                .and(OrderSpecification.hasStatus(request.status()))
                .and(OrderSpecification.createdAfter(request.createdAfter()))
                .and(OrderSpecification.createdBefore(request.createdBefore()))
                .and(OrderSpecification.totalGreaterThanOrEqual(request.minTotal()))
                .and(OrderSpecification.totalLessThanOrEqual(request.maxTotal()));

        return orderRepository.findAll(spec, pageable)
                .map(OrderMapper::toResponse);
    }

    @Override
    @Transactional
    public void cancelOrder(Long orderId, Long userId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() ->
                        new NoResourceFoundException("No order with id: " + orderId +  " found for this user id: " + userId));

        validateOrderCanBeCancelled(order);

        restoreStock(order);

        order.setStatus(OrderStatus.CANCELLED);
    }

    @Override
    @Transactional
    public OrderResponse checkoutCart(Long userId) {
        Cart cart = cartLookupService.getCartByUserId(userId);

        if (cart.getItems().isEmpty()) {
            throw new InvalidOrderStateException("Cannot checkout an empty cart");
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

        return OrderMapper.toResponse(savedOrder);
    }
}
