package com.namnguyen.ecommerce_platform.order.repository;

import com.namnguyen.ecommerce_platform.order.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.Optional;

public interface OrderRepository extends
        JpaRepository<Order, Long>,
        JpaSpecificationExecutor<Order> {

    Optional<Order> findByIdAndUserId(Long orderId, Long userId);

    Page<Order> findByUserId(Long userId, Pageable pageable);

}
