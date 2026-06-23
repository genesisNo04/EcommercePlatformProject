package com.namnguyen.ecommerce_platform.product.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record ProductPatchRequest(

        @Size(min = 1, max = 100, message = "Name cannot be empty")
        @Pattern(regexp = ".*\\S.*", message = "Name cannot be blank")
        String name,

        @Pattern(regexp = ".*\\S.*", message = "Description cannot be blank")
        @Size(min = 5, max = 1000, message = "Description has to be from 5 to 1000 chars")
        String description,

        @DecimalMin(value = "0.01", message = "Price must be greater than 0")
        BigDecimal price,

        @Min(value = 0, message = "Quantity cannot be negative")
        Integer quantity
) {}
