package com.namnguyen.ecommerce_platform.payment.service;

import com.namnguyen.ecommerce_platform.common.exception.NoResourceFoundException;
import com.namnguyen.ecommerce_platform.payment.entity.Payment;
import com.namnguyen.ecommerce_platform.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import static com.namnguyen.ecommerce_platform.payment.error.PaymentErrorMessages.paymentNotFoundWithOrderId;

@Service
@RequiredArgsConstructor
public class PaymentLookupService {

    private final PaymentRepository paymentRepository;

    public Payment getPaymentByOrderId(Long orderId) {
        return paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new NoResourceFoundException(paymentNotFoundWithOrderId(orderId)));
    }
}
