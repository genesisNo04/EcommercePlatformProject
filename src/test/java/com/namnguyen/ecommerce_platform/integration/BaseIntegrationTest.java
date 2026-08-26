package com.namnguyen.ecommerce_platform.integration;

import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

@AutoConfigureMockMvc(addFilters = false)
public abstract class BaseIntegrationTest extends AbstractIntegrationTestSupport {
}
