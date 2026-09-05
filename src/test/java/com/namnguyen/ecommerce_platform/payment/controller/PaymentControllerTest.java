package com.namnguyen.ecommerce_platform.payment.controller;

import com.namnguyen.ecommerce_platform.common.exception.*;
import com.namnguyen.ecommerce_platform.common.rate_limit.RateLimitService;
import com.namnguyen.ecommerce_platform.order.exception.InvalidOrderStateException;
import com.namnguyen.ecommerce_platform.payment.dto.*;
import com.namnguyen.ecommerce_platform.payment.enums.*;
import com.namnguyen.ecommerce_platform.payment.exception.InvalidPaymentStateException;
import com.namnguyen.ecommerce_platform.payment.service.PaymentService;
import com.namnguyen.ecommerce_platform.security.jwt.JwtService;
import com.namnguyen.ecommerce_platform.security.user.CustomUserDetailsService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static com.namnguyen.ecommerce_platform.testutil.MockAuthentication.*;
import static com.namnguyen.ecommerce_platform.testutil.TestDataFactory.*;
import static com.namnguyen.ecommerce_platform.testutil.messages.CommonTestMessages.VALIDATION_FAILED;
import static com.namnguyen.ecommerce_platform.testutil.messages.CommonTestMessages.invalidParameter;
import static com.namnguyen.ecommerce_platform.testutil.messages.PaymentTestMessages.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.containsInAnyOrder;

@WebMvcTest(PaymentController.class)
@AutoConfigureMockMvc(addFilters = false)
public class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PaymentService paymentService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private RateLimitService rateLimitService;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void submitPayment_whenRequestIsValid_returnsPaymentResponse() throws Exception {
        Long userId = 1L;
        Long orderId = 2L;
        Long paymentId = 3L;
        BigDecimal amount = BigDecimal.valueOf(499.99);
        PaymentMethod paymentMethod = PaymentMethod.CARD;
        PaymentStatus paymentStatus = PaymentStatus.PENDING;

        PaymentRequest paymentRequest = new PaymentRequest(paymentMethod);

