package com.namnguyen.ecommerce_platform.product.repository;

import com.namnguyen.ecommerce_platform.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends
        JpaRepository<Product, Long>,
        JpaSpecificationExecutor<Product> {

    Optional<Product> findByName(String name);

    boolean existsByName(String name);
}
