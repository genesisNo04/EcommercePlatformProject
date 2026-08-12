package com.namnguyen.ecommerce_platform.product.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record ProductPatchRequest(

        @Size(min = 1, max = 100, message = "Product Name cannot exceed 100 characters")
        @Pattern(regexp = ".*\\S.*", message = "Product Name cannot be empty")
        String name,

        @Pattern(regexp = ".*\\S.*", message = "Product Description cannot be empty")
        @Size(min = 5, max = 1000, message = "Description has to be from 5 to 1000 chars")
        String description,

        @DecimalMin(value = "0.01", message = "Price must be greater than 0")
        BigDecimal price,

        @Min(value = 0, message = "Quantity cannot be negative")
        Integer quantity
) {}
