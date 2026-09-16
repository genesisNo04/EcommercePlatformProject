package com.namnguyen.ecommerce_platform.cart.exception;

public class InvalidCartStateException extends RuntimeException {
    public InvalidCartStateException(String message) {
        super(message);
    }
}
