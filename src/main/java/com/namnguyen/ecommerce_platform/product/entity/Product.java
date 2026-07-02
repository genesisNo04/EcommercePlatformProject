package com.namnguyen.ecommerce_platform.product.entity;

import com.namnguyen.ecommerce_platform.product.enums.ProductStatus;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(
        name = "products",
        indexes = {
                @Index(name = "idx_products_name", columnList = "name"),
                @Index(name = "idx_products_price", columnList = "price"),
                @Index(name = "idx_products_created_at", columnList = "created_at"),
                @Index(name = "idx_products_status", columnList = "status"),
                @Index(name = "idx_products_status_price", columnList = "status,price")
        }
)
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

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
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
