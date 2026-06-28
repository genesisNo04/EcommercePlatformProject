package com.namnguyen.ecommerce_platform.order.Specifications;

import com.namnguyen.ecommerce_platform.order.entity.Order;
import com.namnguyen.ecommerce_platform.order.enums.OrderStatus;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class OrderSpecification {

    public static Specification<Order> hasUserId(Long userId) {
        return (root, query, cb) -> {
            if (userId == null) {
                return null;
            }

            return cb.equal(root.get("user").get("id"), userId);
        };
    }

    public static Specification<Order> hasStatus(OrderStatus status) {
        return (root, query, cb) -> {
            if (status == null) {
                return null;
            }

            return cb.equal(root.get("status"), status);
        };
    }

    public static Specification<Order> totalGreaterThanOrEqual(BigDecimal minTotal) {
        return (root, query, cb) -> {
            if (minTotal == null) {
                return null;
            }

            return cb.greaterThanOrEqualTo(root.get("total"), minTotal);
        };
    }

    public static Specification<Order> totalLessThanOrEqual(BigDecimal maxTotal) {
        return (root, query, cb) -> {
            if (maxTotal == null) {
                return null;
            }

            return cb.lessThanOrEqualTo(root.get("total"), maxTotal);
        };
    }

    public static Specification<Order> createdAfter(LocalDateTime start) {
        if (start == null) {
            return null;
        }

        return (root, query, cb) ->
                cb.greaterThanOrEqualTo(root.get("createdAt"), start);
    }

    public static Specification<Order> createdBefore(LocalDateTime end) {
        if (end == null) {
            return null;
        }

        return (root, query, cb) ->
                cb.lessThanOrEqualTo(root.get("createdAt"), end);
    }
}
