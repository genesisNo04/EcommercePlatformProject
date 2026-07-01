package com.namnguyen.ecommerce_platform.payment.service;

import com.namnguyen.ecommerce_platform.common.exception.NoResourceFoundException;
import com.namnguyen.ecommerce_platform.payment.entity.Payment;
import com.namnguyen.ecommerce_platform.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentLookupService {

    private final PaymentRepository paymentRepository;

    public Payment getPaymentById(Long orderId) {
        return paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new NoResourceFoundException("No payment found for order with id:" + orderId));
    }
}
