package com.namnguyen.ecommerce_platform.integration.security;

import com.namnguyen.ecommerce_platform.integration.BaseSecurityIntegrationTest;
import com.namnguyen.ecommerce_platform.order.entity.Order;
import com.namnguyen.ecommerce_platform.order.enums.OrderStatus;
import com.namnguyen.ecommerce_platform.payment.dto.PaymentRequest;
import com.namnguyen.ecommerce_platform.payment.enums.PaymentMethod;
import com.namnguyen.ecommerce_platform.payment.enums.PaymentStatus;
import com.namnguyen.ecommerce_platform.user.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.util.List;

import static com.namnguyen.ecommerce_platform.testutil.TestDataFactory.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class PaymentSecurityIntegrationTest extends BaseSecurityIntegrationTest {

    @Test
    void getPayment_withCustomerJwt_returnsOk() throws Exception {
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

        String token = loginAndGetToken(user.getEmail(), VALID_PASSWORD);

        mockMvc.perform(get(paymentUri(order.getId()))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void getPayment_withoutJwt_returnsUnauthorized() throws Exception {
        mockMvc.perform(get(paymentUri(1L)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updatePayment_withCustomerJwt_returnsOk() throws Exception {
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

        String token = loginAndGetToken(user.getEmail(), VALID_PASSWORD);

        mockMvc.perform(patch(paymentUri(order.getId()))
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paymentRequest)))
                .andExpect(status().isOk());
    }

    @Test
    void updatePayment_withoutJwt_returnsUnauthorized() throws Exception {
        PaymentRequest paymentRequest =
                new PaymentRequest(PaymentMethod.PAYPAL);

        mockMvc.perform(patch(paymentUri(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paymentRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void submitPayment_withCustomerJwt_returnsCreated() throws Exception {
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

        String token = loginAndGetToken(user.getEmail(), VALID_PASSWORD);

        mockMvc.perform(post(paymentUri(order.getId()))
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paymentRequest)))
                .andExpect(status().isCreated());
    }

    @Test
    void submitPayment_withoutJwt_returnsUnauthorized() throws Exception {
        PaymentRequest paymentRequest =
                new PaymentRequest(PaymentMethod.PAYPAL);

        mockMvc.perform(post(paymentUri(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paymentRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void confirmPayment_withCustomerJwt_returnsOk() throws Exception {
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

        String token = loginAndGetToken(user.getEmail(), VALID_PASSWORD);

        mockMvc.perform(post(paymentConfirmUri(order.getId()))
                        .header("Authorization", "Bearer " + token)
                        .param("paymentStatus", PaymentStatus.SUCCESS.name()))
                .andExpect(status().isOk());
    }

    @Test
    void confirmPayment_withoutJwt_returnsUnauthorized() throws Exception {
        mockMvc.perform(post(paymentConfirmUri(1L))
                        .param("paymentStatus", PaymentStatus.SUCCESS.name()))
                .andExpect(status().isUnauthorized());
    }
}
