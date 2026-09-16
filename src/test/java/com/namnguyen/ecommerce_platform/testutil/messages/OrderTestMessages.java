package com.namnguyen.ecommerce_platform.testutil.messages;

public final class OrderTestMessages {

    public static final String ORDER_ITEM_PRODUCT_ID_IS_REQUIRED =
            "Product id is required.";

    public static final String ORDER_ITEM_PRODUCT_ID_IS_INVALID =
            "Product id must be greater than 0.";

    public static final String ORDER_ITEM_QUANTITY_IS_REQUIRED =
            "Order item quantity is required.";

    public static final String ORDER_ITEM_QUANTITY_IS_INVALID =
            "Order item quantity must be at least 1.";

    public static final String DELIVERED_ORDER_CANNOT_BE_CANCELLED =
            "Delivered order cannot be cancelled.";

    public static final String ORDER_ALREADY_CANCELLED =
            "Order is already cancelled.";

    public static final String ORDER_CANNOT_BE_CANCELLED =
            "Order cannot be cancelled.";

    public static final String ORDER_IS_EMPTY =
            "Order must contain at least one item.";

    public static final String EMPTY_CART =
            "Cannot checkout an empty cart.";

    private OrderTestMessages() {}

    public static String orderNotFoundWithId(Long orderId) {
        return "No order found with id: " + orderId + ".";
    }

    public static String insufficientStock(String productName) {
        return "Insufficient stock for product: " + productName + ".";
    }

    public static String orderNotFoundWithIdAndUserId(Long orderId, Long userId) {
        return  "No order found with id: " + orderId +
                " for user id: " + userId + ".";
    }

    public static String productNotFoundWithId(Long productId) {
        return "Product not found with id: " + productId + ".";
    }

    public static String userNotFoundWithId(Long userId) {
        return "User not found with id: " + userId + ".";
    }

    public static String cartNotFoundWithUserId(Long userId) {
        return "No cart for user with id: " + userId + ".";
    }
}
