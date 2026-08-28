package com.namnguyen.ecommerce_platform.product.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

import static com.namnguyen.ecommerce_platform.product.error.ProductErrorMessages.*;

public record ProductPatchRequest(

        @Pattern(regexp = ".*\\S.*", message = PRODUCT_NAME_IS_EMPTY)
        @Size(max = 100, message = PRODUCT_NAME_IS_INVALID)
        String name,

        @Pattern(regexp = ".*\\S.*", message = PRODUCT_DESCRIPTION_IS_EMPTY)
        @Size(min = 5, max = 1000, message = PRODUCT_DESCRIPTION_IS_INVALID)
        String description,

        @DecimalMin(value = "0.01", message = PRODUCT_PRICE_IS_INVALID)
        BigDecimal price,

        @Min(value = 0, message = PRODUCT_QUANTITY_IS_INVALID)
        Integer quantity
) {}
