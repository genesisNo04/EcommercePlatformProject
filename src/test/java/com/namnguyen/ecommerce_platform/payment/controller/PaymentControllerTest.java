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
import static com.namnguyen.ecommerce_platform.testutil.TestMessages.*;
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
    CustomUserDetailsService customUserDetailsService;

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
        PaymentMethod method = PaymentMethod.CARD;
        PaymentStatus status = PaymentStatus.PENDING;

        PaymentRequest request = new PaymentRequest(method);

        PaymentResponse response = new PaymentResponse(
                paymentId,
                orderId,
                amount,
                method,
                status,
                LocalDateTime.now(),
                LocalDateTime.now()
        );


        when(paymentService.submitPayment(orderId, userId, request))
                .thenReturn(response);

        authenticateUser(userId);

        mockMvc.perform(post(String.format(PAYMENT_URI, orderId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentId").value(paymentId))
                .andExpect(jsonPath("$.orderId").value(orderId))
                .andExpect(jsonPath("$.amount").value(amount.doubleValue()))
                .andExpect(jsonPath("$.paymentMethod").value(method.name()))
                .andExpect(jsonPath("$.paymentStatus").value(status.name()))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());

        ArgumentCaptor<PaymentRequest> captor = ArgumentCaptor.forClass(PaymentRequest.class);
        verify(paymentService).submitPayment(eq(orderId), eq(userId), captor.capture());

        PaymentRequest capturedRequest = captor.getValue();

        assertThat(capturedRequest.paymentMethod()).isEqualTo(method);

        verifyNoMoreInteractions(paymentService);
    }

    @Test
    void submitPayment_whenPaymentMethodIsNull_returnsBadRequest() throws Exception {
        Long userId = 1L;
        Long orderId = 2L;

        PaymentRequest request = new PaymentRequest(null);

        authenticateUser(userId);

        mockMvc.perform(post(String.format(PAYMENT_URI, orderId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.uri").value(String.format(PAYMENT_URI, orderId)))
                .andExpect(jsonPath("$.fieldErrors.paymentMethod",
                        containsInAnyOrder(paymentMethodRequired())));

        verifyNoInteractions(paymentService);
    }

    @Test
    void submitPayment_whenPaymentMethodIsInvalid_returnsBadRequest() throws Exception {
        Long userId = 1L;
        Long orderId = 2L;
        String method = "TESTING";

        String requestBody = """
            {
                "paymentMethod": "%s"
            }
            """.formatted(method);

        authenticateUser(userId);

        mockMvc.perform(post(String.format(PAYMENT_URI, orderId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.uri").value(String.format(PAYMENT_URI, orderId)))
                .andExpect(jsonPath("$.fieldErrors.paymentMethod",
                        containsInAnyOrder(paymentMethodInvalid(method))));

        verifyNoInteractions(paymentService);
    }

    @Test
    void submitPayment_whenOrderNotFound_returnsNotFound() throws Exception {
        Long userId = 1L;
        Long orderId = 2L;

        PaymentRequest request = new PaymentRequest(PaymentMethod.CARD);

        when(paymentService.submitPayment(orderId, userId, request))
                .thenThrow(new NoResourceFoundException(orderNotFound(orderId, userId)));

        authenticateUser(userId);

        mockMvc.perform(post(String.format(PAYMENT_URI, orderId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.NOT_FOUND.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(orderNotFound(orderId, userId)))
                .andExpect(jsonPath("$.uri").value(String.format(PAYMENT_URI, orderId)));

        verify(paymentService).submitPayment(orderId, userId, request);
        verifyNoMoreInteractions(paymentService);
    }

    @Test
    void submitPayment_whenOrderStatusIsNotInPendingPayment_returnsBadRequest() throws Exception {
        Long userId = 1L;
        Long orderId = 2L;

        PaymentRequest request = new PaymentRequest(PaymentMethod.CARD);

        when(paymentService.submitPayment(orderId, userId, request))
                .thenThrow(new InvalidOrderStateException(orderNotInPendingPayment()));

        authenticateUser(userId);

        mockMvc.perform(post(String.format(PAYMENT_URI, orderId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(orderNotInPendingPayment()))
                .andExpect(jsonPath("$.uri").value(String.format(PAYMENT_URI, orderId)));

        verify(paymentService).submitPayment(orderId, userId, request);
        verifyNoMoreInteractions(paymentService);
    }

    @Test
    void submitPayment_whenPaymentAlreadyExists_returnsBadRequest() throws Exception {
        Long userId = 1L;
        Long orderId = 2L;

        PaymentRequest request = new PaymentRequest(PaymentMethod.CARD);

        when(paymentService.submitPayment(orderId, userId, request))
                .thenThrow(new DuplicateResourceException(paymentDuplicate()));

        authenticateUser(userId);

        mockMvc.perform(post(String.format(PAYMENT_URI, orderId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.CONFLICT.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.CONFLICT.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(paymentDuplicate()))
                .andExpect(jsonPath("$.uri").value(String.format(PAYMENT_URI, orderId)));

        verify(paymentService).submitPayment(orderId, userId, request);
        verifyNoMoreInteractions(paymentService);
    }

    @Test
    void submitPayment_whenOrderIdIsInvalid_returnsBadRequest() throws Exception {
        Long userId = 1L;
        String orderId = INVALID_ID;

        authenticateUser(userId);

        mockMvc.perform(post(String.format(PAYMENT_URI, orderId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(invalidParameter("orderId")))
                .andExpect(jsonPath("$.uri").value(String.format(PAYMENT_URI, orderId)));

        verifyNoInteractions(paymentService);
    }

    @Test
    void getPayment_whenPaymentExists_returnsPaymentResponse() throws Exception {
        Long userId = 1L;
        Long orderId = 2L;
        Long paymentId = 3L;
        BigDecimal amount = BigDecimal.valueOf(499.99);
        PaymentMethod method = PaymentMethod.CARD;
        PaymentStatus status = PaymentStatus.PENDING;

        PaymentResponse response = new PaymentResponse(
                paymentId,
                orderId,
                amount,
                method,
                status,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(paymentService.getPaymentByOrderId(orderId, userId))
                .thenReturn(response);

        authenticateUser(userId);

        mockMvc.perform(get(String.format(PAYMENT_URI, orderId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(paymentId))
                .andExpect(jsonPath("$.orderId").value(orderId))
                .andExpect(jsonPath("$.amount").value(amount.doubleValue()))
                .andExpect(jsonPath("$.paymentMethod").value(method.name()))
                .andExpect(jsonPath("$.paymentStatus").value(status.name()))
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
                .thenThrow(new NoResourceFoundException(paymentNotFound(orderId)));

        authenticateUser(userId);

        mockMvc.perform(get(String.format(PAYMENT_URI, orderId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.NOT_FOUND.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(paymentNotFound(orderId)))
                .andExpect(jsonPath("$.uri").value(String.format(PAYMENT_URI, orderId)));

        verify(paymentService).getPaymentByOrderId(orderId, userId);
        verifyNoMoreInteractions(paymentService);
    }

    @Test
    void getPayment_whenOrderNotFound_returnsNotFound() throws Exception {
        Long userId = 1L;
        Long orderId = 2L;

        when(paymentService.getPaymentByOrderId(orderId, userId))
                .thenThrow(new NoResourceFoundException(orderNotFound(orderId, userId)));

        authenticateUser(userId);

        mockMvc.perform(get(String.format(PAYMENT_URI, orderId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.NOT_FOUND.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(orderNotFound(orderId, userId)))
                .andExpect(jsonPath("$.uri").value(String.format(PAYMENT_URI, orderId)));

        verify(paymentService).getPaymentByOrderId(orderId, userId);
        verifyNoMoreInteractions(paymentService);
    }

    @Test
    void getPayment_whenOrderIdIsInvalid_returnsBadRequest() throws Exception {
        Long userId = 1L;
        String orderId = INVALID_ID;

        authenticateUser(userId);

        mockMvc.perform(get(String.format(PAYMENT_URI, orderId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(invalidParameter("orderId")))
                .andExpect(jsonPath("$.uri").value(String.format(PAYMENT_URI, orderId)));

        verifyNoInteractions(paymentService);
    }

    @Test
    void updatePayment_whenRequestIsValid_returnsPaymentResponse() throws Exception {
        Long userId = 1L;
        Long orderId = 2L;
        Long paymentId = 3L;
        BigDecimal amount = BigDecimal.valueOf(499.99);
        PaymentMethod method = PaymentMethod.CARD;
        PaymentStatus status = PaymentStatus.PENDING;

        PaymentRequest request = new PaymentRequest(method);

        PaymentResponse response = new PaymentResponse(
                paymentId,
                orderId,
                amount,
                method,
                status,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(paymentService.updatePayment(orderId, userId, request))
                .thenReturn(response);

        authenticateUser(userId);

        mockMvc.perform(patch(String.format(PAYMENT_URI, orderId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(paymentId))
                .andExpect(jsonPath("$.orderId").value(orderId))
                .andExpect(jsonPath("$.amount").value(amount.doubleValue()))
                .andExpect(jsonPath("$.paymentMethod").value(method.name()))
                .andExpect(jsonPath("$.paymentStatus").value(status.name()))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());

        ArgumentCaptor<PaymentRequest> captor = ArgumentCaptor.forClass(PaymentRequest.class);
        verify(paymentService).updatePayment(eq(orderId), eq(userId), captor.capture());

        PaymentRequest capturedRequest = captor.getValue();
        assertThat(capturedRequest.paymentMethod()).isEqualTo(method);

        verifyNoMoreInteractions(paymentService);
    }

    @Test
    void updatePayment_whenPaymentNotFound_returnsNotFound() throws Exception {
        Long userId = 1L;
        Long orderId = 2L;
        PaymentMethod method = PaymentMethod.CARD;

        PaymentRequest request = new PaymentRequest(method);

        when(paymentService.updatePayment(orderId, userId, request))
                .thenThrow(new NoResourceFoundException(paymentNotFound(orderId)));

        authenticateUser(userId);

        mockMvc.perform(patch(String.format(PAYMENT_URI, orderId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.NOT_FOUND.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(paymentNotFound(orderId)))
                .andExpect(jsonPath("$.uri").value(String.format(PAYMENT_URI, orderId)));

        verify(paymentService).updatePayment(orderId, userId, request);
        verifyNoMoreInteractions(paymentService);
    }

    @Test
    void updatePayment_whenPaymentMethodIsNull_returnsBadRequest() throws Exception {
        Long userId = 1L;
        Long orderId = 2L;

        PaymentRequest request = new PaymentRequest(null);

        authenticateUser(userId);

        mockMvc.perform(patch(String.format(PAYMENT_URI, orderId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.uri").value(String.format(PAYMENT_URI, orderId)))
                .andExpect(jsonPath("$.fieldErrors.paymentMethod",
                        containsInAnyOrder(paymentMethodRequired())));

        verifyNoInteractions(paymentService);
    }

    @Test
    void updatePayment_whenPaymentMethodIsInvalid_returnsBadRequest() throws Exception {
        Long userId = 1L;
        Long orderId = 2L;
        String method = "TESTING";

        String requestBody = """
            {
                "paymentMethod": "%s"
            }
            """.formatted(method);

        authenticateUser(userId);

        mockMvc.perform(patch(String.format(PAYMENT_URI, orderId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.uri").value(String.format(PAYMENT_URI, orderId)))
                .andExpect(jsonPath("$.fieldErrors.paymentMethod",
                        containsInAnyOrder(paymentMethodInvalid(method))));

        verifyNoInteractions(paymentService);
    }

    @Test
    void updatePayment_whenOrderNotFound_returnsNotFound() throws Exception {
        Long userId = 1L;
        Long orderId = 2L;
        PaymentMethod method = PaymentMethod.CARD;

        PaymentRequest request = new PaymentRequest(method);

        when(paymentService.updatePayment(orderId, userId, request))
                .thenThrow(new NoResourceFoundException(orderNotFound(orderId, userId)));

        authenticateUser(userId);

        mockMvc.perform(patch(String.format(PAYMENT_URI, orderId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.NOT_FOUND.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(orderNotFound(orderId, userId)))
                .andExpect(jsonPath("$.uri").value(String.format(PAYMENT_URI, orderId)));

        verify(paymentService).updatePayment(orderId, userId, request);
        verifyNoMoreInteractions(paymentService);
    }

    @Test
    void updatePayment_whenPaymentNotInPending_returnsBadRequest() throws Exception {
        Long userId = 1L;
        Long orderId = 2L;
        PaymentMethod method = PaymentMethod.CARD;

        PaymentRequest request = new PaymentRequest(method);

        when(paymentService.updatePayment(orderId, userId, request))
                .thenThrow(new InvalidPaymentStateException(paymentNotPending()));

        authenticateUser(userId);

        mockMvc.perform(patch(String.format(PAYMENT_URI, orderId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(paymentNotPending()))
                .andExpect(jsonPath("$.uri").value(String.format(PAYMENT_URI, orderId)));

        verify(paymentService).updatePayment(orderId, userId, request);
        verifyNoMoreInteractions(paymentService);
    }

    @Test
    void updatePayment_whenOrderIdIsInvalid_returnsBadRequest() throws Exception {
        Long userId = 1L;
        String orderId = INVALID_ID;
        PaymentMethod method = PaymentMethod.CARD;

        PaymentRequest request = new PaymentRequest(method);

        authenticateUser(userId);

        mockMvc.perform(patch(String.format(PAYMENT_URI, orderId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(invalidParameter("orderId")))
                .andExpect(jsonPath("$.uri").value(String.format(PAYMENT_URI, orderId)));

        verifyNoInteractions(paymentService);
    }

    @Test
    void confirmPayment_whenRequestIsValidWithSuccess_returnsPaymentResponse() throws Exception {
        Long userId = 1L;
        Long orderId = 2L;
        Long paymentId = 3L;
        BigDecimal amount = BigDecimal.valueOf(499.99);
        PaymentMethod method = PaymentMethod.CARD;
        PaymentStatus status = PaymentStatus.SUCCESS;

        PaymentResponse response = new PaymentResponse(
                paymentId,
                orderId,
                amount,
                method,
                status,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(paymentService.confirmPayment(orderId, userId, status))
                .thenReturn(response);

        authenticateUser(userId);

        mockMvc.perform(post(String.format(PAYMENT_URI + "/confirm", orderId))
                        .param("paymentStatus", status.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(paymentId))
                .andExpect(jsonPath("$.orderId").value(orderId))
                .andExpect(jsonPath("$.amount").value(amount.doubleValue()))
                .andExpect(jsonPath("$.paymentMethod").value(method.name()))
                .andExpect(jsonPath("$.paymentStatus").value(status.name()))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());

        ArgumentCaptor<PaymentStatus> captor = ArgumentCaptor.forClass(PaymentStatus.class);
        verify(paymentService).confirmPayment(eq(orderId), eq(userId), captor.capture());

        PaymentStatus capturedStatus = captor.getValue();
        assertThat(capturedStatus).isEqualTo(status);

        verifyNoMoreInteractions(paymentService);
    }

    @Test
    void confirmPayment_whenRequestIsValidWithFailed_returnsPaymentResponse() throws Exception {
        Long userId = 1L;
        Long orderId = 2L;
        Long paymentId = 3L;
        BigDecimal amount = BigDecimal.valueOf(499.99);
        PaymentMethod method = PaymentMethod.CARD;
        PaymentStatus status = PaymentStatus.FAILED;

        PaymentResponse response = new PaymentResponse(
                paymentId,
                orderId,
                amount,
                method,
                status,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(paymentService.confirmPayment(orderId, userId, status))
                .thenReturn(response);

        authenticateUser(userId);

        mockMvc.perform(post(String.format(PAYMENT_URI + "/confirm", orderId))
                        .param("paymentStatus", status.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(paymentId))
                .andExpect(jsonPath("$.orderId").value(orderId))
                .andExpect(jsonPath("$.amount").value(amount.doubleValue()))
                .andExpect(jsonPath("$.paymentMethod").value(method.name()))
                .andExpect(jsonPath("$.paymentStatus").value(status.name()))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());

        ArgumentCaptor<PaymentStatus> captor = ArgumentCaptor.forClass(PaymentStatus.class);
        verify(paymentService).confirmPayment(eq(orderId), eq(userId), captor.capture());

        PaymentStatus capturedStatus = captor.getValue();
        assertThat(capturedStatus).isEqualTo(status);

        verifyNoMoreInteractions(paymentService);
    }

    @Test
    void confirmPayment_whenPaymentNotFound_returnsNotFound() throws Exception {
        Long userId = 1L;
        Long orderId = 2L;
        PaymentMethod method = PaymentMethod.CARD;
        PaymentStatus status = PaymentStatus.SUCCESS;


        when(paymentService.confirmPayment(orderId, userId, status))
                .thenThrow(new NoResourceFoundException(paymentNotFound(orderId)));

        authenticateUser(userId);

        mockMvc.perform(post(String.format(PAYMENT_URI + "/confirm", orderId))
                        .param("paymentStatus", status.name()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.NOT_FOUND.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(paymentNotFound(orderId)))
                .andExpect(jsonPath("$.uri").value(String.format(PAYMENT_URI + "/confirm", orderId)));

        verify(paymentService).confirmPayment(orderId, userId, status);
        verifyNoMoreInteractions(paymentService);
    }

    @Test
    void confirmPayment_whenOrderNotFound_returnsNotFound() throws Exception {
        Long userId = 1L;
        Long orderId = 2L;
        PaymentStatus status = PaymentStatus.SUCCESS;

        when(paymentService.confirmPayment(orderId, userId, status))
                .thenThrow(new NoResourceFoundException(orderNotFound(orderId, userId)));

        authenticateUser(userId);

        mockMvc.perform(post(String.format(PAYMENT_URI + "/confirm", orderId))
                        .param("paymentStatus", status.name()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.NOT_FOUND.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(orderNotFound(orderId, userId)))
                .andExpect(jsonPath("$.uri").value(String.format(PAYMENT_URI + "/confirm", orderId)));

        verify(paymentService).confirmPayment(orderId, userId, status);
        verifyNoMoreInteractions(paymentService);
    }

    @Test
    void confirmPayment_whenOrderNotInPendingPayment_returnsBadRequest() throws Exception {
        Long userId = 1L;
        Long orderId = 2L;
        PaymentStatus status = PaymentStatus.SUCCESS;

        when(paymentService.confirmPayment(orderId, userId, status))
                .thenThrow(new InvalidOrderStateException(orderNotInPendingPayment()));

        authenticateUser(userId);

        mockMvc.perform(post(String.format(PAYMENT_URI + "/confirm", orderId))
                        .param("paymentStatus", status.name()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(orderNotInPendingPayment()))
                .andExpect(jsonPath("$.uri").value(String.format(PAYMENT_URI + "/confirm", orderId)));

        verify(paymentService).confirmPayment(orderId, userId, status);
        verifyNoMoreInteractions(paymentService);
    }

    @Test
    void confirmPayment_whenPaymentStatusNotInPending_returnsBadRequest() throws Exception {
        Long userId = 1L;
        Long orderId = 2L;
        PaymentStatus status = PaymentStatus.SUCCESS;

        when(paymentService.confirmPayment(orderId, userId, status))
                .thenThrow(new InvalidPaymentStateException(paymentNotPending()));

        authenticateUser(userId);

        mockMvc.perform(post(String.format(PAYMENT_URI + "/confirm", orderId))
                        .param("paymentStatus", status.name()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(paymentNotPending()))
                .andExpect(jsonPath("$.uri").value(String.format(PAYMENT_URI + "/confirm", orderId)));

        verify(paymentService).confirmPayment(orderId, userId, status);
        verifyNoMoreInteractions(paymentService);
    }

    @Test
    void confirmPayment_whenConfirmedStatusIsPending_returnsBadRequest() throws Exception {
        Long userId = 1L;
        Long orderId = 2L;
        PaymentStatus status = PaymentStatus.PENDING;

        when(paymentService.confirmPayment(orderId, userId, status))
                .thenThrow(new InvalidPaymentStateException(invalidStatusConfirmed()));

        authenticateUser(userId);

        mockMvc.perform(post(String.format(PAYMENT_URI + "/confirm", orderId))
                        .param("paymentStatus", status.name()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(invalidStatusConfirmed()))
                .andExpect(jsonPath("$.uri").value(String.format(PAYMENT_URI + "/confirm", orderId)));

        verify(paymentService).confirmPayment(orderId, userId, status);
        verifyNoMoreInteractions(paymentService);
    }

    @Test
    void confirmPayment_whenPaymentStatusParamIsInvalid_returnsBadRequest() throws Exception {
        Long userId = 1L;
        Long orderId = 2L;
        String status = "TESTING";

        authenticateUser(userId);

        mockMvc.perform(post(String.format(PAYMENT_URI + "/confirm", orderId))
                        .param("paymentStatus", status))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(paymentStatusInvalid(status)))
                .andExpect(jsonPath("$.uri").value(String.format(PAYMENT_URI + "/confirm", orderId)));

        verifyNoInteractions(paymentService);
    }

    @Test
    void confirmPayment_whenOrderIdIsInvalid_returnsBadRequest() throws Exception {
        Long userId = 1L;
        String orderId = INVALID_ID;
        PaymentStatus status = PaymentStatus.PENDING;

        authenticateUser(userId);

        mockMvc.perform(post(String.format(PAYMENT_URI + "/confirm", orderId))
                        .param("paymentStatus", status.name()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(invalidParameter("orderId")))
                .andExpect(jsonPath("$.uri").value(String.format(PAYMENT_URI + "/confirm", orderId)));

        verifyNoInteractions(paymentService);
    }

    @Test
    void confirmPayment_whenPaymentStatusParamIsMissing_returnsBadRequest() throws Exception {
        Long userId = 1L;
        Long orderId = 2L;

        authenticateUser(userId);

        mockMvc.perform(post(String.format(PAYMENT_URI + "/confirm", orderId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.uri").value(String.format(PAYMENT_URI + "/confirm", orderId)))
                .andExpect(jsonPath("$.fieldErrors.paymentStatus",
                        containsInAnyOrder(invalidParameter("paymentStatus"))));

        verifyNoInteractions(paymentService);
    }
}
