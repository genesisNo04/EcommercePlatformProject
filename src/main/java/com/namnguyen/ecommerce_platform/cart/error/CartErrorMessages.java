package com.namnguyen.ecommerce_platform.cart.error;

public final class CartErrorMessages {

    public static final String CART_ITEM_PRODUCT_ID_IS_REQUIRED =
            "Product id is required.";

    public static final String CART_ITEM_PRODUCT_ID_IS_INVALID =
            "Product id must be greater than 0.";

    public static final String EMPTY_CART =
            "Cannot checkout an empty cart.";

    public static final String CART_ITEM_QUANTITY_IS_REQUIRED =
            "Cart item quantity is required.";

    public static final String CART_ITEM_QUANTITY_IS_INVALID =
            "Cart item quantity must be at least 1.";

    public static final String CART_ITEM_UPDATE_QUANTITY_IS_INVALID =
            "Cart item update quantity must be greater than or equal to 0.";

    private CartErrorMessages() {}

    public static String cartNotFoundWithUserId(Long userId) {
        return "No cart for user with id: " + userId;
    }

    public static String cartItemNotFoundWithProductId(Long productId) {
        return "No cart item found with product id: " + productId;
    }
}
