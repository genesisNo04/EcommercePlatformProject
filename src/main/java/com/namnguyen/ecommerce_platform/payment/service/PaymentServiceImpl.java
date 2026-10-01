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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.namnguyen.ecommerce_platform.payment.error.PaymentErrorMessages.*;

@Slf4j
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
        log.debug(
                "Fetching payment orderId={} userId={}",
                orderId,
                userId
        );

        Order order = orderLookupService.getOrderByIdAndUserId(orderId, userId);
        Payment payment = paymentLookupService.getPaymentByOrderId(order.getId());

        return PaymentMapper.toResponse(payment);
    }

    @Override
    @Transactional
    public PaymentResponse updatePayment(Long orderId, Long userId, PaymentRequest request) {
        log.info(
                "Updating payment orderId={} userId={}",
                orderId,
                userId
        );
        Order order = orderLookupService.getOrderByIdAndUserId(orderId, userId);
        Payment payment = paymentLookupService.getPaymentByOrderId(order.getId());

        if (payment.getPaymentStatus() != PaymentStatus.PENDING) {
            throw new InvalidPaymentStateException(PAYMENT_NOT_PENDING);
        }

        payment.setPaymentMethod(request.paymentMethod());

        log.info(
                "Payment updated paymentId={}",
                payment.getId()
        );
        return PaymentMapper.toResponse(payment);
    }

    @Override
    @Transactional
    public PaymentResponse submitPayment(Long orderId, Long userId, PaymentRequest paymentRequest) {
        log.info(
                "Submitting payment orderId={} userId={}",
                orderId,
                userId
        );

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

        Payment savedPayment = paymentRepository.save(payment);

        log.info(
                "Payment submitted paymentId={}",
                savedPayment.getId()
        );

        return PaymentMapper.toResponse(savedPayment);
    }

    @Override
    @Transactional
    public PaymentResponse confirmPayment(Long orderId, Long userId, PaymentStatus status) {
        log.info(
                "Confirming payment orderId={} userId={} status={}",
                orderId,
                userId,
                status
        );

        Order order = orderLookupService.getOrderByIdAndUserId(orderId, userId);
        Payment payment = paymentLookupService.getPaymentByOrderId(order.getId());

        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new InvalidOrderStateException(ORDER_NOT_PENDING_PAYMENT);
        }

        if (payment.getPaymentStatus() != PaymentStatus.PENDING) {
            throw new InvalidPaymentStateException(PAYMENT_CANNOT_BE_CONFIRMED);
        }

        if (status == PaymentStatus.SUCCESS) {
            payment.setPaymentStatus(PaymentStatus.SUCCESS);
            order.setStatus(OrderStatus.PAID);
        } else if (status == PaymentStatus.FAILED) {
            payment.setPaymentStatus(PaymentStatus.FAILED);
        } else {
            throw new InvalidPaymentStateException(INVALID_PAYMENT_STATUS);
        }

        log.info(
                "Payment status updated paymentId={} status={}",
                payment.getId(),
                payment.getPaymentStatus()
        );

        return PaymentMapper.toResponse(payment);
    }
}
