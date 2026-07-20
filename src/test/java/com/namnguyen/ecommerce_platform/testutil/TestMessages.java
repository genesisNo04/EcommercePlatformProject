package com.namnguyen.ecommerce_platform.testutil;

public final class TestMessages {

    private static final String NO_RESOURCE_FOUND_EXCEPTION_USER_MESSAGE = "User not found with id: ";
    private static final String NO_RESOURCE_FOUND_EXCEPTION_CART_MESSAGE = "Cart not found for user id: ";
    private static final String NO_RESOURCE_FOUND_EXCEPTION_PRODUCT_MESSAGE = "Product not found with id: ";
    private static final String NO_RESOURCE_FOUND_EXCEPTION_ITEM_MESSAGE = "No item found with product id: ";
    private static final String INSUFFICIENT_STOCK_EXCEPTION_MESSAGE = "Not enough stock for product: ";
    private static final String DUPLICATE_RESOURCE_EXCEPTION_EMAIL_MESSAGE = "Email already exists";
    private static final String DUPLICATE_RESOURCE_EXCEPTION_PHONE_NUMBER_MESSAGE = "Phone number already exists";
    private static final String ORDER_ITEM_QUANTITY_LARGER_THAN_ZERO_MESSAGE = "Order item quantity must be greater than zero";
    private static final String INVALID_ORDER_NO_ITEM_MESSAGE = "Order must contain at least one item";

    private TestMessages() {}

    public static String userNotFound(Long userId) {
        return NO_RESOURCE_FOUND_EXCEPTION_USER_MESSAGE + userId;
    }

    public static String cartNotFound(Long userId) {
        return NO_RESOURCE_FOUND_EXCEPTION_CART_MESSAGE + userId;
    }

    public static String productNotFound(Long productId) {
        return NO_RESOURCE_FOUND_EXCEPTION_PRODUCT_MESSAGE + productId;
    }

    public static String cartItemNotFound(Long productId) {
        return NO_RESOURCE_FOUND_EXCEPTION_ITEM_MESSAGE + productId;
    }

    public static String insufficientStock(String productName) {
        return INSUFFICIENT_STOCK_EXCEPTION_MESSAGE + productName;
    }

    public static String duplicateEmail() {
        return DUPLICATE_RESOURCE_EXCEPTION_EMAIL_MESSAGE;
    }

    public static String duplicatePhoneNumber() {
        return DUPLICATE_RESOURCE_EXCEPTION_PHONE_NUMBER_MESSAGE;
    }

    public static String orderItemGreaterThanZero() {
        return ORDER_ITEM_QUANTITY_LARGER_THAN_ZERO_MESSAGE;
    }

    public static String orderHasAtLeastOneItem() {
        return INVALID_ORDER_NO_ITEM_MESSAGE;
    }

}
