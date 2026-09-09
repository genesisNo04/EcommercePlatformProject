package com.namnguyen.ecommerce_platform.integration.payment;

import com.namnguyen.ecommerce_platform.integration.BaseIntegrationTest;
import com.namnguyen.ecommerce_platform.order.entity.Order;
import com.namnguyen.ecommerce_platform.order.enums.OrderStatus;
import com.namnguyen.ecommerce_platform.payment.dto.PaymentRequest;
import com.namnguyen.ecommerce_platform.payment.dto.PaymentResponse;
import com.namnguyen.ecommerce_platform.payment.entity.Payment;
import com.namnguyen.ecommerce_platform.payment.enums.PaymentMethod;
import com.namnguyen.ecommerce_platform.payment.enums.PaymentStatus;
import com.namnguyen.ecommerce_platform.user.entity.User;
import com.namnguyen.ecommerce_platform.user.enums.Role;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.List;

import static com.namnguyen.ecommerce_platform.testutil.MockAuthentication.*;
import static com.namnguyen.ecommerce_platform.testutil.TestDataFactory.*;
import static com.namnguyen.ecommerce_platform.testutil.messages.CommonTestMessages.VALIDATION_FAILED;
import static com.namnguyen.ecommerce_platform.testutil.messages.PaymentTestMessages.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class PaymentIntegrationTest extends BaseIntegrationTest {

    @Test
    void submitPayment_whenRequestIsValid_createsPaymentInDatabase() throws Exception {
        User user = persistDefaultCustomer();

        BigDecimal total = BigDecimal.valueOf(299.99);

        Order order = persistOrder(
                total,
                OrderStatus.PENDING_PAYMENT,
                user,
                List.of(),
                null
        );

        PaymentRequest paymentRequest = new PaymentRequest(PaymentMethod.CARD);

        authenticateUser(user.getId());

        MvcResult result = mockMvc.perform(post(paymentUri(order.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paymentRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentId").exists())
                .andExpect(jsonPath("$.orderId").value(order.getId()))
                .andExpect(jsonPath("$.amount").value(total.doubleValue()))
                .andExpect(jsonPath("$.paymentMethod").value(PaymentMethod.CARD.name()))
                .andExpect(jsonPath("$.paymentStatus").value(PaymentStatus.PENDING.name()))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists())
                .andReturn();

        PaymentResponse paymentResponse = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                PaymentResponse.class
        );

        Payment savedPayment = paymentRepository.findById(paymentResponse.paymentId()).orElseThrow();

        assertThat(paymentResponse.orderId()).isEqualTo(order.getId());
        assertThat(savedPayment.getAmount()).isEqualByComparingTo(total);
        assertThat(savedPayment.getPaymentMethod()).isEqualTo(PaymentMethod.CARD);
        assertThat(savedPayment.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    void submitPayment_whenPaymentAlreadyExists_returnsConflict() throws Exception {
        User user = persistDefaultCustomer();

        BigDecimal total = BigDecimal.valueOf(299.99);

        Order order = persistOrder(
                total,
                OrderStatus.PENDING_PAYMENT,
                user,
                List.of(),
                null
        );

        persistPayment(
                PaymentMethod.CARD,
                PaymentStatus.PENDING,
                order,
                total
        );

        PaymentRequest paymentRequest = new PaymentRequest(PaymentMethod.CARD);

        authenticateUser(user.getId());

        mockMvc.perform(post(paymentUri(order.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paymentRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(PAYMENT_ALREADY_EXISTS));

        assertThat(paymentRepository.count()).isEqualTo(1);
    }

    @Test
    void submitPayment_whenOrderIsNotPendingPayment_returnsBadRequest() throws Exception {
        User user = persistDefaultCustomer();
        BigDecimal total = BigDecimal.valueOf(299.99);

        Order order = persistOrder(
                total,
                OrderStatus.PAID,
                user,
                List.of(),
                null
        );

        PaymentRequest paymentRequest = new PaymentRequest(PaymentMethod.CARD);

        authenticateUser(user.getId());

        mockMvc.perform(post(paymentUri(order.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paymentRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(ORDER_NOT_PENDING_PAYMENT));

        assertThat(paymentRepository.count()).isZero();
    }

    @Test
    void submitPayment_whenOrderNotFound_returnsNotFound() throws Exception {
        User user = persistDefaultCustomer();
        long orderId = 999_999L;

        PaymentRequest paymentRequest = new PaymentRequest(PaymentMethod.CARD);

        authenticateUser(user.getId());

        mockMvc.perform(post(paymentUri(orderId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paymentRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(orderNotFoundWithIdAndUserId(orderId, user.getId())));

        assertThat(paymentRepository.count()).isZero();
    }

    @Test
    void submitPayment_whenPaymentMethodIsInvalid_returnsBadRequest() throws Exception {
        User user = persistDefaultCustomer();

        BigDecimal total = BigDecimal.valueOf(299.99);
        Order order = persistOrder(
                total,
                OrderStatus.PENDING_PAYMENT,
                user,
                List.of(),
                null
        );

        String requestBody = """
            {
                "paymentMethod": "%s"
            }
            """.formatted(INVALID_ENUM_VALUE);

        authenticateUser(user.getId());

        mockMvc.perform(post(paymentUri(order.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                .andExpect(jsonPath("$.fieldErrors.paymentMethod").value(paymentMethodIsInvalid(INVALID_ENUM_VALUE)));

        assertThat(paymentRepository.count()).isZero();
    }

    @Test
    void submitPayment_whenPaymentMethodIsNull_returnsBadRequest() throws Exception {
        User user = persistDefaultCustomer();

        BigDecimal total = BigDecimal.valueOf(299.99);
        Order order = persistOrder(
                total,
                OrderStatus.PENDING_PAYMENT,
                user,
                List.of(),
                null
        );

        String requestBody = """
            {
                "paymentMethod": null
            }
            """;

        authenticateUser(user.getId());

        mockMvc.perform(post(paymentUri(order.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                .andExpect(jsonPath("$.fieldErrors.paymentMethod").value(PAYMENT_METHOD_IS_REQUIRED));

        assertThat(paymentRepository.count()).isZero();
    }

    @Test
    void submitPayment_whenOrderBelongsToDifferentUser_returnsNotFound() throws Exception {
        User owner = persistDefaultCustomer();

        User otherUser = persistUser(
                "otheruser@gmail.com",
                VALID_PASSWORD,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                "9876543212",
                Role.CUSTOMER
        );

        BigDecimal total = BigDecimal.valueOf(299.99);

        Order order = persistOrder(
                total,
                OrderStatus.PENDING_PAYMENT,
                owner,
                List.of(),
                null
        );

        PaymentRequest paymentRequest =
                new PaymentRequest(PaymentMethod.CARD);

        authenticateUser(otherUser.getId());

        mockMvc.perform(post(paymentUri(order.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paymentRequest)))
                .andExpect(status().isNotFound());

        assertThat(paymentRepository.count()).isZero();
    }

    @Test
    void getPayment_whenPaymentExists_returnsPaymentResponse() throws Exception {
        User user = persistDefaultCustomer();

        BigDecimal total = BigDecimal.valueOf(299.99);
        Order order = persistOrder(
                total,
                OrderStatus.PENDING_PAYMENT,
                user,
                List.of(),
                null
        );

        persistPayment(
                PaymentMethod.CARD,
                PaymentStatus.PENDING,
                order,
                total
        );

        authenticateUser(user.getId());

        mockMvc.perform(get(paymentUri(order.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").exists())
                .andExpect(jsonPath("$.orderId").value(order.getId()))
                .andExpect(jsonPath("$.amount").value(total.doubleValue()))
                .andExpect(jsonPath("$.paymentMethod").value(PaymentMethod.CARD.name()))
                .andExpect(jsonPath("$.paymentStatus").value(PaymentStatus.PENDING.name()))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());
    }

    @Test
    void getPayment_whenOrderBelongsToDifferentUser_returnsNotFound() throws Exception {
        User user = persistDefaultCustomer();
        User otherUser = persistUser(
                "secondemail@gmail.com",
                VALID_PASSWORD,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                "9876543212",
                Role.CUSTOMER
        );

        BigDecimal total = BigDecimal.valueOf(299.99);
        Order order = persistOrder(
                total,
                OrderStatus.PENDING_PAYMENT,
                user,
                List.of(),
                null
        );

        persistPayment(
                PaymentMethod.CARD,
                PaymentStatus.PENDING,
                order,
                total
        );

        authenticateUser(otherUser.getId());

        mockMvc.perform(get(paymentUri(order.getId())))
                .andExpect(status().isNotFound());
    }

    @Test
    void getPayment_whenPaymentNotFound_returnsNotFound() throws Exception {
        User user = persistDefaultCustomer();

        BigDecimal total = BigDecimal.valueOf(299.99);
        Order order = persistOrder(
                total,
                OrderStatus.PENDING_PAYMENT,
                user,
                List.of(),
                null
        );

        authenticateUser(user.getId());

        mockMvc.perform(get(paymentUri(order.getId())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(paymentNotFoundWithOrderId(order.getId())));
    }

    @Test
    void updatePayment_whenPaymentExists_returnsPaymentResponse() throws Exception {
        User user = persistDefaultCustomer();

        BigDecimal total = BigDecimal.valueOf(299.99);
        Order order = persistOrder(
                total,
                OrderStatus.PENDING_PAYMENT,
                user,
                List.of(),
                null
        );

        persistPayment(
                PaymentMethod.CARD,
                PaymentStatus.PENDING,
                order,
                total
        );

        PaymentRequest paymentRequest = new PaymentRequest(PaymentMethod.PAYPAL);

        authenticateUser(user.getId());

        mockMvc.perform(patch(paymentUri(order.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paymentRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").exists())
                .andExpect(jsonPath("$.orderId").value(order.getId()))
                .andExpect(jsonPath("$.amount").value(total.doubleValue()))
                .andExpect(jsonPath("$.paymentMethod").value(PaymentMethod.PAYPAL.name()))
                .andExpect(jsonPath("$.paymentStatus").value(PaymentStatus.PENDING.name()))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());

        Payment savedPayment = paymentRepository.findByOrderId(order.getId()).orElseThrow();

        assertThat(savedPayment.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(savedPayment.getPaymentMethod()).isEqualTo(PaymentMethod.PAYPAL);
    }

    @Test
    void updatePayment_whenPaymentNotFound_returnsNotFound() throws Exception {
        User user = persistDefaultCustomer();

        BigDecimal total = BigDecimal.valueOf(299.99);
        Order order = persistOrder(
                total,
                OrderStatus.PENDING_PAYMENT,
                user,
                List.of(),
                null
        );

        PaymentRequest paymentRequest = new PaymentRequest(PaymentMethod.PAYPAL);

        authenticateUser(user.getId());

        mockMvc.perform(patch(paymentUri(order.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paymentRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(paymentNotFoundWithOrderId(order.getId())));
    }

    @Test
    void updatePayment_whenPaymentMethodIsInvalid_returnsBadRequest() throws Exception {
        User user = persistDefaultCustomer();

        BigDecimal total = BigDecimal.valueOf(299.99);
        Order order = persistOrder(
                total,
                OrderStatus.PENDING_PAYMENT,
                user,
                List.of(),
                null
        );

        persistPayment(
                PaymentMethod.CARD,
                PaymentStatus.PENDING,
                order,
                total
        );
        
        String requestBody = """
                {
                    "paymentMethod": "%s"
                }
                """.formatted(INVALID_ENUM_VALUE);

        authenticateUser(user.getId());

        mockMvc.perform(patch(paymentUri(order.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                .andExpect(jsonPath("$.fieldErrors.paymentMethod").value(paymentMethodIsInvalid(INVALID_ENUM_VALUE)));

        Payment savedPayment = paymentRepository.findByOrderId(order.getId()).orElseThrow();

        assertThat(savedPayment.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(savedPayment.getPaymentMethod()).isEqualTo(PaymentMethod.CARD);
    }

    @Test
    void updatePayment_whenPaymentIsNotPending_returnsBadRequest() throws Exception {
        User user = persistDefaultCustomer();

        BigDecimal total = BigDecimal.valueOf(299.99);
        Order order = persistOrder(
                total,
                OrderStatus.PENDING_PAYMENT,
                user,
                List.of(),
                null
        );

        persistPayment(
                PaymentMethod.CARD,
                PaymentStatus.SUCCESS,
                order,
                total
        );

        PaymentRequest paymentRequest = new PaymentRequest(PaymentMethod.PAYPAL);

        authenticateUser(user.getId());

        mockMvc.perform(patch(paymentUri(order.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paymentRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(PAYMENT_NOT_PENDING));

        Payment savedPayment = paymentRepository.findByOrderId(order.getId()).orElseThrow();

        assertThat(savedPayment.getPaymentStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(savedPayment.getPaymentMethod()).isEqualTo(PaymentMethod.CARD);
    }

    @Test
    void updatePayment_whenOrderBelongsToDifferentUser_returnsNotFound() throws Exception {
        User user = persistDefaultCustomer();
        User otherUser = persistUser(
                "email@gmail.com",
                VALID_PASSWORD,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                "98765432123",
                Role.CUSTOMER
        );

        BigDecimal total = BigDecimal.valueOf(299.99);
        Order order = persistOrder(
                total,
                OrderStatus.PENDING_PAYMENT,
                user,
                List.of(),
                null
        );

        persistPayment(
                PaymentMethod.CARD,
                PaymentStatus.PENDING,
                order,
                total
        );

        PaymentRequest paymentRequest = new PaymentRequest(PaymentMethod.CARD);

        authenticateUser(otherUser.getId());

        mockMvc.perform(patch(paymentUri(order.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paymentRequest)))
                .andExpect(status().isNotFound());

        Payment savedPayment =
                paymentRepository.findByOrderId(order.getId()).orElseThrow();

        assertThat(savedPayment.getPaymentMethod())
                .isEqualTo(PaymentMethod.CARD);

        assertThat(savedPayment.getPaymentStatus())
                .isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    void confirmPayment_whenRequestIsValid_returnsPaymentResponse() throws Exception {
        User user = persistDefaultCustomer();

        BigDecimal total = BigDecimal.valueOf(299.99);
        Order order = persistOrder(
                total,
                OrderStatus.PENDING_PAYMENT,
                user,
                List.of(),
                null
        );

        persistPayment(
                PaymentMethod.CARD,
                PaymentStatus.PENDING,
                order,
                total
        );

        authenticateUser(user.getId());

        mockMvc.perform(post(paymentConfirmUri(order.getId()))
                        .param("paymentStatus", PaymentStatus.SUCCESS.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").exists())
                .andExpect(jsonPath("$.orderId").value(order.getId()))
                .andExpect(jsonPath("$.amount").value(total.doubleValue()))
                .andExpect(jsonPath("$.paymentMethod").value(PaymentMethod.CARD.name()))
                .andExpect(jsonPath("$.paymentStatus").value(PaymentStatus.SUCCESS.name()))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());

        Payment savedPayment = paymentRepository.findByOrderId(order.getId()).orElseThrow();

        assertThat(savedPayment.getPaymentStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(savedPayment.getPaymentMethod()).isEqualTo(PaymentMethod.CARD);

        Order savedOrder = orderRepository.findById(order.getId()).orElseThrow();

        assertThat(savedOrder.getStatus()).isEqualTo(OrderStatus.PAID);
    }

    @Test
    void confirmPayment_whenPaymentFails_updatesPaymentButDoesNotMarkOrderPaid() throws Exception {
        User user = persistDefaultCustomer();

        BigDecimal total = BigDecimal.valueOf(299.99);
        Order order = persistOrder(
                total,
                OrderStatus.PENDING_PAYMENT,
                user,
                List.of(),
                null
        );

        persistPayment(
                PaymentMethod.CARD,
                PaymentStatus.PENDING,
                order,
                total
        );

        authenticateUser(user.getId());

        mockMvc.perform(post(paymentConfirmUri(order.getId()))
                        .param("paymentStatus", PaymentStatus.FAILED.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").exists())
                .andExpect(jsonPath("$.orderId").value(order.getId()))
                .andExpect(jsonPath("$.amount").value(total.doubleValue()))
                .andExpect(jsonPath("$.paymentMethod").value(PaymentMethod.CARD.name()))
                .andExpect(jsonPath("$.paymentStatus").value(PaymentStatus.FAILED.name()))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());

        Payment savedPayment = paymentRepository.findByOrderId(order.getId()).orElseThrow();

        assertThat(savedPayment.getPaymentStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(savedPayment.getPaymentMethod()).isEqualTo(PaymentMethod.CARD);

        Order savedOrder = orderRepository.findById(order.getId()).orElseThrow();

        assertThat(savedOrder.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
    }

    @Test
    void confirmPayment_whenPaymentStatusIsInvalid_returnsBadRequest() throws Exception {
        User user = persistDefaultCustomer();

        BigDecimal total = BigDecimal.valueOf(299.99);
        Order order = persistOrder(
                total,
                OrderStatus.PENDING_PAYMENT,
                user,
                List.of(),
                null
        );

        persistPayment(
                PaymentMethod.CARD,
                PaymentStatus.PENDING,
                order,
                total
        );

        String invalidValue = INVALID_ENUM_VALUE;

        authenticateUser(user.getId());

        mockMvc.perform(post(paymentConfirmUri(order.getId()))
                        .param("paymentStatus", invalidValue))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(paymentStatusIsInvalid(invalidValue)));

        Payment savedPayment = paymentRepository.findByOrderId(order.getId()).orElseThrow();

        assertThat(savedPayment.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(savedPayment.getPaymentMethod()).isEqualTo(PaymentMethod.CARD);
    }

    @Test
    void confirmPayment_whenPaymentIsNotPending_returnsBadRequest() throws Exception {
        User user = persistDefaultCustomer();

        BigDecimal total = BigDecimal.valueOf(299.99);
        Order order = persistOrder(
                total,
                OrderStatus.PENDING_PAYMENT,
                user,
                List.of(),
                null
        );

        persistPayment(
                PaymentMethod.CARD,
                PaymentStatus.SUCCESS,
                order,
                total
        );

        authenticateUser(user.getId());

        mockMvc.perform(post(paymentConfirmUri(order.getId()))
                        .param("paymentStatus", PaymentStatus.FAILED.name()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(PAYMENT_CANNOT_BE_CONFIRMED));

        Payment savedPayment = paymentRepository.findByOrderId(order.getId()).orElseThrow();

        assertThat(savedPayment.getPaymentMethod()).isEqualTo(PaymentMethod.CARD);
        assertThat(savedPayment.getPaymentStatus()).isEqualTo(PaymentStatus.SUCCESS);

        Order savedOrder = orderRepository.findById(order.getId()).orElseThrow();

        assertThat(savedOrder.getStatus())
                .isEqualTo(OrderStatus.PENDING_PAYMENT);
    }

    @Test
    void confirmPayment_whenPaymentNotFound_returnsNotFound() throws Exception {
        User user = persistDefaultCustomer();

        BigDecimal total = BigDecimal.valueOf(299.99);
        Order order = persistOrder(
                total,
                OrderStatus.PENDING_PAYMENT,
                user,
                List.of(),
                null
        );

        authenticateUser(user.getId());

        mockMvc.perform(post(paymentConfirmUri(order.getId()))
                        .param("paymentStatus", PaymentStatus.SUCCESS.name()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(paymentNotFoundWithOrderId(order.getId())));
    }

    @Test
    void confirmPayment_whenRequestedStatusIsPending_returnsBadRequest() throws Exception {
        User user = persistDefaultCustomer();

        BigDecimal total = BigDecimal.valueOf(299.99);

        Order order = persistOrder(
                total,
                OrderStatus.PENDING_PAYMENT,
                user,
                List.of(),
                null
        );

        persistPayment(
                PaymentMethod.CARD,
                PaymentStatus.PENDING,
                order,
                total
        );

        authenticateUser(user.getId());

        mockMvc.perform(post(paymentConfirmUri(order.getId()))
                        .param("paymentStatus", PaymentStatus.PENDING.name()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(INVALID_PAYMENT_STATUS));

        Payment savedPayment =
                paymentRepository.findByOrderId(order.getId()).orElseThrow();

        Order savedOrder =
                orderRepository.findById(order.getId()).orElseThrow();

        assertThat(savedPayment.getPaymentStatus())
                .isEqualTo(PaymentStatus.PENDING);

        assertThat(savedOrder.getStatus())
                .isEqualTo(OrderStatus.PENDING_PAYMENT);
    }

    @Test
    void confirmPayment_whenOrderIsNotPendingPayment_returnsBadRequest() throws Exception {
        User user = persistDefaultCustomer();

        BigDecimal total = BigDecimal.valueOf(299.99);

        Order order = persistOrder(
                total,
                OrderStatus.PAID,
                user,
                List.of(),
                null
        );

        persistPayment(
                PaymentMethod.CARD,
                PaymentStatus.PENDING,
                order,
                total
        );

        authenticateUser(user.getId());

        mockMvc.perform(post(paymentConfirmUri(order.getId()))
                        .param("paymentStatus", PaymentStatus.SUCCESS.name()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(ORDER_NOT_PENDING_PAYMENT));

        Payment savedPayment =
                paymentRepository.findByOrderId(order.getId()).orElseThrow();

        Order savedOrder =
                orderRepository.findById(order.getId()).orElseThrow();

        assertThat(savedPayment.getPaymentStatus())
                .isEqualTo(PaymentStatus.PENDING);

        assertThat(savedOrder.getStatus())
                .isEqualTo(OrderStatus.PAID);
    }

    @Test
    void confirmPayment_whenOrderBelongsToDifferentUser_returnsNotFound() throws Exception {
        User owner = persistDefaultCustomer();

        User otherUser = persistUser(
                "otheruser@gmail.com",
                VALID_PASSWORD,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                "9876543212",
                Role.CUSTOMER
        );

        BigDecimal total = BigDecimal.valueOf(299.99);

        Order order = persistOrder(
                total,
                OrderStatus.PENDING_PAYMENT,
                owner,
                List.of(),
                null
        );

        persistPayment(
                PaymentMethod.CARD,
                PaymentStatus.PENDING,
                order,
                total
        );

        authenticateUser(otherUser.getId());

        mockMvc.perform(post(paymentConfirmUri(order.getId()))
                        .param("paymentStatus", PaymentStatus.SUCCESS.name()))
                .andExpect(status().isNotFound());

        Payment savedPayment =
                paymentRepository.findByOrderId(order.getId()).orElseThrow();

        Order savedOrder =
                orderRepository.findById(order.getId()).orElseThrow();

        assertThat(savedPayment.getPaymentStatus())
                .isEqualTo(PaymentStatus.PENDING);

        assertThat(savedOrder.getStatus())
                .isEqualTo(OrderStatus.PENDING_PAYMENT);
    }
}
