package com.namnguyen.ecommerce_platform.product.service;

import com.namnguyen.ecommerce_platform.common.config.CacheNames;
import com.namnguyen.ecommerce_platform.common.exception.NoResourceFoundException;
import com.namnguyen.ecommerce_platform.product.specifications.ProductSpecification;
import com.namnguyen.ecommerce_platform.product.dto.*;
import com.namnguyen.ecommerce_platform.product.entity.Product;
import com.namnguyen.ecommerce_platform.product.mapper.ProductMapper;
import com.namnguyen.ecommerce_platform.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    private Product getProductOrThrow(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new NoResourceFoundException("Product not found with id: " + productId));
    }

    @Override
    @Transactional
    @Caching(
            evict = {
                    @CacheEvict(value = CacheNames.PRODUCT_PAGES, allEntries = true)
            }
    )
    public ProductResponse createProduct(ProductCreateRequest request) {
        Product product = ProductMapper.toEntity(request);
        product.updateStatusBasedOnQuantity();
        Product savedProduct = productRepository.save(product);
        return ProductMapper.toResponse(savedProduct);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = CacheNames.PRODUCTS, key = "#productId")
    public ProductResponse getProductById(Long productId) {
        Product product = getProductOrThrow(productId);
        return ProductMapper.toResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(
            value = CacheNames.PRODUCT_PAGES,
            key= "(#request.status() == null ? 'null' : #request.status().name()) + ':' + " +
                    "#request.keyword() + ':' + " +
                    "#request.minPrice() + ':' + " +
                    "#request.maxPrice() + ':' + " +
                    "#pageable.pageNumber + ':' + " +
                    "#pageable.pageSize + ':' + " +
                    "#pageable.sort.toString().replace(' ', '')"
    )
    public Page<ProductResponse> getAllProducts(
            ProductFilterRequest request,
            Pageable pageable) {
        Specification<Product> spec = Specification
                .where(ProductSpecification.hasStatus(request.status()))
                .and(ProductSpecification.nameContains(request.keyword()))
                .and(ProductSpecification.priceGreaterThanOrEqual(request.minPrice()))
                .and(ProductSpecification.priceLessThanOrEqual(request.maxPrice()));

        return productRepository
                .findAll(spec, pageable)
                .map(ProductMapper::toResponse);
    }

    @Override
    @Transactional
    @Caching(
            put = {
                    @CachePut(value = CacheNames.PRODUCTS, key="#productId")
            },
            evict = {
                    @CacheEvict(value = CacheNames.PRODUCT_PAGES, allEntries = true)
            }
    )
    public ProductResponse putProduct(Long productId, ProductPutRequest request) {
        Product product = getProductOrThrow(productId);
        product.setName(request.name());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setQuantity(request.quantity());
        product.updateStatusBasedOnQuantity();
        return ProductMapper.toResponse(product);
    }

    @Override
    @Transactional
    @Caching(
            put = {
                    @CachePut(value = CacheNames.PRODUCTS, key="#productId")
            },
            evict = {
                    @CacheEvict(value = CacheNames.PRODUCT_PAGES, allEntries = true)
            }
    )
    public ProductResponse patchProduct(Long productId, ProductPatchRequest request) {
        Product product = getProductOrThrow(productId);

        if (request.name() != null) {
            product.setName(request.name());
        }

        if (request.description() != null) {
            product.setDescription(request.description());
        }

        if (request.price() != null) {
            product.setPrice(request.price());
        }

        if (request.quantity() != null) {
            product.setQuantity(request.quantity());
            product.updateStatusBasedOnQuantity();
        }

        return ProductMapper.toResponse(product);
    }

    @Override
    @Transactional
    @Caching(
            evict = {
                    @CacheEvict(value = CacheNames.PRODUCTS, key="#productId"),
                    @CacheEvict(value = CacheNames.PRODUCT_PAGES, allEntries = true)
            }
    )
    public void deleteProduct(Long productId) {
        Product product = getProductOrThrow(productId);
        productRepository.delete(product);
    }
}
