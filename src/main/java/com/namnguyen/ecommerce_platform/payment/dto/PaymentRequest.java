package com.namnguyen.ecommerce_platform.payment.dto;

import com.namnguyen.ecommerce_platform.payment.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;

public record PaymentRequest(
        @NotNull
        PaymentMethod paymentMethod
) {}
