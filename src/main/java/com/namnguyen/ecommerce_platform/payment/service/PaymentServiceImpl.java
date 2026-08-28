package com.namnguyen.ecommerce_platform.payment.service;

import com.namnguyen.ecommerce_platform.common.exception.DuplicateResourceException;
import com.namnguyen.ecommerce_platform.order.exception.InvalidOrderStateException;
import com.namnguyen.ecommerce_platform.payment.exception.InvalidPaymentStateException;
import com.namnguyen.ecommerce_platform.order.entity.Order;
import com.namnguyen.ecommerce_platform.order.enums.OrderStatus;
import com.namnguyen.ecommerce_platform.order.service.OrderLookupService;
import com.namnguyen.ecommerce_platform.payment.dto.PaymentRequest;
import com.namnguyen.ecommerce_platform.payment.dto.PaymentResponse;
import com.namnguyen.ecommerce_platform.payment.entity.Payment;
import com.namnguyen.ecommerce_platform.payment.enums.PaymentStatus;
import com.namnguyen.ecommerce_platform.payment.mapper.PaymentMapper;
import com.namnguyen.ecommerce_platform.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.namnguyen.ecommerce_platform.payment.error.PaymentErrorMessages.*;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderLookupService orderLookupService;
    private final PaymentLookupService paymentLookupService;

    private void validatePaymentDoesNotExist(Long orderId) {
        if (paymentRepository.existsByOrderId(orderId)) {
            throw new DuplicateResourceException(PAYMENT_ALREADY_EXISTS);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByOrderId(Long orderId, Long userId) {
        Order order = orderLookupService.getOrderByIdAndUserId(orderId, userId);
        Payment payment = paymentLookupService.getPaymentByOrderId(order.getId());
        return PaymentMapper.toResponse(payment);
    }

    @Override
    @Transactional
    public PaymentResponse updatePayment(Long orderId, Long userId, PaymentRequest request) {
        Order order = orderLookupService.getOrderByIdAndUserId(orderId, userId);
        Payment payment = paymentLookupService.getPaymentByOrderId(order.getId());

        if (payment.getPaymentStatus() != PaymentStatus.PENDING) {
            throw new InvalidPaymentStateException(PAYMENT_NOT_PENDING);
        }

        payment.setPaymentMethod(request.paymentMethod());
        return PaymentMapper.toResponse(payment);
    }

    @Override
    @Transactional
    public PaymentResponse submitPayment(Long orderId, Long userId, PaymentRequest paymentRequest) {
        Order order = orderLookupService.getOrderByIdAndUserId(orderId, userId);

        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new InvalidOrderStateException(ORDER_NOT_PENDING_PAYMENT);
        }

        validatePaymentDoesNotExist(orderId);

        Payment payment = Payment.builder()
                .paymentStatus(PaymentStatus.PENDING)
                .paymentMethod(paymentRequest.paymentMethod())
                .order(order)
                .amount(order.getTotal())
                .build();

        return PaymentMapper.toResponse(paymentRepository.save(payment));
    }

    @Override
    @Transactional
    public PaymentResponse confirmPayment(Long orderId, Long userId, PaymentStatus status) {
        Order order = orderLookupService.getOrderByIdAndUserId(orderId, userId);
        Payment payment = paymentLookupService.getPaymentByOrderId(order.getId());

        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new InvalidOrderStateException(ORDER_NOT_PENDING_PAYMENT);
        }

        if (payment.getPaymentStatus() != PaymentStatus.PENDING) {
            throw new InvalidPaymentStateException(PAYMENT_NOT_PENDING);
        }

        if (status == PaymentStatus.SUCCESS) {
            payment.setPaymentStatus(PaymentStatus.SUCCESS);
            order.setStatus(OrderStatus.PAID);
        } else if (status == PaymentStatus.FAILED) {
            payment.setPaymentStatus(PaymentStatus.FAILED);
        } else {
            throw new InvalidPaymentStateException(INVALID_PAYMENT_STATUS);
        }

        return PaymentMapper.toResponse(payment);
    }
}
