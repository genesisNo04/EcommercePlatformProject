package com.namnguyen.ecommerce_platform.product.service;

import com.namnguyen.ecommerce_platform.common.exception.NoResourceFoundException;
import com.namnguyen.ecommerce_platform.product.entity.Product;
import com.namnguyen.ecommerce_platform.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import static com.namnguyen.ecommerce_platform.product.error.ProductErrorMessages.productNotFoundWithId;
import static com.namnguyen.ecommerce_platform.product.error.ProductErrorMessages.productNotFoundWithName;

@Service
@RequiredArgsConstructor
public class ProductLookupService {
    private final ProductRepository productRepository;

    public Product getProductById(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new NoResourceFoundException(productNotFoundWithId(productId)));
    }

    public Product getProductByName(String name) {
        return productRepository.findByName(name)
                .orElseThrow(() -> new NoResourceFoundException(productNotFoundWithName(name)));
    }
}
