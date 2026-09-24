package com.namnguyen.ecommerce_platform.product.service;

import com.namnguyen.ecommerce_platform.common.response.PageResponse;
import com.namnguyen.ecommerce_platform.product.dto.*;
import org.springframework.data.domain.Pageable;

public interface ProductService {

    ProductResponse createProduct(ProductCreateRequest request);

    ProductResponse getProductById(Long id);

    PageResponse<ProductResponse> getAllProducts(ProductFilterRequest request,
                                                 Pageable pageable);

    ProductResponse putProduct(Long productId, ProductPutRequest request);

    ProductResponse patchProduct(Long productId, ProductPatchRequest request);

    void deleteProduct(Long id);
}
