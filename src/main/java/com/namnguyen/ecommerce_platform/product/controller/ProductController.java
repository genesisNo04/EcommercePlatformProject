package com.namnguyen.ecommerce_platform.product.controller;

import com.namnguyen.ecommerce_platform.product.dto.*;
import com.namnguyen.ecommerce_platform.product.enums.ProductStatus;
import com.namnguyen.ecommerce_platform.product.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody ProductCreateRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(productService.createProduct(request));
    }

    @GetMapping
    public ResponseEntity<Page<ProductResponse>> getAllProducts(
            @ModelAttribute ProductFilterRequest request,
            @PageableDefault(
                    size = 10,
                    sort = "id",
                    direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(productService.getAllProducts(request, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> putProduct(@PathVariable Long id, @Valid @RequestBody ProductPutRequest request) {
        return ResponseEntity
                .ok(productService.putProduct(id, request));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ProductResponse> patchProduct(@PathVariable Long id, @Valid @RequestBody ProductPatchRequest request) {
        return ResponseEntity
                .ok(productService.patchProduct(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProductById(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}
