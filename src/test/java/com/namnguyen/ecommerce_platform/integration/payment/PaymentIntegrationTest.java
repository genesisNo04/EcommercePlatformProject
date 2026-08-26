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
import static com.namnguyen.ecommerce_platform.testutil.TestMessages.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class PaymentIntegrationTest extends BaseIntegrationTest {

    @Test
    void submitPayment_withValidRequest_returnsPaymentResponse() throws Exception {
        User user = createDefaultCustomer();

        BigDecimal total = BigDecimal.valueOf(299.99);

        Order order = createOrder(
                total,
                OrderStatus.PENDING_PAYMENT,
                user,
                List.of(),
                null
        );

        PaymentRequest request = new PaymentRequest(PaymentMethod.CARD);

        authenticateUser(user.getId());

        MvcResult result = mockMvc.perform(post(String.format(PAYMENT_URI, order.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentId").exists())
                .andExpect(jsonPath("$.orderId").value(order.getId()))
                .andExpect(jsonPath("$.amount").value(total.doubleValue()))
                .andExpect(jsonPath("$.paymentMethod").value(PaymentMethod.CARD.name()))
                .andExpect(jsonPath("$.paymentStatus").value(PaymentStatus.PENDING.name()))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists())
                .andReturn();

        PaymentResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                PaymentResponse.class
        );

        Payment savedPayment = paymentRepository.findById(response.paymentId()).orElseThrow();

        assertThat(response.orderId()).isEqualTo(order.getId());
        assertThat(savedPayment.getAmount()).isEqualByComparingTo(total);
        assertThat(savedPayment.getPaymentMethod()).isEqualTo(PaymentMethod.CARD);
        assertThat(savedPayment.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    void submitPayment_whenPaymentAlreadyExists_returnsConflict() throws Exception {
        User user = createDefaultCustomer();

        BigDecimal total = BigDecimal.valueOf(299.99);

        Order order = createOrder(
                total,
                OrderStatus.PENDING_PAYMENT,
                user,
                List.of(),
                null
        );

        createPayment(
                PaymentMethod.CARD,
                PaymentStatus.PENDING,
                order,
                total
        );

        PaymentRequest request = new PaymentRequest(PaymentMethod.CARD);

        authenticateUser(user.getId());

        mockMvc.perform(post(String.format(PAYMENT_URI, order.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(paymentDuplicate()));

        assertThat(paymentRepository.count()).isEqualTo(1);
    }

    @Test
    void submitPayment_whenOrderNotPendingPayment_returnsBadRequest() throws Exception {
        User user = createDefaultCustomer();
        BigDecimal total = BigDecimal.valueOf(299.99);

        Order order = createOrder(
                total,
                OrderStatus.PAID,
                user,
                List.of(),
                null
        );

        PaymentRequest request = new PaymentRequest(PaymentMethod.CARD);

        authenticateUser(user.getId());

        mockMvc.perform(post(String.format(PAYMENT_URI, order.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(orderNotInPendingPayment()));

        assertThat(paymentRepository.count()).isZero();
    }

    @Test
    void submitPayment_whenOrderNotFound_returnsNotFound() throws Exception {
        User user = createDefaultCustomer();
        long orderId = 999_999L;

        PaymentRequest request = new PaymentRequest(PaymentMethod.CARD);

        authenticateUser(user.getId());

        mockMvc.perform(post(String.format(PAYMENT_URI, orderId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(orderNotFound(orderId, user.getId())));

        assertThat(paymentRepository.count()).isZero();
    }

    @Test
    void submitPayment_whenPaymentMethodIsInvalid_returnsBadRequest() throws Exception {
        User user = createDefaultCustomer();

        BigDecimal total = BigDecimal.valueOf(299.99);
        Order order = createOrder(
                total,
                OrderStatus.PENDING_PAYMENT,
                user,
                List.of(),
                null
        );

        String method = "TESTING";

        String requestBody = """
            {
                "paymentMethod": "%s"
            }
            """.formatted(method);

        authenticateUser(user.getId());

        mockMvc.perform(post(String.format(PAYMENT_URI, order.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.fieldErrors.paymentMethod").value(paymentMethodInvalid(method)));

        assertThat(paymentRepository.count()).isZero();
    }

    @Test
    void submitPayment_whenPaymentMethodIsNull_returnsBadRequest() throws Exception {
        User user = createDefaultCustomer();

        BigDecimal total = BigDecimal.valueOf(299.99);
        Order order = createOrder(
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

        mockMvc.perform(post(String.format(PAYMENT_URI, order.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.fieldErrors.paymentMethod").value(paymentMethodRequired()));

        assertThat(paymentRepository.count()).isZero();
    }

    @Test
    void getPayment_whenPaymentExists_returnsPaymentResponse() throws Exception {
        User user = createDefaultCustomer();

        BigDecimal total = BigDecimal.valueOf(299.99);
        Order order = createOrder(
                total,
                OrderStatus.PENDING_PAYMENT,
                user,
                List.of(),
                null
        );

        createPayment(
                PaymentMethod.CARD,
                PaymentStatus.PENDING,
                order,
                total
        );

        authenticateUser(user.getId());

        mockMvc.perform(get(String.format(PAYMENT_URI, order.getId())))
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
        User user = createDefaultCustomer();
        User otherUser = createUser(
                "email@gmail.com",
                "test123456789",
                "userother",
                "testother",
                "user",
                Role.CUSTOMER
        );

        BigDecimal total = BigDecimal.valueOf(299.99);
        Order order = createOrder(
                total,
                OrderStatus.PENDING_PAYMENT,
                user,
                List.of(),
                null
        );

        createPayment(
                PaymentMethod.CARD,
                PaymentStatus.PENDING,
                order,
                total
        );

        authenticateUser(otherUser.getId());

        mockMvc.perform(get(String.format(PAYMENT_URI, order.getId())))
                .andExpect(status().isNotFound());
    }

    @Test
    void getPayment_whenPaymentNotFound_returnsNotFound() throws Exception {
        User user = createDefaultCustomer();

        BigDecimal total = BigDecimal.valueOf(299.99);
        Order order = createOrder(
                total,
                OrderStatus.PENDING_PAYMENT,
                user,
                List.of(),
                null
        );

        authenticateUser(user.getId());

        mockMvc.perform(get(String.format(PAYMENT_URI, order.getId())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(paymentNotFound(order.getId())));
    }

    @Test
    void updatePayment_whenPaymentExists_returnsPaymentResponse() throws Exception {
        User user = createDefaultCustomer();

        BigDecimal total = BigDecimal.valueOf(299.99);
        Order order = createOrder(
                total,
                OrderStatus.PENDING_PAYMENT,
                user,
                List.of(),
                null
        );

        createPayment(
                PaymentMethod.CARD,
                PaymentStatus.PENDING,
                order,
                total
        );

        PaymentRequest request = new PaymentRequest(PaymentMethod.PAYPAL);

        authenticateUser(user.getId());

        mockMvc.perform(patch(String.format(PAYMENT_URI, order.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
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
        User user = createDefaultCustomer();

        BigDecimal total = BigDecimal.valueOf(299.99);
        Order order = createOrder(
                total,
                OrderStatus.PENDING_PAYMENT,
                user,
                List.of(),
                null
        );

        PaymentRequest request = new PaymentRequest(PaymentMethod.PAYPAL);

        authenticateUser(user.getId());

        mockMvc.perform(patch(String.format(PAYMENT_URI, order.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(paymentNotFound(order.getId())));
    }

    @Test
    void updatePayment_whenPaymentMethodIsInvalid_returnsBadRequest() throws Exception {
        User user = createDefaultCustomer();

        BigDecimal total = BigDecimal.valueOf(299.99);
        Order order = createOrder(
                total,
                OrderStatus.PENDING_PAYMENT,
                user,
                List.of(),
                null
        );

        createPayment(
                PaymentMethod.CARD,
                PaymentStatus.PENDING,
                order,
                total
        );

        String invalidValue = "TESTING";
        String requestBody = """
                {
                    "paymentMethod": "%s"
                }
                """.formatted(invalidValue);

        authenticateUser(user.getId());

        mockMvc.perform(patch(String.format(PAYMENT_URI, order.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.fieldErrors.paymentMethod").value(paymentMethodInvalid(invalidValue)));

        Payment savedPayment = paymentRepository.findByOrderId(order.getId()).orElseThrow();

        assertThat(savedPayment.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(savedPayment.getPaymentMethod()).isEqualTo(PaymentMethod.CARD);
    }

    @Test
    void updatePayment_whenPaymentIsNotPending_returnsBadRequest() throws Exception {
        User user = createDefaultCustomer();

        BigDecimal total = BigDecimal.valueOf(299.99);
        Order order = createOrder(
                total,
                OrderStatus.PENDING_PAYMENT,
                user,
                List.of(),
                null
        );

        createPayment(
                PaymentMethod.CARD,
                PaymentStatus.SUCCESS,
                order,
                total
        );

        PaymentRequest request = new PaymentRequest(PaymentMethod.PAYPAL);

        authenticateUser(user.getId());

        mockMvc.perform(patch(String.format(PAYMENT_URI, order.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(paymentNotPending()));

        Payment savedPayment = paymentRepository.findByOrderId(order.getId()).orElseThrow();

        assertThat(savedPayment.getPaymentStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(savedPayment.getPaymentMethod()).isEqualTo(PaymentMethod.CARD);
    }

    @Test
    void updatePayment_whenOrderBelongsToDifferentUser_returnsNotFound() throws Exception {
        User user = createDefaultCustomer();
        User otherUser = createUser(
                "email@gmail.com",
                "test123456789",
                "userother",
                "testother",
                "user",
                Role.CUSTOMER
        );

        BigDecimal total = BigDecimal.valueOf(299.99);
        Order order = createOrder(
                total,
                OrderStatus.PENDING_PAYMENT,
                user,
                List.of(),
                null
        );

        createPayment(
                PaymentMethod.CARD,
                PaymentStatus.PENDING,
                order,
                total
        );

        PaymentRequest request = new PaymentRequest(PaymentMethod.CARD);

        authenticateUser(otherUser.getId());

        mockMvc.perform(patch(String.format(PAYMENT_URI, order.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void confirmPayment_whenRequestIsValid_returnsPaymentResponse() throws Exception {
        User user = createDefaultCustomer();

        BigDecimal total = BigDecimal.valueOf(299.99);
        Order order = createOrder(
                total,
                OrderStatus.PENDING_PAYMENT,
                user,
                List.of(),
                null
        );

        createPayment(
                PaymentMethod.CARD,
                PaymentStatus.PENDING,
                order,
                total
        );

        authenticateUser(user.getId());

        mockMvc.perform(post(String.format(PAYMENT_URI, order.getId()) + "/confirm")
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
        User user = createDefaultCustomer();

        BigDecimal total = BigDecimal.valueOf(299.99);
        Order order = createOrder(
                total,
                OrderStatus.PENDING_PAYMENT,
                user,
                List.of(),
                null
        );

        createPayment(
                PaymentMethod.CARD,
                PaymentStatus.PENDING,
                order,
                total
        );

        authenticateUser(user.getId());

        mockMvc.perform(post(String.format(PAYMENT_URI, order.getId()) + "/confirm")
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
        User user = createDefaultCustomer();

        BigDecimal total = BigDecimal.valueOf(299.99);
        Order order = createOrder(
                total,
                OrderStatus.PENDING_PAYMENT,
                user,
                List.of(),
                null
        );

        createPayment(
                PaymentMethod.CARD,
                PaymentStatus.PENDING,
                order,
                total
        );

        String invalidValue = "TESTING";

        authenticateUser(user.getId());

        mockMvc.perform(post(String.format(PAYMENT_URI, order.getId()) + "/confirm")
                        .param("paymentStatus", invalidValue))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(paymentStatusInvalid(invalidValue)));

        Payment savedPayment = paymentRepository.findByOrderId(order.getId()).orElseThrow();

        assertThat(savedPayment.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(savedPayment.getPaymentMethod()).isEqualTo(PaymentMethod.CARD);
    }

    @Test
    void confirmPayment_whenPaymentIsNotPending_returnsBadRequest() throws Exception {
        User user = createDefaultCustomer();

        BigDecimal total = BigDecimal.valueOf(299.99);
        Order order = createOrder(
                total,
                OrderStatus.PENDING_PAYMENT,
                user,
                List.of(),
                null
        );

        createPayment(
                PaymentMethod.CARD,
                PaymentStatus.SUCCESS,
                order,
                total
        );

        authenticateUser(user.getId());

        mockMvc.perform(post(String.format(PAYMENT_URI, order.getId()) + "/confirm")
                        .param("paymentStatus", PaymentStatus.FAILED.name()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(paymentCannotConfirmed()));

        Payment savedPayment = paymentRepository.findByOrderId(order.getId()).orElseThrow();

        assertThat(savedPayment.getPaymentMethod()).isEqualTo(PaymentMethod.CARD);
        assertThat(savedPayment.getPaymentStatus()).isEqualTo(PaymentStatus.SUCCESS);
    }

    @Test
    void confirmPayment_whenPaymentNotFound_returnsNotFound() throws Exception {
        User user = createDefaultCustomer();

        BigDecimal total = BigDecimal.valueOf(299.99);
        Order order = createOrder(
                total,
                OrderStatus.PENDING_PAYMENT,
                user,
                List.of(),
                null
        );

        authenticateUser(user.getId());

        mockMvc.perform(post(String.format(PAYMENT_URI, order.getId()) + "/confirm")
                        .param("paymentStatus", PaymentStatus.SUCCESS.name()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(paymentNotFound(order.getId())));
    }
}
