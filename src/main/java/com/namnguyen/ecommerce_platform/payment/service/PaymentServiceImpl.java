package com.namnguyen.ecommerce_platform.payment.service;

import com.namnguyen.ecommerce_platform.common.exception.DuplicateResourceException;
import com.namnguyen.ecommerce_platform.common.exception.InvalidOrderStateException;
import com.namnguyen.ecommerce_platform.common.exception.InvalidPaymentStateException;
import com.namnguyen.ecommerce_platform.common.exception.NoResourceFoundException;
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

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService{

    private final PaymentRepository paymentRepository;
    private final OrderLookupService orderLookupService;

    private void paymentExist(Long orderId) {
        if (paymentRepository.existsByOrderId(orderId)) {
            throw new DuplicateResourceException("This order already have a payment");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByOrderId(Long orderId, Long userId) {
        Order order = orderLookupService.getOrderByIdAndUserId(orderId, userId);
        Payment payment = paymentRepository.findByOrderId(order.getId())
                .orElseThrow(() -> new NoResourceFoundException("No payment found for order with id:" + order.getId()));
        return PaymentMapper.toResponse(payment);
    }

    @Override
    @Transactional
    public PaymentResponse updatePayment(Long orderId, Long userId, PaymentRequest request) {
        Order order = orderLookupService.getOrderByIdAndUserId(orderId, userId);
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new NoResourceFoundException("No payment found for order with id:" + order.getId()));

        if (payment.getPaymentStatus() != PaymentStatus.SUCCESS) {
            throw new InvalidPaymentStateException("Cannot update a success payment");
        }

        payment.setPaymentMethod(request.paymentMethod());
        return PaymentMapper.toResponse(payment);
    }

    @Override
    @Transactional
    public PaymentResponse submitPayment(Long orderId, Long userId, PaymentRequest paymentRequest) {
        Order order = orderLookupService.getOrderByIdAndUserId(orderId, userId);

        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new InvalidOrderStateException("Order is not pending payment");
        }

        paymentExist(orderId);

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
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new NoResourceFoundException("No payment found for order with id:" + order.getId()));

        if (payment.getPaymentStatus() != PaymentStatus.PENDING) {
            throw new InvalidPaymentStateException("Only Pending payments can be confirmed");
        }

        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new InvalidOrderStateException("Order is not in pending payment");
        }

        if (status == PaymentStatus.SUCCESS) {
            payment.setPaymentStatus(PaymentStatus.SUCCESS);
            order.setStatus(OrderStatus.PAID);
        } else if (status == PaymentStatus.FAILED) {
            payment.setPaymentStatus(PaymentStatus.FAILED);
            order.setStatus(OrderStatus.PENDING_PAYMENT);
        } else {
            throw new InvalidPaymentStateException("Payment can only be confirmed as SUCCESS OR FAILED");
        }

        return PaymentMapper.toResponse(payment);
    }
}
