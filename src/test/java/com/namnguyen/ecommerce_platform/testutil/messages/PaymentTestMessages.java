package com.namnguyen.ecommerce_platform.testutil.messages;

public final class PaymentTestMessages {

    public static final String PAYMENT_ALREADY_EXISTS =
            "A duplicate transaction has been submitted.";

    public static final String PAYMENT_NOT_PENDING =
            "Only pending payments can be modified.";

    public static final String PAYMENT_CANNOT_BE_CONFIRMED =
            "Only pending payments can be confirmed.";

    public static final String ORDER_NOT_PENDING_PAYMENT =
            "Payment cannot be submitted because this order is no longer pending.";

    public static final String INVALID_PAYMENT_STATUS =
            "Payment can only be confirmed as SUCCESS or FAILED.";

    public static final String PAYMENT_METHOD_IS_REQUIRED =
            "Payment method is required.";


    private PaymentTestMessages() {}

    public static String paymentNotFoundWithOrderId(Long orderId) {
        return "No payment found for order with id: " + orderId + ".";
    }

    public static String paymentMethodIsInvalid(String method) {
        return "Invalid value '%s' for parameter 'paymentMethod'. Allowed values: [CARD, PAYPAL, BANK_TRANSFER].".formatted(method);
    }

    public static String paymentStatusIsInvalid(String status) {
        return "Invalid value '%s' for parameter 'paymentStatus'. Allowed values: [PENDING, SUCCESS, FAILED].".formatted(status);
    }

    public static String orderNotFoundWithIdAndUserId(Long orderId, Long userId) {
        return  "No order found with id: " + orderId +
                " for user id: " + userId + ".";
    }
}
