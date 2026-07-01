package com.namnguyen.ecommerce_platform.payment.service;

import com.namnguyen.ecommerce_platform.order.enums.OrderStatus;
import com.namnguyen.ecommerce_platform.payment.dto.PaymentRequest;
import com.namnguyen.ecommerce_platform.payment.dto.PaymentResponse;
import com.namnguyen.ecommerce_platform.payment.enums.PaymentStatus;

public interface PaymentService {

    PaymentResponse getPaymentByOrderId(Long orderId, Long userId);

    PaymentResponse updatePayment(Long orderId, Long userId, PaymentRequest request);

    PaymentResponse submitPayment(Long orderId, Long userId, PaymentRequest paymentRequest);

    PaymentResponse confirmPayment(Long orderId, Long userId, PaymentStatus status);
}
