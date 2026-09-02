package com.namnguyen.ecommerce_platform.testutil.messages;

public final class ProductTestMessages {

    public static final String PRODUCT_NAME_IS_REQUIRED =
            "Product name is required.";

    public static final String PRODUCT_NAME_IS_EMPTY =
            "Product name cannot be empty.";

    public static final String PRODUCT_NAME_IS_INVALID =
            "Product name must not exceed 100 characters.";

    public static final String PRODUCT_DESCRIPTION_IS_REQUIRED =
            "Product description is required.";

    public static final String PRODUCT_DESCRIPTION_IS_EMPTY =
            "Product description cannot be empty.";

    public static final String PRODUCT_DESCRIPTION_IS_INVALID =
            "Product description must be between 5 and 1000 characters.";

    public static final String PRODUCT_PRICE_IS_REQUIRED =
            "Product price is required.";

    public static final String PRODUCT_PRICE_IS_INVALID =
            "Product price must be at least 0.01.";

    public static final String PRODUCT_QUANTITY_IS_REQUIRED =
            "Product quantity is required.";

    public static final String PRODUCT_QUANTITY_IS_INVALID =
            "Product quantity must be greater than or equal to 0.";

    private ProductTestMessages() {
    }

    public static String productNotFoundWithId(Long productId) {
        return "Product not found with id: " + productId + ".";
    }

    public static String productNotFoundWithName(String productName) {
        return "Product not found with name: " + productName + ".";
    }

    public static String insufficientStockForProduct(String productName) {
        return  "Insufficient stock for product: " + productName + ".";
    }
}
