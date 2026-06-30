package com.namnguyen.ecommerce_platform.order.service;

import com.namnguyen.ecommerce_platform.cart.entity.Cart;
import com.namnguyen.ecommerce_platform.cart.service.CartLookupService;
import com.namnguyen.ecommerce_platform.common.exception.InsufficientStockException;
import com.namnguyen.ecommerce_platform.common.exception.InvalidOrderStateException;
import com.namnguyen.ecommerce_platform.common.exception.NoResourceFoundException;
import com.namnguyen.ecommerce_platform.order.specifications.OrderSpecification;
import com.namnguyen.ecommerce_platform.order.dto.*;
import com.namnguyen.ecommerce_platform.order.entity.*;
import com.namnguyen.ecommerce_platform.order.enums.OrderStatus;
import com.namnguyen.ecommerce_platform.order.mapper.OrderMapper;
import com.namnguyen.ecommerce_platform.order.repository.OrderRepository;
import com.namnguyen.ecommerce_platform.product.entity.Product;
import com.namnguyen.ecommerce_platform.product.repository.ProductRepository;
import com.namnguyen.ecommerce_platform.user.entity.User;
import com.namnguyen.ecommerce_platform.user.repository.UserRepository;
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
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final CartLookupService cartLookupService;

    private OrderItem createOrderItem(CreateOrderItemRequest request, Order order) {

        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new NoResourceFoundException("No product found with id: " + request.productId()));

        if (product.getQuantity() < request.quantity()) {
            throw new InsufficientStockException("Not enough stock for product: " + product.getName());
        }

        product.setQuantity(product.getQuantity() - request.quantity());
        product.updateStatusBasedOnQuantity();

        return OrderItem.builder()
                .order(order)
                .product(product)
                .quantity(request.quantity())
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

    @Override
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoResourceFoundException("No user found with id: " + userId));

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

        Order order = new Order();
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setUser(cart.getUser());

        List<OrderItem> items = cart.getItems()
                .stream()
                .map(item ->
                        OrderItem
                                .builder()
                                .order(order)
                                .product(item.getProduct())
                                .quantity(item.getQuantity())
                                .price(item.getProduct().getPrice())
                                .build())
                .toList();

        order.getOrderItems().addAll(items);
        order.setTotal(calculateTotal(items));

        return OrderMapper.toResponse(orderRepository.save(order));
    }
}
