package com.namnguyen.ecommerce_platform.product.service;

import com.namnguyen.ecommerce_platform.product.entity.Product;
import com.namnguyen.ecommerce_platform.product.exception.InsufficientStockException;
import com.namnguyen.ecommerce_platform.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.namnguyen.ecommerce_platform.product.error.ProductErrorMessages.insufficientStockForProduct;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductStockService {

    private final ProductRepository productRepository;

    @Transactional
    public void decreaseStock(Product product, int quantity) {
        int updated = productRepository.decreaseStock(product.getId(), quantity);

        if (updated == 0) {
            throw new InsufficientStockException(insufficientStockForProduct(product.getName()));
        }

        log.debug("Stock decreased productId={} quantity={}", product.getId(), quantity);
    }

    @Transactional
    public void increaseStock(Product product, int quantity) {
        productRepository.increaseStock(product.getId(), quantity);
        log.debug("Stock increased productId={} quantity={}", product.getId(), quantity);
    }
}
