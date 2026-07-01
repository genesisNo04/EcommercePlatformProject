package com.namnguyen.ecommerce_platform.payment.mapper;

import com.namnguyen.ecommerce_platform.payment.dto.PaymentResponse;
import com.namnguyen.ecommerce_platform.payment.entity.Payment;

public class PaymentMapper {

    public static PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getOrder().getId(),
                payment.getAmount(),
                payment.getPaymentMethod(),
                payment.getPaymentStatus(),
                payment.getCreatedAt(),
                payment.getUpdatedAt()
        );
    }
}
