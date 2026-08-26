package com.namnguyen.ecommerce_platform.integration.security;

import com.namnguyen.ecommerce_platform.integration.BaseSecurityIntegrationTest;
import com.namnguyen.ecommerce_platform.order.entity.Order;
import com.namnguyen.ecommerce_platform.order.enums.OrderStatus;
import com.namnguyen.ecommerce_platform.payment.dto.PaymentRequest;
import com.namnguyen.ecommerce_platform.payment.enums.PaymentMethod;
import com.namnguyen.ecommerce_platform.payment.enums.PaymentStatus;
import com.namnguyen.ecommerce_platform.user.entity.User;
import com.namnguyen.ecommerce_platform.user.enums.Role;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.util.List;

import static com.namnguyen.ecommerce_platform.testutil.TestDataFactory.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class PaymentSecurityIntegrationTest extends BaseSecurityIntegrationTest {

    @Test
    void getPayment_withJwt_returnsOk() throws Exception {
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

        String token = loginAndGetToken(user.getEmail(), VALID_PASSWORD);

        mockMvc.perform(get(String.format(PAYMENT_URI, order.getId()))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void getPayment_withoutJwt_returnsUnauthorized() throws Exception {
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

        mockMvc.perform(get(String.format(PAYMENT_URI, order.getId())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getPayment_withInvalidJwt_returnsUnauthorized() throws Exception {
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

        String token = loginAndGetToken(user.getEmail(), VALID_PASSWORD) + "abc";

        mockMvc.perform(get(String.format(PAYMENT_URI, order.getId()))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getPayment_withOtherUserJwt_returnsNotFound() throws Exception {
        User user = createDefaultCustomer();

        User otherUser = createUser(
                "userother@gmail.com",
                "test123456789",
                "other",
                "user",
                "1234567897",
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

        String token = loginAndGetToken(otherUser.getEmail(), VALID_PASSWORD);

        mockMvc.perform(get(String.format(PAYMENT_URI, order.getId()))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void updatePayment_withJwt_returnsOk() throws Exception {
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

        String token = loginAndGetToken(user.getEmail(), VALID_PASSWORD);

        mockMvc.perform(patch(String.format(PAYMENT_URI, order.getId()))
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void updatePayment_withoutJwt_returnsUnauthorized() throws Exception {
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

        mockMvc.perform(patch(String.format(PAYMENT_URI, order.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void submitPayment_withJwt_returnsCreated() throws Exception {
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

        String token = loginAndGetToken(user.getEmail(), VALID_PASSWORD);

        mockMvc.perform(post(String.format(PAYMENT_URI, order.getId()))
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void submitPayment_withoutJwt_returnsUnauthorized() throws Exception {
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

        mockMvc.perform(post(String.format(PAYMENT_URI, order.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void confirmPayment_withJwt_returnsOk() throws Exception {
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

        String token = loginAndGetToken(user.getEmail(), VALID_PASSWORD);

        mockMvc.perform(post(String.format(PAYMENT_URI, order.getId()) + "/confirm")
                        .header("Authorization", "Bearer " + token)
                        .param("paymentStatus", PaymentStatus.SUCCESS.name()))
                .andExpect(status().isOk());
    }

    @Test
    void confirmPayment_withoutJwt_returnsUnauthorized() throws Exception {
        mockMvc.perform(post(String.format(PAYMENT_URI, 1L) + "/confirm")
                        .param("paymentStatus", PaymentStatus.SUCCESS.name()))
                .andExpect(status().isUnauthorized());
    }
}
