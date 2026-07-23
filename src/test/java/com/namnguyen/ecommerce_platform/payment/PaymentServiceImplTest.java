package com.namnguyen.ecommerce_platform.payment;

import com.namnguyen.ecommerce_platform.common.exception.DuplicateResourceException;
import com.namnguyen.ecommerce_platform.common.exception.InvalidOrderStateException;
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
import com.namnguyen.ecommerce_platform.payment.service.PaymentServiceImpl;
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
    void submitPayment_whenOrderNotExists_throwNoResourceFoundException() {
        Long orderId = 1L;
        Long userId = 2L;
        BigDecimal total = BigDecimal.valueOf(500);
        PaymentMethod method = PaymentMethod.CARD;

        User user = createUser(userId);
        Order order = createOrder(
                orderId,
                total,
                OrderStatus.PENDING_PAYMENT,
                user
        );

        PaymentRequest request = new PaymentRequest(method);

        when(orderLookupService.getOrderByIdAndUserId(orderId, userId))
                .thenThrow(new NoResourceFoundException(orderNotFound(orderId, userId)));

        NoResourceFoundException ex = assertThrows(
                NoResourceFoundException.class,
                () -> paymentService.submitPayment(orderId, userId, request)
        );

        assertThat(ex).isNotNull();
        assertThat(ex.getMessage()).isEqualTo(orderNotFound(orderId, userId));
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        assertThat(order.getPayment()).isNull();

        verify(orderLookupService).getOrderByIdAndUserId(orderId, userId);
        verifyNoMoreInteractions(orderLookupService);
        verifyNoInteractions(paymentRepository);
    }

    @Test
    void submitPayment_whenOrderNotInPendingPaymentStatus_throwInvalidOrderStateException() {
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
        assertThat(ex.getMessage()).isEqualTo(notPendingPayment());
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);

        verify(orderLookupService).getOrderByIdAndUserId(orderId, userId);
        verifyNoMoreInteractions(orderLookupService);
        verifyNoInteractions(paymentRepository);
    }

    @Test
    void submitPayment_whenPaymentExistsForOrder_throwDuplicateResourceException() {
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
    void getPaymentByOrderId_whenPaymentExits_returnPaymentResponse() {
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
    void getPaymentByOrderId_whenOrderNotExits_returnPaymentResponse() {
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
    void getPaymentByOrderId_whenPaymentNotExits_returnPaymentResponse() {
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
        assertThat(response.amount()).isEqualTo(total);
        assertThat(response.paymentMethod()).isEqualTo(updatePaymentMethod);
        assertThat(response.paymentStatus()).isEqualTo(paymentStatus);

        verify(orderLookupService).getOrderByIdAndUserId(orderId, userId);
        verify(paymentRepository).findByOrderId(orderId);
        verifyNoMoreInteractions(orderLookupService);
        verifyNoMoreInteractions(paymentRepository);
    }
}
