package com.namnguyen.ecommerce_platform.product.repository;

import com.namnguyen.ecommerce_platform.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends
        JpaRepository<Product, Long>,
        JpaSpecificationExecutor<Product> {

    Optional<Product> findByName(String name);

    boolean existsByName(String name);

    @Modifying
    @Query(value = """
        UPDATE products
        SET quantity = quantity - :qty,
            status = CASE WHEN quantity - :qty <= 0 THEN 'OUT_OF_STOCK' ELSE 'ACTIVE' END,
            updated_at = now()
        WHERE id = :id AND quantity >= :qty
    """, nativeQuery = true)
    int decreaseStock(@Param("id") Long id, @Param("qty") int qty);

    @Modifying
    @Query(value = """
        UPDATE products
        SET quantity = quantity + :qty,
            status = 'ACTIVE',
            updated_at = now()
        WHERE id = :id
    """, nativeQuery = true)
    int increaseStock(@Param("id") Long id, @Param("qty") int qty);
}
