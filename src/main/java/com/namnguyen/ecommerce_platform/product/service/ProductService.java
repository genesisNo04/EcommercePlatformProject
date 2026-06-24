package com.namnguyen.ecommerce_platform.product.service;

import com.namnguyen.ecommerce_platform.product.dto.*;
import com.namnguyen.ecommerce_platform.product.enums.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

public interface ProductService {

    ProductResponse createProduct(ProductCreateRequest request);

    ProductResponse getProductById(Long id);

    Page<ProductResponse> getAllProducts(ProductFilterRequest request,
                                         Pageable pageable);

    ProductResponse putProduct(Long productId, ProductPutRequest request);

    ProductResponse patchProduct(Long productId, ProductPatchRequest request);

    void deleteProduct(Long id);
}
