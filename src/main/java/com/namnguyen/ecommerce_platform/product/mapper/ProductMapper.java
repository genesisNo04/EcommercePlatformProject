package com.namnguyen.ecommerce_platform.product.mapper;

import com.namnguyen.ecommerce_platform.product.dto.ProductCreateRequest;
import com.namnguyen.ecommerce_platform.product.dto.ProductResponse;
import com.namnguyen.ecommerce_platform.product.entity.Product;

public class ProductMapper {

    public static Product toEntity(ProductCreateRequest productCreateRequest) {
        return Product.builder()
                .name(productCreateRequest.name())
                .description(productCreateRequest.description())
                .price(productCreateRequest.price())
                .quantity(productCreateRequest.quantity())
                .build();
    }

    public static ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getQuantity(),
                product.getStatus(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}
