package com.namnguyen.ecommerce_platform.order.service;

import com.namnguyen.ecommerce_platform.common.exception.NoResourceFoundException;
import com.namnguyen.ecommerce_platform.order.dto.CreateOrderItemRequest;
import com.namnguyen.ecommerce_platform.order.dto.CreateOrderRequest;
import com.namnguyen.ecommerce_platform.order.dto.OrderResponse;
import com.namnguyen.ecommerce_platform.order.entity.Order;
import com.namnguyen.ecommerce_platform.order.entity.OrderItem;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    private User getUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new NoResourceFoundException("No user found with email: " + email));
    }

    private List<OrderItem> createListOrderItem(List<CreateOrderItemRequest> requests, Order order) {
        List<OrderItem> items = new ArrayList<>();
        for (CreateOrderItemRequest request : requests) {
            OrderItem item = new OrderItem();
            Product product = productRepository.findById(request.productId())
                    .orElseThrow(() -> new NoResourceFoundException("No product found with id: " + request.productId()));
            item.setProduct(product);
            item.setQuantity(request.quantity());
            item.setPrice(product.getPrice());
            item.setOrder(order);
            items.add(item);
        }
        return items;
    }

    @Override
    public OrderResponse createOrder(CreateOrderRequest request) {
        Order order = new Order();
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setUser(getUser());
        orderRepository.save(order);
        List<OrderItem> items = createListOrderItem(request.items(), order);
        BigDecimal total = new BigDecimal(0);
        for (OrderItem item : items) {
            total = total.add(item.getPrice().multiply(new BigDecimal(item.getQuantity())));
        }
        order.setTotal(total);
        order.setOrderItems(items);
        Order savedOrder = orderRepository.save(order);

        return OrderMapper.toResponse(savedOrder);
    }

    @Override
    public OrderResponse getOrderByIdAndUserId(Long orderId) {
        return null;
    }

    @Override
    public Page<OrderResponse> getOrdersAndUserId(Pageable pageable) {
        return null;
    }

    @Override
    public void cancelOrder(Long orderId) {

    }
}
