package com.namnguyen.ecommerce_platform.testutil.messages;

public final class PaymentTestMessages {

    public static final String PAYMENT_ALREADY_EXISTS =
            "A duplicate transaction has been submitted.";

    public static final String PAYMENT_NOT_PENDING =
            "Only pending payments can be modified.";

    public static final String ORDER_NOT_PENDING_PAYMENT =
            "Payment cannot be submitted because this order is no longer pending.";

    public static final String INVALID_PAYMENT_STATUS =
            "Payment can only be confirmed as SUCCESS or FAILED.";

    public static final String PAYMENT_METHOD_IS_REQUIRED =
            "Payment method is required.";

    private PaymentTestMessages() {}

    public static String paymentNotFoundWithOrderId(Long orderId) {
        return "No payment found for order with id: " + orderId;
    }
}
