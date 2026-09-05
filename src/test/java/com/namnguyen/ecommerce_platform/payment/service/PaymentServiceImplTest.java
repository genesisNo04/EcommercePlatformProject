package com.namnguyen.ecommerce_platform.payment.service;

import com.namnguyen.ecommerce_platform.common.exception.DuplicateResourceException;
import com.namnguyen.ecommerce_platform.order.exception.InvalidOrderStateException;
import com.namnguyen.ecommerce_platform.payment.exception.InvalidPaymentStateException;
import com.namnguyen.ecommerce_platform.common.exception.NoResourceFoundException;
import com.namnguyen.ecommerce_platform.order.entity.Order;
import com.namnguyen.ecommerce_platform.order.enums.OrderStatus;
import com.namnguyen.ecommerce_platform.order.service.OrderLookupService;
import com.namnguyen.ecommerce_platform.payment.dto.PaymentRequest;
import com.namnguyen.ecommerce_platform.payment.dto.PaymentResponse;
import com.namnguyen.ecommerce_platform.payment.entity.Payment;
import com.namnguyen.ecommerce_platform.payment.enums.PaymentMethod;
import com.namnguyen.ecommerce_platform.payment.enums.PaymentStatus;
import com.namnguyen.ecommerce_platform.payment.repository.PaymentRepository;
import com.namnguyen.ecommerce_platform.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static com.namnguyen.ecommerce_platform.testutil.TestDataFactory.*;
import static com.namnguyen.ecommerce_platform.testutil.messages.PaymentTestMessages.*;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PaymentServiceImplTest {

    @Mock
    private OrderLookupService orderLookupService;

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    @Test
    void submitPayment_whenOrderExists_returnsPaymentResponse() {
        Long orderId = 1L;
        Long userId = 2L;
        Long paymentId = 3L;
        BigDecimal total = BigDecimal.valueOf(500);
        PaymentMethod paymentMethod = PaymentMethod.CARD;
        PaymentStatus paymentStatus = PaymentStatus.PENDING;

        User user = createUser(userId);
        Order order = createOrder(
                orderId,
                total,
                OrderStatus.PENDING_PAYMENT,
                user
        );

        PaymentRequest paymentRequest = new PaymentRequest(paymentMethod);

        when(orderLookupService.getOrderByIdAndUserId(orderId, userId)).thenReturn(order);
        when(paymentRepository.existsByOrderId(orderId)).thenReturn(false);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment payment = inv.getArgument(0);
            payment.setId(paymentId);
            return payment;
        });

        PaymentResponse paymentResponse = paymentService.submitPayment(orderId, userId, paymentRequest);

        assertThat(paymentResponse).isNotNull();
        assertThat(paymentResponse.paymentId()).isEqualTo(paymentId);
        assertThat(paymentResponse.orderId()).isEqualTo(orderId);
        assertThat(paymentResponse.amount()).isEqualByComparingTo(total);
        assertThat(paymentResponse.paymentMethod()).isEqualTo(paymentMethod);
        assertThat(paymentResponse.paymentStatus()).isEqualTo(paymentStatus);

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(paymentCaptor.capture());

        Payment savedPayment = paymentCaptor.getValue();
        assertThat(savedPayment.getId()).isEqualTo(paymentId);
        assertThat(savedPayment.getOrder()).isEqualTo(order);
        assertThat(savedPayment.getAmount()).isEqualByComparingTo(total);
        assertThat(savedPayment.getPaymentMethod()).isEqualTo(paymentMethod);
        assertThat(savedPayment.getPaymentStatus()).isEqualTo(paymentStatus);

        verify(orderLookupService).getOrderByIdAndUserId(orderId, userId);
        verify(paymentRepository).existsByOrderId(orderId);
        verifyNoMoreInteractions(orderLookupService);
        verifyNoMoreInteractions(paymentRepository);
    }

    @Test
    void submitPayment_whenOrderDoesNotExist_throwsNoResourceFoundException() {
        Long orderId = 1L;
        Long userId = 2L;
        PaymentMethod paymentMethod = PaymentMethod.CARD;

        PaymentRequest paymentRequest = new PaymentRequest(paymentMethod);

        when(orderLookupService.getOrderByIdAndUserId(orderId, userId))
                .thenThrow(new NoResourceFoundException(orderNotFoundWithIdAndUserId(orderId, userId)));

        NoResourceFoundException ex = assertThrows(
                NoResourceFoundException.class,
                () -> paymentService.submitPayment(orderId, userId, paymentRequest)
        );

        assertThat(ex).isNotNull();
        assertThat(ex.getMessage()).isEqualTo(orderNotFoundWithIdAndUserId(orderId, userId));

        verify(orderLookupService).getOrderByIdAndUserId(orderId, userId);
        verifyNoMoreInteractions(orderLookupService);
        verifyNoInteractions(paymentRepository);
    }

    @Test
    void submitPayment_whenOrderIsNotPendingPayment_throwsInvalidOrderStateException() {
        Long orderId = 1L;
        Long userId = 2L;
        BigDecimal total = BigDecimal.valueOf(500);
        PaymentMethod paymentMethod = PaymentMethod.CARD;

        User user = createUser(userId);
        Order order = createOrder(
                orderId,
                total,
                OrderStatus.PAID,
                user
        );

        PaymentRequest paymentRequest = new PaymentRequest(paymentMethod);

        when(orderLookupService.getOrderByIdAndUserId(orderId, userId)).thenReturn(order);

        InvalidOrderStateException ex = assertThrows(
                InvalidOrderStateException.class,
                () -> paymentService.submitPayment(orderId, userId, paymentRequest)
        );

        assertThat(ex).isNotNull();
        assertThat(ex.getMessage()).isEqualTo(ORDER_NOT_PENDING_PAYMENT);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);

        verify(orderLookupService).getOrderByIdAndUserId(orderId, userId);
        verifyNoMoreInteractions(orderLookupService);
        verifyNoInteractions(paymentRepository);
    }

    @Test
    void submitPayment_whenPaymentExistsForOrder_throwsDuplicateResourceException() {
        Long orderId = 1L;
        Long userId = 2L;
        BigDecimal total = BigDecimal.valueOf(500);
        PaymentMethod paymentMethod = PaymentMethod.CARD;
        OrderStatus orderStatus = OrderStatus.PENDING_PAYMENT;

        User user = createUser(userId);
        Order order = createOrder(
                orderId,
                total,
                orderStatus,
                user
        );

        PaymentRequest paymentRequest = new PaymentRequest(paymentMethod);

        when(orderLookupService.getOrderByIdAndUserId(orderId, userId)).thenReturn(order);
        when(paymentRepository.existsByOrderId(orderId)).thenReturn(true);

        DuplicateResourceException ex = assertThrows(
                DuplicateResourceException.class,
                () -> paymentService.submitPayment(orderId, userId, paymentRequest)
        );

        assertThat(ex).isNotNull();
        assertThat(ex.getMessage()).isEqualTo(PAYMENT_ALREADY_EXISTS);
        assertThat(order.getStatus()).isEqualTo(orderStatus);

        verify(orderLookupService).getOrderByIdAndUserId(orderId, userId);
        verify(paymentRepository).existsByOrderId(orderId);
        verifyNoMoreInteractions(orderLookupService);
        verifyNoMoreInteractions(paymentRepository);
    }

    @Test
    void getPaymentByOrderId_whenPaymentExists_returnsPaymentResponse() {
        Long paymentId = 1L;
        Long userId = 2L;
        Long orderId = 3L;
        BigDecimal total = BigDecimal.valueOf(500);
        OrderStatus orderStatus = OrderStatus.PENDING_PAYMENT;
        PaymentStatus paymentStatus = PaymentStatus.PENDING;
        PaymentMethod paymentMethod = PaymentMethod.CARD;

        User user = createUser(userId);
        Order order = createOrder(
                orderId,
                total,
                orderStatus,
                user
        );
        Payment payment = createPayment(
                paymentId,
                paymentMethod,
                paymentStatus,
                order,
                total
        );

        when(orderLookupService.getOrderByIdAndUserId(orderId, userId)).thenReturn(order);
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(payment));

        PaymentResponse paymentResponse = paymentService.getPaymentByOrderId(orderId, userId);

        assertThat(paymentResponse).isNotNull();
        assertThat(paymentResponse.paymentId()).isEqualTo(paymentId);
        assertThat(paymentResponse.orderId()).isEqualTo(orderId);
        assertThat(paymentResponse.amount()).isEqualByComparingTo(total);
        assertThat(paymentResponse.paymentStatus()).isEqualTo(paymentStatus);
        assertThat(paymentResponse.paymentMethod()).isEqualTo(paymentMethod);

        verify(orderLookupService).getOrderByIdAndUserId(orderId, userId);
        verify(paymentRepository).findByOrderId(orderId);
        verifyNoMoreInteractions(orderLookupService);
        verifyNoMoreInteractions(paymentRepository);
    }

    @Test
    void getPaymentByOrderId_whenOrderDoesNotExist_throwsNoResourceFoundException() {
        Long userId = 2L;
        Long orderId = 3L;

        when(orderLookupService.getOrderByIdAndUserId(orderId, userId))
                .thenThrow(new NoResourceFoundException(orderNotFoundWithIdAndUserId(orderId, userId)));

        NoResourceFoundException ex = assertThrows(
                NoResourceFoundException.class,
                () ->  paymentService.getPaymentByOrderId(orderId, userId)
        );

        assertThat(ex).isNotNull();
        assertThat(ex.getMessage()).isEqualTo(orderNotFoundWithIdAndUserId(orderId, userId));

        verify(orderLookupService).getOrderByIdAndUserId(orderId, userId);
        verifyNoMoreInteractions(orderLookupService);
        verifyNoInteractions(paymentRepository);
    }

    @Test
    void getPaymentByOrderId_whenPaymentDoesNotExist_throwsNoResourceFoundException() {
        Long userId = 2L;
        Long orderId = 3L;
        BigDecimal total = BigDecimal.valueOf(500);
        OrderStatus orderStatus = OrderStatus.PENDING_PAYMENT;

        User user = createUser(userId);
        Order order = createOrder(
                orderId,
                total,
                orderStatus,
                user
        );

        when(orderLookupService.getOrderByIdAndUserId(orderId, userId)).thenReturn(order);
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.empty());

        NoResourceFoundException ex = assertThrows(
                NoResourceFoundException.class,
                () ->  paymentService.getPaymentByOrderId(orderId, userId)
        );

        assertThat(ex).isNotNull();
        assertThat(ex.getMessage()).isEqualTo(paymentNotFoundWithOrderId(orderId));

        verify(orderLookupService).getOrderByIdAndUserId(orderId, userId);
        verify(paymentRepository).findByOrderId(orderId);
        verifyNoMoreInteractions(orderLookupService);
        verifyNoMoreInteractions(paymentRepository);
    }

    @Test
    void updatePayment_whenPaymentIsPending_returnsPaymentResponse() {
        Long paymentId = 1L;
        Long userId = 2L;
        Long orderId = 3L;
        BigDecimal total = BigDecimal.valueOf(500);
        OrderStatus orderStatus = OrderStatus.PENDING_PAYMENT;
        PaymentStatus paymentStatus = PaymentStatus.PENDING;
        PaymentMethod paymentMethod = PaymentMethod.CARD;
        PaymentMethod updatePaymentMethod = PaymentMethod.PAYPAL;

        PaymentRequest paymentRequest = new PaymentRequest(updatePaymentMethod);

        User user = createUser(userId);
        Order order = createOrder(
                orderId,
                total,
                orderStatus,
                user
        );
        Payment payment = createPayment(
                paymentId,
                paymentMethod,
                paymentStatus,
                order,
                total
        );

        when(orderLookupService.getOrderByIdAndUserId(orderId, userId)).thenReturn(order);
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(payment));

        PaymentResponse paymentResponse = paymentService.updatePayment(orderId, userId, paymentRequest);

        assertThat(paymentResponse).isNotNull();
        assertThat(paymentResponse.paymentId()).isEqualTo(paymentId);
        assertThat(paymentResponse.orderId()).isEqualTo(orderId);
        assertThat(paymentResponse.amount()).isEqualByComparingTo(total);
        assertThat(paymentResponse.paymentMethod()).isEqualTo(updatePaymentMethod);
        assertThat(paymentResponse.paymentStatus()).isEqualTo(paymentStatus);
        assertThat(payment.getPaymentStatus()).isEqualTo(paymentStatus);
        assertThat(payment.getPaymentMethod()).isEqualTo(updatePaymentMethod);
        assertThat(order.getStatus()).isEqualTo(orderStatus);

        verify(orderLookupService).getOrderByIdAndUserId(orderId, userId);
        verify(paymentRepository).findByOrderId(orderId);
        verifyNoMoreInteractions(orderLookupService);
        verifyNoMoreInteractions(paymentRepository);
    }

    @Test
    void updatePayment_whenPaymentExistsAndNotInPending_throwsInvalidPaymentStateException() {
        Long paymentId = 1L;
        Long userId = 2L;
        Long orderId = 3L;
        BigDecimal total = BigDecimal.valueOf(500);
        OrderStatus orderStatus = OrderStatus.PENDING_PAYMENT;
        PaymentStatus paymentStatus = PaymentStatus.SUCCESS;
        PaymentMethod paymentMethod = PaymentMethod.CARD;
        PaymentMethod updatePaymentMethod = PaymentMethod.PAYPAL;

        PaymentRequest paymentRequest = new PaymentRequest(updatePaymentMethod);

        User user = createUser(userId);
        Order order = createOrder(
                orderId,
                total,
                orderStatus,
                user
        );
        Payment payment = createPayment(
                paymentId,
                paymentMethod,
                paymentStatus,
                order,
                total
        );

        when(orderLookupService.getOrderByIdAndUserId(orderId, userId)).thenReturn(order);
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(payment));

        InvalidPaymentStateException ex = assertThrows(
                InvalidPaymentStateException.class,
                () -> paymentService.updatePayment(orderId, userId, paymentRequest)
        );

        assertThat(ex).isNotNull();
        assertThat(ex.getMessage()).isEqualTo(PAYMENT_NOT_PENDING);

        assertThat(payment.getPaymentStatus()).isEqualTo(paymentStatus);
        assertThat(payment.getPaymentMethod()).isEqualTo(paymentMethod);
        assertThat(order.getStatus()).isEqualTo(orderStatus);

        verify(orderLookupService).getOrderByIdAndUserId(orderId, userId);
        verify(paymentRepository).findByOrderId(orderId);
        verifyNoMoreInteractions(orderLookupService);
        verifyNoMoreInteractions(paymentRepository);
    }

    @Test
    void updatePayment_whenOrderDoesNotExist_throwsNoResourceFoundException() {
        Long userId = 2L;
        Long orderId = 3L;
        PaymentMethod updatePaymentMethod = PaymentMethod.PAYPAL;

        PaymentRequest paymentRequest = new PaymentRequest(updatePaymentMethod);

        when(orderLookupService.getOrderByIdAndUserId(orderId, userId))
                .thenThrow(new NoResourceFoundException(orderNotFoundWithIdAndUserId(orderId, userId)));

        NoResourceFoundException ex = assertThrows(
                NoResourceFoundException.class,
                () -> paymentService.updatePayment(orderId, userId, paymentRequest)
        );

        assertThat(ex).isNotNull();
        assertThat(ex.getMessage()).isEqualTo(orderNotFoundWithIdAndUserId(orderId, userId));

        verify(orderLookupService).getOrderByIdAndUserId(orderId, userId);
        verifyNoMoreInteractions(orderLookupService);
        verifyNoInteractions(paymentRepository);
    }

    @Test
    void updatePayment_whenPaymentDoesNotExist_throwsNoResourceFoundException() {
        Long userId = 2L;
        Long orderId = 3L;
        BigDecimal total = BigDecimal.valueOf(500);
        OrderStatus orderStatus = OrderStatus.PENDING_PAYMENT;
        PaymentMethod updatePaymentMethod = PaymentMethod.PAYPAL;

        PaymentRequest paymentRequest = new PaymentRequest(updatePaymentMethod);

        User user = createUser(userId);
        Order order = createOrder(
                orderId,
                total,
                orderStatus,
                user
        );

        when(orderLookupService.getOrderByIdAndUserId(orderId, userId)).thenReturn(order);
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.empty());

        NoResourceFoundException ex = assertThrows(
                NoResourceFoundException.class,
                () -> paymentService.updatePayment(orderId, userId, paymentRequest)
        );

        assertThat(ex).isNotNull();
        assertThat(ex.getMessage()).isEqualTo(paymentNotFoundWithOrderId(orderId));

        verify(orderLookupService).getOrderByIdAndUserId(orderId, userId);
        verify(paymentRepository).findByOrderId(orderId);
        verifyNoMoreInteractions(orderLookupService);
        verifyNoMoreInteractions(paymentRepository);
    }

    @Test
    void confirmPayment_whenConfirmationSucceeds_returnsPaymentResponse() {
        Long paymentId = 1L;
        Long userId = 2L;
        Long orderId = 3L;
        BigDecimal total = BigDecimal.valueOf(500);
        OrderStatus orderStatus = OrderStatus.PENDING_PAYMENT;
        PaymentStatus paymentStatus = PaymentStatus.PENDING;
        PaymentMethod paymentMethod = PaymentMethod.CARD;
        PaymentStatus confirmationStatus = PaymentStatus.SUCCESS;

        User user = createUser(userId);
        Order order = createOrder(
                orderId,
                total,
                orderStatus,
                user
        );
        Payment payment = createPayment(
                paymentId,
                paymentMethod,
                paymentStatus,
                order,
                total
        );

        when(orderLookupService.getOrderByIdAndUserId(orderId, userId)).thenReturn(order);
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(payment));

        PaymentResponse paymentResponse = paymentService.confirmPayment(orderId, userId, confirmationStatus);

        assertThat(paymentResponse).isNotNull();
        assertThat(paymentResponse.paymentId()).isEqualTo(paymentId);
        assertThat(paymentResponse.orderId()).isEqualTo(orderId);
        assertThat(paymentResponse.amount()).isEqualByComparingTo(total);
        assertThat(paymentResponse.paymentMethod()).isEqualTo(paymentMethod);
        assertThat(paymentResponse.paymentStatus()).isEqualTo(confirmationStatus);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.SUCCESS);

        verify(orderLookupService).getOrderByIdAndUserId(orderId, userId);
        verify(paymentRepository).findByOrderId(orderId);
        verifyNoMoreInteractions(orderLookupService);
        verifyNoMoreInteractions(paymentRepository);
    }

    @Test
    void confirmPayment_whenConfirmationFails_returnsPaymentResponse() {
        Long paymentId = 1L;
        Long userId = 2L;
        Long orderId = 3L;
        BigDecimal total = BigDecimal.valueOf(500);
        OrderStatus orderStatus = OrderStatus.PENDING_PAYMENT;
        PaymentStatus paymentStatus = PaymentStatus.PENDING;
        PaymentMethod paymentMethod = PaymentMethod.CARD;
        PaymentStatus confirmationStatus = PaymentStatus.FAILED;

        User user = createUser(userId);
        Order order = createOrder(
                orderId,
                total,
                orderStatus,
                user
        );
        Payment payment = createPayment(
                paymentId,
                paymentMethod,
                paymentStatus,
                order,
                total
        );

        when(orderLookupService.getOrderByIdAndUserId(orderId, userId)).thenReturn(order);
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(payment));

        PaymentResponse paymentResponse = paymentService.confirmPayment(orderId, userId, confirmationStatus);

        assertThat(paymentResponse).isNotNull();
        assertThat(paymentResponse.paymentId()).isEqualTo(paymentId);
        assertThat(paymentResponse.orderId()).isEqualTo(orderId);
        assertThat(paymentResponse.amount()).isEqualByComparingTo(total);
        assertThat(paymentResponse.paymentMethod()).isEqualTo(paymentMethod);
        assertThat(paymentResponse.paymentStatus()).isEqualTo(confirmationStatus);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.FAILED);

        verify(orderLookupService).getOrderByIdAndUserId(orderId, userId);
        verify(paymentRepository).findByOrderId(orderId);
        verifyNoMoreInteractions(orderLookupService);
        verifyNoMoreInteractions(paymentRepository);
    }

    @Test
    void confirmPayment_whenConfirmationStatusIsPending_throwsInvalidPaymentStateException() {
        Long paymentId = 1L;
        Long userId = 2L;
        Long orderId = 3L;
        BigDecimal total = BigDecimal.valueOf(500);
        OrderStatus orderStatus = OrderStatus.PENDING_PAYMENT;
        PaymentStatus paymentStatus = PaymentStatus.PENDING;
        PaymentMethod paymentMethod = PaymentMethod.CARD;
        PaymentStatus confirmationStatus = PaymentStatus.PENDING;

        User user = createUser(userId);
        Order order = createOrder(
                orderId,
                total,
                orderStatus,
                user
        );
        Payment payment = createPayment(
                paymentId,
                paymentMethod,
                paymentStatus,
                order,
                total
        );

        when(orderLookupService.getOrderByIdAndUserId(orderId, userId)).thenReturn(order);
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(payment));

        InvalidPaymentStateException ex = assertThrows(
                InvalidPaymentStateException.class,
                () -> paymentService.confirmPayment(
                        orderId,
                        userId,
                        confirmationStatus
                )
        );

        assertThat(ex.getMessage())
                .isEqualTo(INVALID_PAYMENT_STATUS);
        assertThat(order.getStatus()).isEqualTo(orderStatus);
        assertThat(payment.getPaymentStatus()).isEqualTo(paymentStatus);

        verify(orderLookupService).getOrderByIdAndUserId(orderId, userId);
        verify(paymentRepository).findByOrderId(orderId);
        verifyNoMoreInteractions(orderLookupService);
        verifyNoMoreInteractions(paymentRepository);
    }

    @Test
    void confirmPayment_whenOrderDoesNotExist_throwsNoResourceFoundException() {
        Long userId = 2L;
        Long orderId = 3L;
        PaymentStatus paymentStatus = PaymentStatus.SUCCESS;

        when(orderLookupService.getOrderByIdAndUserId(orderId, userId))
                .thenThrow(new NoResourceFoundException(orderNotFoundWithIdAndUserId(orderId, userId)));

        NoResourceFoundException ex = assertThrows(
                NoResourceFoundException.class,
                () -> paymentService.confirmPayment(orderId, userId, paymentStatus)
        );

        assertThat(ex).isNotNull();
        assertThat(ex.getMessage()).isEqualTo(orderNotFoundWithIdAndUserId(orderId, userId));

        verify(orderLookupService).getOrderByIdAndUserId(orderId, userId);
        verifyNoMoreInteractions(orderLookupService);
        verifyNoInteractions(paymentRepository);
    }

    @Test
    void confirmPayment_whenPaymentDoesNotExist_throwsNoResourceFoundException() {
        Long userId = 2L;
        Long orderId = 3L;
        User user = createUser(userId);
        BigDecimal total = BigDecimal.valueOf(500);
        OrderStatus orderStatus = OrderStatus.PENDING_PAYMENT;
        Order order = createOrder(
                orderId,
                total,
                orderStatus,
                user
        );
        PaymentStatus paymentStatus = PaymentStatus.SUCCESS;

        when(orderLookupService.getOrderByIdAndUserId(orderId, userId)).thenReturn(order);
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.empty());

        NoResourceFoundException ex = assertThrows(
                NoResourceFoundException.class,
                () -> paymentService.confirmPayment(orderId, userId, paymentStatus)
        );

        assertThat(ex).isNotNull();
        assertThat(ex.getMessage()).isEqualTo(paymentNotFoundWithOrderId(orderId));

        verify(orderLookupService).getOrderByIdAndUserId(orderId, userId);
        verify(paymentRepository).findByOrderId(orderId);
        verifyNoMoreInteractions(orderLookupService);
        verifyNoMoreInteractions(paymentRepository);
    }

    @Test
    void confirmPayment_whenPaymentIsNotPending_throwsInvalidPaymentStateException() {
        Long paymentId = 1L;
        Long userId = 2L;
        Long orderId = 3L;
        User user = createUser(userId);
        BigDecimal total = BigDecimal.valueOf(500);
        OrderStatus orderStatus = OrderStatus.PENDING_PAYMENT;
        PaymentMethod paymentMethod = PaymentMethod.CARD;
        PaymentStatus paymentStatus = PaymentStatus.SUCCESS;
        PaymentStatus confirmationStatus = PaymentStatus.FAILED;

        Order order = createOrder(
                orderId,
                total,
                orderStatus,
                user
        );

        Payment payment = createPayment(
                paymentId,
                paymentMethod,
                paymentStatus,
                order,
                total
        );

        when(orderLookupService.getOrderByIdAndUserId(orderId, userId)).thenReturn(order);
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(payment));

        InvalidPaymentStateException ex = assertThrows(
                InvalidPaymentStateException.class,
                () -> paymentService.confirmPayment(orderId, userId, confirmationStatus)
        );

        assertThat(ex).isNotNull();
        assertThat(ex.getMessage()).isEqualTo(PAYMENT_CANNOT_BE_CONFIRMED);
        assertThat(order.getStatus()).isEqualTo(orderStatus);
        assertThat(payment.getPaymentStatus()).isEqualTo(paymentStatus);

        verify(orderLookupService).getOrderByIdAndUserId(orderId, userId);
        verify(paymentRepository).findByOrderId(orderId);
        verifyNoMoreInteractions(orderLookupService);
        verifyNoMoreInteractions(paymentRepository);
    }

    @Test
    void confirmPayment_whenOrderIsNotPendingPayment_throwsInvalidOrderStateException() {
        Long paymentId = 1L;
        Long userId = 2L;
        Long orderId = 3L;
        User user = createUser(userId);
        BigDecimal total = BigDecimal.valueOf(500);
        OrderStatus orderStatus = OrderStatus.SHIPPED;
        PaymentMethod paymentMethod = PaymentMethod.CARD;
        PaymentStatus paymentStatus = PaymentStatus.PENDING;
        PaymentStatus confirmationStatus = PaymentStatus.FAILED;

        Order order = createOrder(
                orderId,
                total,
                orderStatus,
                user
        );

        Payment payment = createPayment(
                paymentId,
                paymentMethod,
                paymentStatus,
                order,
                total
        );

        when(orderLookupService.getOrderByIdAndUserId(orderId, userId)).thenReturn(order);
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(payment));

        InvalidOrderStateException ex = assertThrows(
                InvalidOrderStateException.class,
                () -> paymentService.confirmPayment(orderId, userId, confirmationStatus)
        );

        assertThat(ex).isNotNull();
        assertThat(ex.getMessage()).isEqualTo(ORDER_NOT_PENDING_PAYMENT);
        assertThat(order.getStatus()).isEqualTo(orderStatus);
        assertThat(payment.getPaymentStatus()).isEqualTo(paymentStatus);

        verify(orderLookupService).getOrderByIdAndUserId(orderId, userId);
        verify(paymentRepository).findByOrderId(orderId);
        verifyNoMoreInteractions(orderLookupService);
        verifyNoMoreInteractions(paymentRepository);
    }
}