        PaymentResponse paymentResponse = new PaymentResponse(
                paymentId,
                orderId,
                amount,
                paymentMethod,
                paymentStatus,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        
        when(paymentService.submitPayment(orderId, userId, paymentRequest))
                .thenReturn(paymentResponse);

        authenticateUser(userId);

        mockMvc.perform(post(paymentUri(orderId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(paymentRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentId").value(paymentId))
                .andExpect(jsonPath("$.orderId").value(orderId))
                .andExpect(jsonPath("$.amount").value(amount.doubleValue()))
                .andExpect(jsonPath("$.paymentMethod").value(paymentMethod.name()))
                .andExpect(jsonPath("$.paymentStatus").value(paymentStatus.name()))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());

        ArgumentCaptor<PaymentRequest> paymentRequestCaptor = ArgumentCaptor.forClass(PaymentRequest.class);
        verify(paymentService).submitPayment(eq(orderId), eq(userId), paymentRequestCaptor.capture());

        PaymentRequest capturedPaymentRequest = paymentRequestCaptor.getValue();

        assertThat(capturedPaymentRequest.paymentMethod()).isEqualTo(paymentMethod);

        verifyNoMoreInteractions(paymentService);
    }

    @Test
    void submitPayment_whenPaymentMethodIsNull_returnsBadRequest() throws Exception {
        Long userId = 1L;
        Long orderId = 2L;

        PaymentRequest paymentRequest = new PaymentRequest(null);

        authenticateUser(userId);

        mockMvc.perform(post(paymentUri(orderId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paymentRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                .andExpect(jsonPath("$.uri").value(paymentUri(orderId)))
                .andExpect(jsonPath("$.fieldErrors.paymentMethod",
                        containsInAnyOrder(PAYMENT_METHOD_IS_REQUIRED)));

        verifyNoInteractions(paymentService);
    }

    @Test
    void submitPayment_whenPaymentMethodIsInvalid_returnsBadRequest() throws Exception {
        Long userId = 1L;
        Long orderId = 2L;

        String requestBody = """
            {
                "paymentMethod": "%s"
            }
            """.formatted(INVALID_ENUM_VALUE);

        authenticateUser(userId);

        mockMvc.perform(post(paymentUri(orderId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                .andExpect(jsonPath("$.uri").value(paymentUri(orderId)))
                .andExpect(jsonPath("$.fieldErrors.paymentMethod",
                        containsInAnyOrder(paymentMethodIsInvalid(INVALID_ENUM_VALUE))));

        verifyNoInteractions(paymentService);
    }

    @Test
    void submitPayment_whenOrderNotFound_returnsNotFound() throws Exception {
        Long userId = 1L;
        Long orderId = 2L;

        PaymentRequest paymentRequest = new PaymentRequest(PaymentMethod.CARD);

        when(paymentService.submitPayment(orderId, userId, paymentRequest))
                .thenThrow(new NoResourceFoundException(orderNotFoundWithIdAndUserId(orderId, userId)));

        authenticateUser(userId);

        mockMvc.perform(post(paymentUri(orderId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paymentRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.NOT_FOUND.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(orderNotFoundWithIdAndUserId(orderId, userId)))
                .andExpect(jsonPath("$.uri").value(paymentUri(orderId)));

        verify(paymentService).submitPayment(orderId, userId, paymentRequest);
        verifyNoMoreInteractions(paymentService);
    }

    @Test
    void submitPayment_whenOrderIsNotPendingPayment_returnsBadRequest() throws Exception {
        Long userId = 1L;
        Long orderId = 2L;

        PaymentRequest paymentRequest = new PaymentRequest(PaymentMethod.CARD);

        when(paymentService.submitPayment(orderId, userId, paymentRequest))
                .thenThrow(new InvalidOrderStateException(ORDER_NOT_PENDING_PAYMENT));

        authenticateUser(userId);

        mockMvc.perform(post(paymentUri(orderId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paymentRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(ORDER_NOT_PENDING_PAYMENT))
                .andExpect(jsonPath("$.uri").value(paymentUri(orderId)));

        verify(paymentService).submitPayment(orderId, userId, paymentRequest);
        verifyNoMoreInteractions(paymentService);
    }

    @Test
    void submitPayment_whenPaymentAlreadyExists_returnsConflict() throws Exception {
        Long userId = 1L;
        Long orderId = 2L;

        PaymentRequest paymentRequest = new PaymentRequest(PaymentMethod.CARD);

        when(paymentService.submitPayment(orderId, userId, paymentRequest))
                .thenThrow(new DuplicateResourceException(PAYMENT_ALREADY_EXISTS));

        authenticateUser(userId);

        mockMvc.perform(post(paymentUri(orderId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paymentRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.CONFLICT.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.CONFLICT.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(PAYMENT_ALREADY_EXISTS))
                .andExpect(jsonPath("$.uri").value(paymentUri(orderId)));

        verify(paymentService).submitPayment(orderId, userId, paymentRequest);
        verifyNoMoreInteractions(paymentService);
    }

    @Test
    void submitPayment_whenOrderIdIsInvalid_returnsBadRequest() throws Exception {
        Long userId = 1L;
        String orderId = INVALID_ID;

        authenticateUser(userId);

        mockMvc.perform(post(paymentUri(orderId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(invalidParameter("orderId")))
                .andExpect(jsonPath("$.uri").value(paymentUri(orderId)));

        verifyNoInteractions(paymentService);
    }

    @Test
    void getPayment_whenPaymentExists_returnsPaymentResponse() throws Exception {
        Long userId = 1L;
        Long orderId = 2L;
        Long paymentId = 3L;
        BigDecimal amount = BigDecimal.valueOf(499.99);
        PaymentMethod paymentMethod = PaymentMethod.CARD;
        PaymentStatus paymentStatus = PaymentStatus.PENDING;

        PaymentResponse paymentResponse = new PaymentResponse(
                paymentId,
                orderId,
                amount,
                paymentMethod,
                paymentStatus,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(paymentService.getPaymentByOrderId(orderId, userId))
                .thenReturn(paymentResponse);

        authenticateUser(userId);

        mockMvc.perform(get(paymentUri(orderId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(paymentId))
                .andExpect(jsonPath("$.orderId").value(orderId))
                .andExpect(jsonPath("$.amount").value(amount.doubleValue()))
                .andExpect(jsonPath("$.paymentMethod").value(paymentMethod.name()))
                .andExpect(jsonPath("$.paymentStatus").value(paymentStatus.name()))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());

        verify(paymentService).getPaymentByOrderId(orderId, userId);
        verifyNoMoreInteractions(paymentService);
    }

    @Test
    void getPayment_whenPaymentNotFound_returnsNotFound() throws Exception {
        Long userId = 1L;
        Long orderId = 2L;

        when(paymentService.getPaymentByOrderId(orderId, userId))
                .thenThrow(new NoResourceFoundException(paymentNotFoundWithOrderId(orderId)));

        authenticateUser(userId);

        mockMvc.perform(get(paymentUri(orderId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.NOT_FOUND.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(paymentNotFoundWithOrderId(orderId)))
                .andExpect(jsonPath("$.uri").value(paymentUri(orderId)));

        verify(paymentService).getPaymentByOrderId(orderId, userId);
        verifyNoMoreInteractions(paymentService);
    }

    @Test
    void getPayment_whenOrderNotFound_returnsNotFound() throws Exception {
        Long userId = 1L;
        Long orderId = 2L;

        when(paymentService.getPaymentByOrderId(orderId, userId))
                .thenThrow(new NoResourceFoundException(orderNotFoundWithIdAndUserId(orderId, userId)));

        authenticateUser(userId);

        mockMvc.perform(get(paymentUri(orderId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.NOT_FOUND.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(orderNotFoundWithIdAndUserId(orderId, userId)))
                .andExpect(jsonPath("$.uri").value(paymentUri(orderId)));

        verify(paymentService).getPaymentByOrderId(orderId, userId);
        verifyNoMoreInteractions(paymentService);
    }

    @Test
    void getPayment_whenOrderIdIsInvalid_returnsBadRequest() throws Exception {
        Long userId = 1L;
        String orderId = INVALID_ID;

        authenticateUser(userId);

        mockMvc.perform(get(paymentUri(orderId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(invalidParameter("orderId")))
                .andExpect(jsonPath("$.uri").value(paymentUri(orderId)));

        verifyNoInteractions(paymentService);
    }

    @Test
    void updatePayment_whenRequestIsValid_returnsPaymentResponse() throws Exception {
        Long userId = 1L;
        Long orderId = 2L;
        Long paymentId = 3L;
        BigDecimal amount = BigDecimal.valueOf(499.99);
        PaymentMethod paymentMethod = PaymentMethod.CARD;
        PaymentStatus paymentStatus = PaymentStatus.PENDING;

        PaymentRequest paymentRequest = new PaymentRequest(paymentMethod);

        PaymentResponse paymentResponse = new PaymentResponse(
                paymentId,
                orderId,
                amount,
                paymentMethod,
                paymentStatus,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(paymentService.updatePayment(orderId, userId, paymentRequest))
                .thenReturn(paymentResponse);

        authenticateUser(userId);

        mockMvc.perform(patch(paymentUri(orderId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(paymentRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(paymentId))
                .andExpect(jsonPath("$.orderId").value(orderId))
                .andExpect(jsonPath("$.amount").value(amount.doubleValue()))
                .andExpect(jsonPath("$.paymentMethod").value(paymentMethod.name()))
                .andExpect(jsonPath("$.paymentStatus").value(paymentStatus.name()))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());

        ArgumentCaptor<PaymentRequest> paymentRequestCaptor = ArgumentCaptor.forClass(PaymentRequest.class);
        verify(paymentService).updatePayment(eq(orderId), eq(userId), paymentRequestCaptor.capture());

        PaymentRequest capturedPaymentRequest = paymentRequestCaptor.getValue();
        assertThat(capturedPaymentRequest.paymentMethod()).isEqualTo(paymentMethod);

        verifyNoMoreInteractions(paymentService);
    }

    @Test
    void updatePayment_whenPaymentNotFound_returnsNotFound() throws Exception {
        Long userId = 1L;
        Long orderId = 2L;
        PaymentMethod paymentMethod = PaymentMethod.CARD;

        PaymentRequest paymentRequest = new PaymentRequest(paymentMethod);

        when(paymentService.updatePayment(orderId, userId, paymentRequest))
                .thenThrow(new NoResourceFoundException(paymentNotFoundWithOrderId(orderId)));

        authenticateUser(userId);

        mockMvc.perform(patch(paymentUri(orderId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paymentRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.NOT_FOUND.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(paymentNotFoundWithOrderId(orderId)))
                .andExpect(jsonPath("$.uri").value(paymentUri(orderId)));

        verify(paymentService).updatePayment(orderId, userId, paymentRequest);
        verifyNoMoreInteractions(paymentService);
    }

    @Test
    void updatePayment_whenPaymentMethodIsNull_returnsBadRequest() throws Exception {
        Long userId = 1L;
        Long orderId = 2L;

        PaymentRequest paymentRequest = new PaymentRequest(null);

        authenticateUser(userId);

        mockMvc.perform(patch(paymentUri(orderId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paymentRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                .andExpect(jsonPath("$.uri").value(paymentUri(orderId)))
                .andExpect(jsonPath("$.fieldErrors.paymentMethod",
                        containsInAnyOrder(PAYMENT_METHOD_IS_REQUIRED)));

        verifyNoInteractions(paymentService);
    }

    @Test
    void updatePayment_whenPaymentMethodIsInvalid_returnsBadRequest() throws Exception {
        Long userId = 1L;
        Long orderId = 2L;

        String requestBody = """
            {
                "paymentMethod": "%s"
            }
            """.formatted(INVALID_ENUM_VALUE);

        authenticateUser(userId);

        mockMvc.perform(patch(paymentUri(orderId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                .andExpect(jsonPath("$.uri").value(paymentUri(orderId)))
                .andExpect(jsonPath("$.fieldErrors.paymentMethod",
                        containsInAnyOrder(paymentMethodIsInvalid(INVALID_ENUM_VALUE))));

        verifyNoInteractions(paymentService);
    }

    @Test
    void updatePayment_whenOrderNotFound_returnsNotFound() throws Exception {
        Long userId = 1L;
        Long orderId = 2L;
        PaymentMethod paymentMethod = PaymentMethod.CARD;

        PaymentRequest paymentRequest = new PaymentRequest(paymentMethod);

        when(paymentService.updatePayment(orderId, userId, paymentRequest))
                .thenThrow(new NoResourceFoundException(orderNotFoundWithIdAndUserId(orderId, userId)));

        authenticateUser(userId);

        mockMvc.perform(patch(paymentUri(orderId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paymentRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.NOT_FOUND.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(orderNotFoundWithIdAndUserId(orderId, userId)))
                .andExpect(jsonPath("$.uri").value(paymentUri(orderId)));

        verify(paymentService).updatePayment(orderId, userId, paymentRequest);
        verifyNoMoreInteractions(paymentService);
    }

    @Test
    void updatePayment_whenPaymentIsNotPending_returnsBadRequest() throws Exception {
        Long userId = 1L;
        Long orderId = 2L;
        PaymentMethod paymentMethod = PaymentMethod.CARD;

        PaymentRequest paymentRequest = new PaymentRequest(paymentMethod);

        when(paymentService.updatePayment(orderId, userId, paymentRequest))
                .thenThrow(new InvalidPaymentStateException(PAYMENT_NOT_PENDING));

        authenticateUser(userId);

        mockMvc.perform(patch(paymentUri(orderId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paymentRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(PAYMENT_NOT_PENDING))
                .andExpect(jsonPath("$.uri").value(paymentUri(orderId)));

        verify(paymentService).updatePayment(orderId, userId, paymentRequest);
        verifyNoMoreInteractions(paymentService);
    }

    @Test
    void updatePayment_whenOrderIdIsInvalid_returnsBadRequest() throws Exception {
        Long userId = 1L;
        String orderId = INVALID_ID;
        PaymentMethod paymentMethod = PaymentMethod.CARD;

        PaymentRequest paymentRequest = new PaymentRequest(paymentMethod);

        authenticateUser(userId);

        mockMvc.perform(patch(paymentUri(orderId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paymentRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(invalidParameter("orderId")))
                .andExpect(jsonPath("$.uri").value(paymentUri(orderId)));

        verifyNoInteractions(paymentService);
    }

    @Test
    void confirmPayment_whenRequestIsValidWithSuccess_returnsPaymentResponse() throws Exception {
        Long userId = 1L;
        Long orderId = 2L;
        Long paymentId = 3L;
        BigDecimal amount = BigDecimal.valueOf(499.99);
        PaymentMethod paymentMethod = PaymentMethod.CARD;
        PaymentStatus paymentStatus = PaymentStatus.SUCCESS;

        PaymentResponse paymentResponse = new PaymentResponse(
                paymentId,
                orderId,
                amount,
                paymentMethod,
                paymentStatus,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(paymentService.confirmPayment(orderId, userId, paymentStatus))
                .thenReturn(paymentResponse);

        authenticateUser(userId);

        mockMvc.perform(post(paymentConfirmUri(orderId))
                        .param("paymentStatus", paymentStatus.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(paymentId))
                .andExpect(jsonPath("$.orderId").value(orderId))
                .andExpect(jsonPath("$.amount").value(amount.doubleValue()))
                .andExpect(jsonPath("$.paymentMethod").value(paymentMethod.name()))
                .andExpect(jsonPath("$.paymentStatus").value(paymentStatus.name()))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());

        ArgumentCaptor<PaymentStatus> paymentStatusCaptor = ArgumentCaptor.forClass(PaymentStatus.class);
        verify(paymentService).confirmPayment(eq(orderId), eq(userId), paymentStatusCaptor.capture());

        PaymentStatus capturedPaymentStatus = paymentStatusCaptor.getValue();
        assertThat(capturedPaymentStatus).isEqualTo(paymentStatus);

        verifyNoMoreInteractions(paymentService);
    }

    @Test
    void confirmPayment_whenRequestIsValidWithFailed_returnsPaymentResponse() throws Exception {
        Long userId = 1L;
        Long orderId = 2L;
        Long paymentId = 3L;
        BigDecimal amount = BigDecimal.valueOf(499.99);
        PaymentMethod paymentMethod = PaymentMethod.CARD;
        PaymentStatus paymentStatus = PaymentStatus.FAILED;

        PaymentResponse paymentResponse = new PaymentResponse(
                paymentId,
                orderId,
                amount,
                paymentMethod,
                paymentStatus,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(paymentService.confirmPayment(orderId, userId, paymentStatus))
                .thenReturn(paymentResponse);

        authenticateUser(userId);

        mockMvc.perform(post(paymentConfirmUri(orderId))
                        .param("paymentStatus", paymentStatus.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(paymentId))
                .andExpect(jsonPath("$.orderId").value(orderId))
                .andExpect(jsonPath("$.amount").value(amount.doubleValue()))
                .andExpect(jsonPath("$.paymentMethod").value(paymentMethod.name()))
                .andExpect(jsonPath("$.paymentStatus").value(paymentStatus.name()))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());

        ArgumentCaptor<PaymentStatus> paymentStatusCaptor = ArgumentCaptor.forClass(PaymentStatus.class);
        verify(paymentService).confirmPayment(eq(orderId), eq(userId), paymentStatusCaptor.capture());

        PaymentStatus capturedPaymentStatus = paymentStatusCaptor.getValue();
        assertThat(capturedPaymentStatus).isEqualTo(paymentStatus);

        verifyNoMoreInteractions(paymentService);
    }

    @Test
    void confirmPayment_whenPaymentNotFound_returnsNotFound() throws Exception {
        Long userId = 1L;
        Long orderId = 2L;
        PaymentStatus paymentStatus = PaymentStatus.SUCCESS;

        when(paymentService.confirmPayment(orderId, userId, paymentStatus))
                .thenThrow(new NoResourceFoundException(paymentNotFoundWithOrderId(orderId)));

        authenticateUser(userId);

        mockMvc.perform(post(paymentConfirmUri(orderId))
                        .param("paymentStatus", paymentStatus.name()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.NOT_FOUND.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(paymentNotFoundWithOrderId(orderId)))
                .andExpect(jsonPath("$.uri").value(paymentConfirmUri(orderId)));

        verify(paymentService).confirmPayment(orderId, userId, paymentStatus);
        verifyNoMoreInteractions(paymentService);
    }

    @Test
    void confirmPayment_whenOrderNotFound_returnsNotFound() throws Exception {
        Long userId = 1L;
        Long orderId = 2L;
        PaymentStatus paymentStatus = PaymentStatus.SUCCESS;

        when(paymentService.confirmPayment(orderId, userId, paymentStatus))
                .thenThrow(new NoResourceFoundException(orderNotFoundWithIdAndUserId(orderId, userId)));

        authenticateUser(userId);

        mockMvc.perform(post(paymentConfirmUri(orderId))
                        .param("paymentStatus", paymentStatus.name()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.NOT_FOUND.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(orderNotFoundWithIdAndUserId(orderId, userId)))
                .andExpect(jsonPath("$.uri").value(paymentConfirmUri(orderId)));

        verify(paymentService).confirmPayment(orderId, userId, paymentStatus);
        verifyNoMoreInteractions(paymentService);
    }

    @Test
    void confirmPayment_whenOrderIsNotPendingPayment_returnsBadRequest() throws Exception {
        Long userId = 1L;
        Long orderId = 2L;
        PaymentStatus paymentStatus = PaymentStatus.SUCCESS;

        when(paymentService.confirmPayment(orderId, userId, paymentStatus))
                .thenThrow(new InvalidOrderStateException(ORDER_NOT_PENDING_PAYMENT));

        authenticateUser(userId);

        mockMvc.perform(post(paymentConfirmUri(orderId))
                        .param("paymentStatus", paymentStatus.name()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(ORDER_NOT_PENDING_PAYMENT))
                .andExpect(jsonPath("$.uri").value(paymentConfirmUri(orderId)));

        verify(paymentService).confirmPayment(orderId, userId, paymentStatus);
        verifyNoMoreInteractions(paymentService);
    }

    @Test
    void confirmPayment_whenPaymentIsNotPending_returnsBadRequest() throws Exception {
        Long userId = 1L;
        Long orderId = 2L;
        PaymentStatus paymentStatus = PaymentStatus.SUCCESS;

        when(paymentService.confirmPayment(orderId, userId, paymentStatus))
                .thenThrow(new InvalidPaymentStateException(PAYMENT_NOT_PENDING));

        authenticateUser(userId);

        mockMvc.perform(post(paymentConfirmUri(orderId))
                        .param("paymentStatus", paymentStatus.name()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(PAYMENT_NOT_PENDING))
                .andExpect(jsonPath("$.uri").value(paymentConfirmUri(orderId)));

        verify(paymentService).confirmPayment(orderId, userId, paymentStatus);
        verifyNoMoreInteractions(paymentService);
    }

    @Test
    void confirmPayment_whenConfirmedStatusIsPending_returnsBadRequest() throws Exception {
        Long userId = 1L;
        Long orderId = 2L;
        PaymentStatus paymentStatus = PaymentStatus.PENDING;

        when(paymentService.confirmPayment(orderId, userId, paymentStatus))
                .thenThrow(new InvalidPaymentStateException(INVALID_PAYMENT_STATUS));

        authenticateUser(userId);

        mockMvc.perform(post(paymentConfirmUri(orderId))
                        .param("paymentStatus", paymentStatus.name()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(INVALID_PAYMENT_STATUS))
                .andExpect(jsonPath("$.uri").value(paymentConfirmUri(orderId)));

        verify(paymentService).confirmPayment(orderId, userId, paymentStatus);
        verifyNoMoreInteractions(paymentService);
    }

    @Test
    void confirmPayment_whenPaymentStatusParamIsInvalid_returnsBadRequest() throws Exception {
        Long userId = 1L;
        Long orderId = 2L;

        authenticateUser(userId);

        mockMvc.perform(post(paymentConfirmUri(orderId))
                        .param("paymentStatus", INVALID_ENUM_VALUE))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(paymentStatusIsInvalid(INVALID_ENUM_VALUE)))
                .andExpect(jsonPath("$.uri").value(paymentConfirmUri(orderId)));

        verifyNoInteractions(paymentService);
    }

    @Test
    void confirmPayment_whenOrderIdIsInvalid_returnsBadRequest() throws Exception {
        Long userId = 1L;
        String orderId = INVALID_ID;
        PaymentStatus paymentStatus = PaymentStatus.PENDING;

        authenticateUser(userId);

        mockMvc.perform(post(paymentConfirmUri(orderId))
                        .param("paymentStatus", paymentStatus.name()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(invalidParameter("orderId")))
                .andExpect(jsonPath("$.uri").value(paymentConfirmUri(orderId)));

        verifyNoInteractions(paymentService);
    }

    @Test
    void confirmPayment_whenPaymentStatusParamIsMissing_returnsBadRequest() throws Exception {
        Long userId = 1L;
        Long orderId = 2L;

        authenticateUser(userId);

        mockMvc.perform(post(paymentConfirmUri(orderId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                .andExpect(jsonPath("$.uri").value(paymentConfirmUri(orderId)))
                .andExpect(jsonPath("$.fieldErrors.paymentStatus",
                        containsInAnyOrder(invalidParameter("paymentStatus"))));

        verifyNoInteractions(paymentService);
    }
}
