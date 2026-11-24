package com.namnguyen.ecommerce_platform.product.exception;

public class ProductVersionConflictException extends RuntimeException {
    public ProductVersionConflictException(String message) {
        super(message);
    }
}
