package com.namnguyen.ecommerce_platform.testutil;

public final class TestMessages {

    private static final String NO_RESOURCE_FOUND_EXCEPTION_USER_MESSAGE = "User not found with id: ";
    private static final String NO_RESOURCE_FOUND_EXCEPTION_CART_MESSAGE = "Cart not found for user id: ";
    private static final String NO_RESOURCE_FOUND_EXCEPTION_PRODUCT_MESSAGE = "Product not found with id: ";
    private static final String NO_RESOURCE_FOUND_EXCEPTION_ITEM_MESSAGE = "No item found with product id: ";
    private static final String NO_RESOURCE_FOUND_EXCEPTION_ORDER_MESSAGE = "No order with id: %s found for this user id: %s";
    private static final String NO_RESOURCE_FOUND_EXCEPTION_PAYMENT_MESSAGE = "No payment found for order with id: %s";
    private static final String INSUFFICIENT_STOCK_EXCEPTION_MESSAGE = "Not enough stock for product: ";
    private static final String DUPLICATE_RESOURCE_EXCEPTION_EMAIL_MESSAGE = "Email already exists";
    private static final String DUPLICATE_RESOURCE_EXCEPTION_PHONE_NUMBER_MESSAGE = "Phone number already exists";
    private static final String DUPLICATE_RESOURCE_EXCEPTION_PAYMENT_EXISTS_MESSAGE = "This order already has a payment";
    private static final String ORDER_ITEM_QUANTITY_LARGER_THAN_ZERO_MESSAGE = "Order item quantity must be greater than zero";
    private static final String INVALID_ORDER_NO_ITEM_MESSAGE = "Order must contain at least one item";
    private static final String INVALID_ORDER_STATE_DELIVERED_ORDER_MESSAGE = "Delivered order cannot be cancelled";
    private static final String INVALID_ORDER_STATE_CANCELLED_MESSAGE = "Order is already cancelled";
    private static final String INVALID_ORDER_STATE_EMPTY_CART_MESSAGE = "Cannot checkout an empty cart";
    private static final String INVALID_ORDER_STATE_NOT_PENDING_PAYMENT_MESSAGE = "Order is not pending payment";
    private static final String INVALID_PAYMENT_STATE_NOT_PENDING_PAYMENT_MESSAGE = "Only pending payments can be updated";
    private static final String INVALID_PAYMENT_STATE_NOT_PENDING_PAYMENT_CONFIRM_MESSAGE = "Only pending payments can be confirmed";
    private static final String INVALID_PAYMENT_STATUS_CONFIRM_MESSAGE = "Payment can only be confirmed as SUCCESS OR FAILED";
    private static final String VALIDATION_FAILED_MESSAGE = "Validation failed";
    private static final String BAD_CREDENTIAL_MESSAGE = "Bad credentials";
    private static final String EMAIL_IS_REQUIRED_MESSAGE = "Email is required";
    private static final String EMAIL_IS_EMPTY_MESSAGE = "Email cannot be empty";
    private static final String EMAIL_INVALID_MESSAGE = "Invalid Email format";
    private static final String PASSWORD_IS_REQUIRED_MESSAGE = "Password is required";
    private static final String PASSWORD_IS_EMPTY_MESSAGE = "Password cannot be empty";
    private static final String PASSWORD_LENGTH_MESSAGE = "Password has to be from 8 to 50 chars";
    private static final String FIRST_NAME_IS_REQUIRED_MESSAGE = "First Name is required";
    private static final String FIRST_NAME_IS_EMPTY_MESSAGE = "First Name cannot be empty";
    private static final String LAST_NAME_IS_REQUIRED_MESSAGE = "Last Name is required";
    private static final String LAST_NAME_IS_EMPTY_MESSAGE = "Last Name cannot be empty";
    private static final String PHONE_NUMBER_IS_REQUIRED_MESSAGE = "Phone Number is required";
    private static final String PHONE_NUMBER_INVALID_MESSAGE = "Phone number must be from 10 to 15 digits (with or without +)";
    private static final String EMAIL_DUPLICATE_MESSAGE = "Email already exists";
    private static final String PHONE_DUPLICATE_MESSAGE = "Phone number already exists";
    private static final String INVALID_PARAMETER = "Invalid parameter: %s";


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

    public static String orderNotFound(Long orderId, Long userId) {
        return String.format(NO_RESOURCE_FOUND_EXCEPTION_ORDER_MESSAGE, orderId, userId);
    }

    public static String paymentNotFound(Long orderId) {
        return String.format(NO_RESOURCE_FOUND_EXCEPTION_PAYMENT_MESSAGE, orderId);
    }

    public static String cannotCancelDeliveredOrder() {
        return INVALID_ORDER_STATE_DELIVERED_ORDER_MESSAGE;
    }

    public static String orderAlreadyCancelled() {
        return INVALID_ORDER_STATE_CANCELLED_MESSAGE;
    }

    public static String emptyCart() {
        return INVALID_ORDER_STATE_EMPTY_CART_MESSAGE;
    }

    public static String orderNotInPendingPayment() { return INVALID_ORDER_STATE_NOT_PENDING_PAYMENT_MESSAGE; }

    public static String paymentDuplicate() { return DUPLICATE_RESOURCE_EXCEPTION_PAYMENT_EXISTS_MESSAGE; }

    public static String paymentNotPending() { return INVALID_PAYMENT_STATE_NOT_PENDING_PAYMENT_MESSAGE; }

    public static String paymentCannotConfirmed() { return INVALID_PAYMENT_STATE_NOT_PENDING_PAYMENT_CONFIRM_MESSAGE; }

    public static String invalidStatusConfirmed() { return INVALID_PAYMENT_STATUS_CONFIRM_MESSAGE; }

    public static String validationFailed() { return VALIDATION_FAILED_MESSAGE; }

    public static String badCredentials() { return BAD_CREDENTIAL_MESSAGE; }

    public static String emailIsRequired() {
        return EMAIL_IS_REQUIRED_MESSAGE;
    }

    public static String emailIsInvalid() {
        return EMAIL_INVALID_MESSAGE;
    }

    public static String emailIsEmpty() {
        return EMAIL_IS_EMPTY_MESSAGE;
    }

    public static String passwordIsRequired() {
        return PASSWORD_IS_REQUIRED_MESSAGE;
    }

    public static String passwordIsInvalid() {
        return PASSWORD_LENGTH_MESSAGE;
    }

    public static String passwordIsEmpty() {
        return PASSWORD_IS_EMPTY_MESSAGE;
    }

    public static String firstNameIsRequired() { return FIRST_NAME_IS_REQUIRED_MESSAGE; }

    public static String lastNameIsRequired() {
        return LAST_NAME_IS_REQUIRED_MESSAGE;
    }

    public static String firstNameIsEmpty() { return FIRST_NAME_IS_EMPTY_MESSAGE; }

    public static String lastNameIsEmpty() {
        return LAST_NAME_IS_EMPTY_MESSAGE;
    }

    public static String phoneNumberIsRequired() {
        return PHONE_NUMBER_IS_REQUIRED_MESSAGE;
    }

    public static String phoneNumberIsInvalid() {
        return PHONE_NUMBER_INVALID_MESSAGE;
    }

    public static String emailDuplicate() {
        return EMAIL_DUPLICATE_MESSAGE;
    }

    public static String phoneDuplicate() {
        return PHONE_DUPLICATE_MESSAGE;
    }

    public static String invalidParameter(String paramName) {
        return String.format(INVALID_PARAMETER, paramName);
    }
}
