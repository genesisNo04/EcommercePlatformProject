package com.namnguyen.ecommerce_platform.payment.dto;

import com.namnguyen.ecommerce_platform.payment.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;

import static com.namnguyen.ecommerce_platform.payment.error.PaymentErrorMessages.PAYMENT_METHOD_IS_REQUIRED;

public record PaymentRequest(

        @NotNull(message = PAYMENT_METHOD_IS_REQUIRED)
        PaymentMethod paymentMethod
) {}
