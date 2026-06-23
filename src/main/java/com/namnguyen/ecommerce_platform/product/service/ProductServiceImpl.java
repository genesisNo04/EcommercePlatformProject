package com.namnguyen.ecommerce_platform.product.service;

import com.namnguyen.ecommerce_platform.common.exception.NoResourceFoundException;
import com.namnguyen.ecommerce_platform.product.dto.ProductCreateRequest;
import com.namnguyen.ecommerce_platform.product.dto.ProductPatchRequest;
import com.namnguyen.ecommerce_platform.product.dto.ProductPutRequest;
import com.namnguyen.ecommerce_platform.product.dto.ProductResponse;
import com.namnguyen.ecommerce_platform.product.entity.Product;
import com.namnguyen.ecommerce_platform.product.mapper.ProductMapper;
import com.namnguyen.ecommerce_platform.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    private Product getOrThrow(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new NoResourceFoundException("Product not found with id: " + productId));
    }

    @Override
    @Transactional
    public ProductResponse createProduct(ProductCreateRequest request) {
        Product product = productRepository.save(ProductMapper.toEntity(request));
        return ProductMapper.toResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long productId) {
        Product product = getOrThrow(productId);
        return ProductMapper.toResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getAllProducts() {
        List<Product> products = productRepository.findAll();
        return products.stream().map(ProductMapper::toResponse).toList();
    }

    @Override
    @Transactional
    public ProductResponse putProduct(Long productId, ProductPutRequest request) {
        Product product = getOrThrow(productId);
        return null;
    }

    @Override
    @Transactional
    public ProductResponse patchProduct(Long productId, ProductPatchRequest request) {
        Product product = getOrThrow(productId);
        return null;
    }

    @Override
    @Transactional
    public void deleteProduct(Long productId) {
        Product product = getOrThrow(productId);
        productRepository.delete(product);
    }
}
