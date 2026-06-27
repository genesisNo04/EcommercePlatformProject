package com.namnguyen.ecommerce_platform.product.entity;

import com.namnguyen.ecommerce_platform.order.entity.OrderItem;
import com.namnguyen.ecommerce_platform.product.enums.ProductStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "products")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 1000)
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private Integer quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductStatus status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        updateStatusBasedOnQuantity();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
        updateStatusBasedOnQuantity();
    }

    public void updateStatusBasedOnQuantity() {
        if (this.quantity == null || this.quantity <= 0) {
            this.status = ProductStatus.OUT_OF_STOCK;
        } else {
            this.status = ProductStatus.ACTIVE;
        }
    }
}
