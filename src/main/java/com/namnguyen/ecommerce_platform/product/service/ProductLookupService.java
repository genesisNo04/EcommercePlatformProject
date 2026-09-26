package com.namnguyen.ecommerce_platform.product.service;

import com.namnguyen.ecommerce_platform.common.exception.NoResourceFoundException;
import com.namnguyen.ecommerce_platform.product.entity.Product;
import com.namnguyen.ecommerce_platform.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static com.namnguyen.ecommerce_platform.product.error.ProductErrorMessages.productNotFoundWithId;
import static com.namnguyen.ecommerce_platform.product.error.ProductErrorMessages.productNotFoundWithName;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductLookupService {
    private final ProductRepository productRepository;

    public Product getProductById(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NoResourceFoundException(productNotFoundWithId(productId)));
        log.debug("Fetched product productId={}", productId);
        return product;
    }

    public Product getProductByName(String name) {
        Product product = productRepository.findByName(name)
                .orElseThrow(() -> new NoResourceFoundException(productNotFoundWithName(name)));

        log.debug("Fetched product by name productId={}", product.getId());
        return product;
    }
}
