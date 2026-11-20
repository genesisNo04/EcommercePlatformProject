package com.namnguyen.ecommerce_platform.order.repository;

import com.namnguyen.ecommerce_platform.order.entity.Order;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface OrderRepository extends
        JpaRepository<Order, Long>,
        JpaSpecificationExecutor<Order> {

    Optional<Order> findByIdAndUserId(Long orderId, Long userId);

    Page<Order> findByUserId(Long userId, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Order> findWithLockByIdAndUserId(Long orderId, Long userId);

}
