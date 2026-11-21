package com.namnguyen.ecommerce_platform.integration.payment;

import com.namnguyen.ecommerce_platform.integration.BaseIntegrationTest;
import com.namnguyen.ecommerce_platform.payment.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class PaymentConcurrencyIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private PaymentService paymentService;

    @Test
    void concurrentPayAndCancel_onlyOneWinsPerOrder() {
        
    }
}
