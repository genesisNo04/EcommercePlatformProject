package com.namnguyen.ecommerce_platform.payment.controller;

import com.namnguyen.ecommerce_platform.payment.dto.PaymentRequest;
import com.namnguyen.ecommerce_platform.payment.dto.PaymentResponse;
import com.namnguyen.ecommerce_platform.payment.service.PaymentService;
import com.namnguyen.ecommerce_platform.security.user.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders/{orderId}/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping
    public ResponseEntity<PaymentResponse> getPayment(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long orderId) {
        return ResponseEntity.ok(paymentService.getPaymentByOrderId(orderId, userDetails.getUserId()));
    }

    @PatchMapping
    public ResponseEntity<PaymentResponse> updatePayment(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long orderId,
            @Valid @RequestBody PaymentRequest request) {
        return ResponseEntity.ok(paymentService.updatePayment(orderId, userDetails.getUserId(), request));
    }

    @GetMapping
    public ResponseEntity<PaymentResponse> submitPayment(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long orderId,
            @Valid @RequestBody PaymentRequest request) {
        return ResponseEntity.ok(paymentService.submitPayment(orderId, userDetails.getUserId(), request));
    }
}
