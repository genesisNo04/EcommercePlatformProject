package com.namnguyen.ecommerce_platform.product.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record ProductPutRequest(

        @NotBlank(message = "Product Name is required")
        @Size(max = 100, message = "Product Name cannot exceed 100 characters")
        String name,

        @NotBlank(message = "Product Description is required")
        @Size(min = 5, max = 1000, message = "Description has to be from 5 to 1000 chars")
        String description,

        @NotNull(message = "Product Price is required")
        @DecimalMin(value = "0.01", message = "Price must be greater than 0")
        BigDecimal price,

        @NotNull(message = "Product Quantity is required")
        @Min(value = 0, message = "Quantity cannot be negative")
        Integer quantity
) {
}
