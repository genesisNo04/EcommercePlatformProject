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
import static com.namnguyen.ecommerce_platform.testutil.TestMessages.*;
import static com.namnguyen.ecommerce_platform.testutil.TestMessages.invalidStatusConfirmed;
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
    void submitPayment_whenOrderExists_returnPaymentResponse() {
        Long orderId = 1L;
        Long userId = 2L;
        Long paymentId = 3L;
        BigDecimal total = BigDecimal.valueOf(500);
        PaymentMethod method = PaymentMethod.CARD;
        PaymentStatus status = PaymentStatus.PENDING;

        User user = createUser(userId);
        Order order = createOrder(
                orderId,
                total,
                OrderStatus.PENDING_PAYMENT,
                user
        );

        PaymentRequest request = new PaymentRequest(method);

        when(orderLookupService.getOrderByIdAndUserId(orderId, userId)).thenReturn(order);
        when(paymentRepository.existsByOrderId(orderId)).thenReturn(false);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment payment = inv.getArgument(0);
            payment.setId(paymentId);
            return payment;
        });

        PaymentResponse paymentResponse = paymentService.submitPayment(orderId, userId, request);

        assertThat(paymentResponse).isNotNull();
        assertThat(paymentResponse.paymentId()).isEqualTo(paymentId);
        assertThat(paymentResponse.orderId()).isEqualTo(orderId);
        assertThat(paymentResponse.amount()).isEqualByComparingTo(total);
        assertThat(paymentResponse.paymentMethod()).isEqualTo(method);
        assertThat(paymentResponse.paymentStatus()).isEqualTo(status);

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());

        Payment savedPayment = captor.getValue();
        assertThat(savedPayment.getId()).isEqualTo(paymentId);
        assertThat(savedPayment.getOrder()).isEqualTo(order);
        assertThat(savedPayment.getAmount()).isEqualByComparingTo(total);
        assertThat(savedPayment.getPaymentMethod()).isEqualTo(method);
        assertThat(savedPayment.getPaymentStatus()).isEqualTo(status);

        verify(orderLookupService).getOrderByIdAndUserId(orderId, userId);
        verify(paymentRepository).existsByOrderId(orderId);
        verifyNoMoreInteractions(orderLookupService);
        verifyNoMoreInteractions(paymentRepository);
    }

    @Test
    void submitPayment_whenOrderNotExists_throwsNoResourceFoundException() {
        Long orderId = 1L;
        Long userId = 2L;
        PaymentMethod method = PaymentMethod.CARD;

        PaymentRequest request = new PaymentRequest(method);

        when(orderLookupService.getOrderByIdAndUserId(orderId, userId))
                .thenThrow(new NoResourceFoundException(orderNotFound(orderId, userId)));

        NoResourceFoundException ex = assertThrows(
                NoResourceFoundException.class,
                () -> paymentService.submitPayment(orderId, userId, request)
        );

        assertThat(ex).isNotNull();
        assertThat(ex.getMessage()).isEqualTo(orderNotFound(orderId, userId));

        verify(orderLookupService).getOrderByIdAndUserId(orderId, userId);
        verifyNoMoreInteractions(orderLookupService);
        verifyNoInteractions(paymentRepository);
    }

    @Test
    void submitPayment_whenOrderNotInPendingPaymentStatus_throwsInvalidOrderStateException() {
        Long orderId = 1L;
        Long userId = 2L;
        BigDecimal total = BigDecimal.valueOf(500);
        PaymentMethod method = PaymentMethod.CARD;

        User user = createUser(userId);
        Order order = createOrder(
                orderId,
                total,
                OrderStatus.PAID,
                user
        );

        PaymentRequest request = new PaymentRequest(method);

        when(orderLookupService.getOrderByIdAndUserId(orderId, userId)).thenReturn(order);

        InvalidOrderStateException ex = assertThrows(
                InvalidOrderStateException.class,
                () -> paymentService.submitPayment(orderId, userId, request)
        );

        assertThat(ex).isNotNull();
        assertThat(ex.getMessage()).isEqualTo(orderNotInPendingPayment());
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
        PaymentMethod method = PaymentMethod.CARD;
        OrderStatus status = OrderStatus.PENDING_PAYMENT;

        User user = createUser(userId);
        Order order = createOrder(
                orderId,
                total,
                status,
                user
        );

        PaymentRequest request = new PaymentRequest(method);

        when(orderLookupService.getOrderByIdAndUserId(orderId, userId)).thenReturn(order);
        when(paymentRepository.existsByOrderId(orderId)).thenReturn(true);

        DuplicateResourceException ex = assertThrows(
                DuplicateResourceException.class,
                () -> paymentService.submitPayment(orderId, userId, request)
        );

        assertThat(ex).isNotNull();
        assertThat(ex.getMessage()).isEqualTo(paymentDuplicate());
        assertThat(order.getStatus()).isEqualTo(status);

        verify(orderLookupService).getOrderByIdAndUserId(orderId, userId);
        verify(paymentRepository).existsByOrderId(orderId);
        verifyNoMoreInteractions(orderLookupService);
        verifyNoMoreInteractions(paymentRepository);
    }

    @Test
    void getPaymentByOrderId_whenPaymentExists_returnPaymentResponse() {
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

        PaymentResponse response = paymentService.getPaymentByOrderId(orderId, userId);

        assertThat(response).isNotNull();
        assertThat(response.paymentId()).isEqualTo(paymentId);
        assertThat(response.orderId()).isEqualTo(orderId);
        assertThat(response.amount()).isEqualByComparingTo(total);
        assertThat(response.paymentStatus()).isEqualTo(paymentStatus);
        assertThat(response.paymentMethod()).isEqualTo(paymentMethod);

        verify(orderLookupService).getOrderByIdAndUserId(orderId, userId);
        verify(paymentRepository).findByOrderId(orderId);
        verifyNoMoreInteractions(orderLookupService);
        verifyNoMoreInteractions(paymentRepository);
    }

    @Test
    void getPaymentByOrderId_whenOrderNotExists_throwsNoResourceFoundException() {
        Long userId = 2L;
        Long orderId = 3L;

        when(orderLookupService.getOrderByIdAndUserId(orderId, userId))
                .thenThrow(new NoResourceFoundException(orderNotFound(orderId, userId)));

        NoResourceFoundException ex = assertThrows(
                NoResourceFoundException.class,
                () ->  paymentService.getPaymentByOrderId(orderId, userId)
        );

        assertThat(ex).isNotNull();
        assertThat(ex.getMessage()).isEqualTo(orderNotFound(orderId, userId));

        verify(orderLookupService).getOrderByIdAndUserId(orderId, userId);
        verifyNoMoreInteractions(orderLookupService);
        verifyNoInteractions(paymentRepository);
    }

    @Test
    void getPaymentByOrderId_whenPaymentNotExists_throwsNoResourceFoundException() {
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
        assertThat(ex.getMessage()).isEqualTo(paymentNotFound(orderId));

        verify(orderLookupService).getOrderByIdAndUserId(orderId, userId);
        verify(paymentRepository).findByOrderId(orderId);
        verifyNoMoreInteractions(orderLookupService);
        verifyNoMoreInteractions(paymentRepository);
    }

    @Test
    void updatePayment_whenPaymentExistsAndInPending_returnPaymentResponse() {
        Long paymentId = 1L;
        Long userId = 2L;
        Long orderId = 3L;
        BigDecimal total = BigDecimal.valueOf(500);
        OrderStatus orderStatus = OrderStatus.PENDING_PAYMENT;
        PaymentStatus paymentStatus = PaymentStatus.PENDING;
        PaymentMethod paymentMethod = PaymentMethod.CARD;
        PaymentMethod updatePaymentMethod = PaymentMethod.PAYPAL;

        PaymentRequest request = new PaymentRequest(updatePaymentMethod);

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

        PaymentResponse response = paymentService.updatePayment(orderId, userId, request);

        assertThat(response).isNotNull();
        assertThat(response.paymentId()).isEqualTo(paymentId);
        assertThat(response.orderId()).isEqualTo(orderId);
        assertThat(response.amount()).isEqualByComparingTo(total);
        assertThat(response.paymentMethod()).isEqualTo(updatePaymentMethod);
        assertThat(response.paymentStatus()).isEqualTo(paymentStatus);
        assertThat(payment.getPaymentStatus()).isEqualTo(paymentStatus);
        assertThat(payment.getPaymentMethod()).isEqualTo(updatePaymentMethod);

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

        PaymentRequest request = new PaymentRequest(updatePaymentMethod);

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
                () -> paymentService.updatePayment(orderId, userId, request)
        );

        assertThat(ex).isNotNull();
        assertThat(ex.getMessage()).isEqualTo(paymentNotPending());

        verify(orderLookupService).getOrderByIdAndUserId(orderId, userId);
        verify(paymentRepository).findByOrderId(orderId);
        verifyNoMoreInteractions(orderLookupService);
        verifyNoMoreInteractions(paymentRepository);
    }

    @Test
    void updatePayment_whenOrderNotExists_throwNoResourceFoundException() {
        Long userId = 2L;
        Long orderId = 3L;
        PaymentMethod updatePaymentMethod = PaymentMethod.PAYPAL;

        PaymentRequest request = new PaymentRequest(updatePaymentMethod);

        when(orderLookupService.getOrderByIdAndUserId(orderId, userId))
                .thenThrow(new NoResourceFoundException(orderNotFound(orderId, userId)));

        NoResourceFoundException ex = assertThrows(
                NoResourceFoundException.class,
                () -> paymentService.updatePayment(orderId, userId, request)
        );

        assertThat(ex).isNotNull();
        assertThat(ex.getMessage()).isEqualTo(orderNotFound(orderId, userId));

        verify(orderLookupService).getOrderByIdAndUserId(orderId, userId);
        verifyNoMoreInteractions(orderLookupService);
        verifyNoInteractions(paymentRepository);
    }

    @Test
    void updatePayment_whenPaymentNotExists_throwNoResourceFoundException() {
        Long userId = 2L;
        Long orderId = 3L;
        BigDecimal total = BigDecimal.valueOf(500);
        OrderStatus orderStatus = OrderStatus.PENDING_PAYMENT;
        PaymentMethod updatePaymentMethod = PaymentMethod.PAYPAL;

        PaymentRequest request = new PaymentRequest(updatePaymentMethod);

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
                () -> paymentService.updatePayment(orderId, userId, request)
        );

        assertThat(ex).isNotNull();
        assertThat(ex.getMessage()).isEqualTo(paymentNotFound(orderId));

        verify(orderLookupService).getOrderByIdAndUserId(orderId, userId);
        verify(paymentRepository).findByOrderId(orderId);
        verifyNoMoreInteractions(orderLookupService);
        verifyNoMoreInteractions(paymentRepository);
    }

    @Test
    void confirmPayment_whenPaymentExistsAndConfirmSuccess_returnPaymentResponse() {
        Long paymentId = 1L;
        Long userId = 2L;
        Long orderId = 3L;
        BigDecimal total = BigDecimal.valueOf(500);
        OrderStatus orderStatus = OrderStatus.PENDING_PAYMENT;
        PaymentStatus paymentStatus = PaymentStatus.PENDING;
        PaymentMethod paymentMethod = PaymentMethod.CARD;
        PaymentStatus confirmStatus = PaymentStatus.SUCCESS;

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

        PaymentResponse response = paymentService.confirmPayment(orderId, userId, confirmStatus);

        assertThat(response).isNotNull();
        assertThat(response.paymentId()).isEqualTo(paymentId);
        assertThat(response.orderId()).isEqualTo(orderId);
        assertThat(response.amount()).isEqualByComparingTo(total);
        assertThat(response.paymentMethod()).isEqualTo(paymentMethod);
        assertThat(response.paymentStatus()).isEqualTo(confirmStatus);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.SUCCESS);

        verify(orderLookupService).getOrderByIdAndUserId(orderId, userId);
        verify(paymentRepository).findByOrderId(orderId);
        verifyNoMoreInteractions(orderLookupService);
        verifyNoMoreInteractions(paymentRepository);
    }

    @Test
    void confirmPayment_whenPaymentExistsAndConfirmFailed_returnPaymentResponse() {
        Long paymentId = 1L;
        Long userId = 2L;
        Long orderId = 3L;
        BigDecimal total = BigDecimal.valueOf(500);
        OrderStatus orderStatus = OrderStatus.PENDING_PAYMENT;
        PaymentStatus paymentStatus = PaymentStatus.PENDING;
        PaymentMethod paymentMethod = PaymentMethod.CARD;
        PaymentStatus confirmStatus = PaymentStatus.FAILED;

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

        PaymentResponse response = paymentService.confirmPayment(orderId, userId, confirmStatus);

        assertThat(response).isNotNull();
        assertThat(response.paymentId()).isEqualTo(paymentId);
        assertThat(response.orderId()).isEqualTo(orderId);
        assertThat(response.amount()).isEqualByComparingTo(total);
        assertThat(response.paymentMethod()).isEqualTo(paymentMethod);
        assertThat(response.paymentStatus()).isEqualTo(confirmStatus);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.FAILED);

        verify(orderLookupService).getOrderByIdAndUserId(orderId, userId);
        verify(paymentRepository).findByOrderId(orderId);
        verifyNoMoreInteractions(orderLookupService);
        verifyNoMoreInteractions(paymentRepository);
    }

    @Test
    void confirmPayment_whenRequestInvalidStatus_returnInvalidPaymentStateException() {
        Long paymentId = 1L;
        Long userId = 2L;
        Long orderId = 3L;
        BigDecimal total = BigDecimal.valueOf(500);
        OrderStatus orderStatus = OrderStatus.PENDING_PAYMENT;
        PaymentStatus paymentStatus = PaymentStatus.PENDING;
        PaymentMethod paymentMethod = PaymentMethod.CARD;
        PaymentStatus confirmStatus = PaymentStatus.PENDING;

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
                () -> paymentService.confirmPayment(orderId, userId, confirmStatus)
        );

        assertThat(ex).isNotNull();
        assertThat(ex.getMessage()).isEqualTo(invalidStatusConfirmed());
        assertThat(order.getStatus()).isEqualTo(orderStatus);
        assertThat(payment.getPaymentStatus()).isEqualTo(paymentStatus);

        verify(orderLookupService).getOrderByIdAndUserId(orderId, userId);
        verify(paymentRepository).findByOrderId(orderId);
        verifyNoMoreInteractions(orderLookupService);
        verifyNoMoreInteractions(paymentRepository);
    }

    @Test
    void confirmPayment_whenOrderNotExists_throwNoResourceFound() {
        Long userId = 2L;
        Long orderId = 3L;
        PaymentStatus status = PaymentStatus.SUCCESS;

        when(orderLookupService.getOrderByIdAndUserId(orderId, userId))
                .thenThrow(new NoResourceFoundException(orderNotFound(orderId, userId)));

        NoResourceFoundException ex = assertThrows(
                NoResourceFoundException.class,
                () -> paymentService.confirmPayment(orderId, userId, status)
        );

        assertThat(ex).isNotNull();
        assertThat(ex.getMessage()).isEqualTo(orderNotFound(orderId, userId));

        verify(orderLookupService).getOrderByIdAndUserId(orderId, userId);
        verifyNoMoreInteractions(orderLookupService);
        verifyNoInteractions(paymentRepository);
    }

    @Test
    void confirmPayment_whenPaymentNotExists_throwNoResourceFound() {
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
        PaymentStatus status = PaymentStatus.SUCCESS;

        when(orderLookupService.getOrderByIdAndUserId(orderId, userId)).thenReturn(order);
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.empty());

        NoResourceFoundException ex = assertThrows(
                NoResourceFoundException.class,
                () -> paymentService.confirmPayment(orderId, userId, status)
        );

        assertThat(ex).isNotNull();
        assertThat(ex.getMessage()).isEqualTo(paymentNotFound(orderId));

        verify(orderLookupService).getOrderByIdAndUserId(orderId, userId);
        verify(paymentRepository).findByOrderId(orderId);
        verifyNoMoreInteractions(orderLookupService);
        verifyNoMoreInteractions(paymentRepository);
    }

    @Test
    void confirmPayment_whenPaymentNotInPending_throwInvalidPaymentStateException() {
        Long paymentId = 1L;
        Long userId = 2L;
        Long orderId = 3L;
        User user = createUser(userId);
        BigDecimal total = BigDecimal.valueOf(500);
        OrderStatus orderStatus = OrderStatus.PENDING_PAYMENT;
        PaymentMethod paymentMethod = PaymentMethod.CARD;
        PaymentStatus paymentStatus = PaymentStatus.SUCCESS;
        PaymentStatus submitStatus = PaymentStatus.FAILED;

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
                () -> paymentService.confirmPayment(orderId, userId, submitStatus)
        );

        assertThat(ex).isNotNull();
        assertThat(ex.getMessage()).isEqualTo(paymentCannotConfirmed());
        assertThat(order.getStatus()).isEqualTo(orderStatus);
        assertThat(payment.getPaymentStatus()).isEqualTo(paymentStatus);

        verify(orderLookupService).getOrderByIdAndUserId(orderId, userId);
        verify(paymentRepository).findByOrderId(orderId);
        verifyNoMoreInteractions(orderLookupService);
        verifyNoMoreInteractions(paymentRepository);
    }

    @Test
    void confirmPayment_whenOrderNotInPendingPayment_throwInvalidOrderStateException() {
        Long paymentId = 1L;
        Long userId = 2L;
        Long orderId = 3L;
        User user = createUser(userId);
        BigDecimal total = BigDecimal.valueOf(500);
        OrderStatus orderStatus = OrderStatus.SHIPPED;
        PaymentMethod paymentMethod = PaymentMethod.CARD;
        PaymentStatus paymentStatus = PaymentStatus.PENDING;
        PaymentStatus submitStatus = PaymentStatus.FAILED;

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
                () -> paymentService.confirmPayment(orderId, userId, submitStatus)
        );

        assertThat(ex).isNotNull();
        assertThat(ex.getMessage()).isEqualTo(orderNotInPendingPayment());
        assertThat(order.getStatus()).isEqualTo(orderStatus);
        assertThat(payment.getPaymentStatus()).isEqualTo(paymentStatus);

        verify(orderLookupService).getOrderByIdAndUserId(orderId, userId);
        verify(paymentRepository).findByOrderId(orderId);
        verifyNoMoreInteractions(orderLookupService);
        verifyNoMoreInteractions(paymentRepository);
    }
}
