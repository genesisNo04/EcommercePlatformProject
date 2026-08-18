package com.namnguyen.ecommerce_platform.integration.security;

import com.namnguyen.ecommerce_platform.cart.dto.CartItemRequest;
import com.namnguyen.ecommerce_platform.integration.BaseSecurityIntegrationTest;
import com.namnguyen.ecommerce_platform.product.entity.Product;
import com.namnguyen.ecommerce_platform.user.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static com.namnguyen.ecommerce_platform.testutil.MockAuthentication.authenticateUser;
import static com.namnguyen.ecommerce_platform.testutil.TestDataFactory.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class CartSecurityIntegrationTest extends BaseSecurityIntegrationTest {

    @Test
    void getCart_withoutJWTToken_returnsBadRequest() throws Exception {
        mockMvc.perform(get(CART_URI))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getCart_withUserJwt_returnsOk() throws Exception {
        User user = createDefaultCustomer();

        String token = loginAndGetToken(user.getEmail(), VALID_PASSWORD);

        mockMvc.perform(get(CART_URI)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void addItem_withoutJWTToken_returnsBadRequest() throws Exception {
        mockMvc.perform(post(CART_URI + "/items"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void addItem_withUserJwt_returnsOk() throws Exception {
        User user = createDefaultCustomer();
        Product product = createDefaultProduct();
        CartItemRequest request = createCartItemRequest(product.getId(), 10);

        String token = loginAndGetToken(user.getEmail(), VALID_PASSWORD);

        mockMvc.perform(post(CART_URI + "/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated());
    }

    @Test
    void updateItem_withoutJWTToken_returnsBadRequest() throws Exception {
        mockMvc.perform(post(CART_URI + "/items"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateItem_withUserJwt_returnsOk() throws Exception {
        User user = createDefaultCustomer();
        Product product = createDefaultProduct();
        CartItemRequest request = createCartItemRequest(product.getId(), 10);

        String token = loginAndGetToken(user.getEmail(), VALID_PASSWORD);

        mockMvc.perform(post(CART_URI + "/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated());
    }
}
