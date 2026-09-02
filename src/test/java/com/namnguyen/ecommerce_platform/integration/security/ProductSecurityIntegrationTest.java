package com.namnguyen.ecommerce_platform.integration.security;

import com.namnguyen.ecommerce_platform.integration.BaseSecurityIntegrationTest;
import com.namnguyen.ecommerce_platform.product.dto.ProductCreateRequest;
import com.namnguyen.ecommerce_platform.product.dto.ProductPatchRequest;
import com.namnguyen.ecommerce_platform.product.dto.ProductPutRequest;
import com.namnguyen.ecommerce_platform.product.entity.Product;
import com.namnguyen.ecommerce_platform.user.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.math.BigDecimal;

import static com.namnguyen.ecommerce_platform.testutil.TestDataFactory.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class ProductSecurityIntegrationTest extends BaseSecurityIntegrationTest {

    @Test
    void createProduct_whenUnauthenticated_returnsUnauthorized() throws Exception {
        ProductCreateRequest request = createDefaultProductCreateRequest();

        mockMvc.perform(post(PRODUCT_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createProduct_withCustomerJwt_returnsForbidden() throws Exception {
        User user = createDefaultCustomer();

        String token = loginAndGetToken(
                user.getEmail(),
                VALID_PASSWORD
        );

        ProductCreateRequest request = createDefaultProductCreateRequest();

        mockMvc.perform(post(PRODUCT_URI)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createProduct_withAdminJWT_returnsCreated() throws Exception {
        User admin = createDefaultAdmin();

        String token = loginAndGetToken(
                admin.getEmail(),
                VALID_PASSWORD
        );

        ProductCreateRequest request = createDefaultProductCreateRequest();

        mockMvc.perform(post(PRODUCT_URI)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void getAllProducts_whenUnauthenticated_returnsOk() throws Exception {
        mockMvc.perform(get(PRODUCT_URI))
                .andExpect(status().isOk());
    }

    @Test
    void getProductById_whenUnauthenticated_returnsOk() throws Exception {
        Product product = createDefaultProduct();

        mockMvc.perform(get(PRODUCT_URI + "/" + product.getId()))
                .andExpect(status().isOk());
    }

    @Test
    void putProduct_whenUnauthenticated_returnsUnauthorized() throws Exception {
        Product product = createDefaultProduct();

        ProductPutRequest request = createDefaultPutProductRequest();

        mockMvc.perform(put(PRODUCT_URI + "/" + product.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void putProduct_withCustomerJWT_returnsForbidden() throws Exception {
        User user = createDefaultCustomer();

        String token = loginAndGetToken(
                user.getEmail(),
                VALID_PASSWORD
        );

        Product product = createDefaultProduct();

        ProductPutRequest request = createDefaultPutProductRequest();

        mockMvc.perform(put(PRODUCT_URI + "/" + product.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void putProduct_withAdminJWT_returnsOk() throws Exception {
        User admin = createDefaultAdmin();

        String token = loginAndGetToken(
                admin.getEmail(),
                VALID_PASSWORD
        );

        Product product = createDefaultProduct();

        ProductPutRequest request = createDefaultPutProductRequest();

        mockMvc.perform(put(PRODUCT_URI + "/" + product.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void patchProduct_whenUnauthenticated_returnsUnauthorized() throws Exception {
        Product product = createDefaultProduct();

        ProductPatchRequest request = createPatchProductRequest(
                "PS5",
                null,
                BigDecimal.valueOf(399.99),
                null
        );

        mockMvc.perform(patch(PRODUCT_URI + "/" + product.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void patchProduct_withCustomerJWT_returnsForbidden() throws Exception {
        User user = createDefaultCustomer();

        String token = loginAndGetToken(
                user.getEmail(),
                VALID_PASSWORD
        );

        Product product = createDefaultProduct();

        ProductPatchRequest request = createPatchProductRequest(
                "PS5",
                null,
                BigDecimal.valueOf(399.99),
                null
        );

        mockMvc.perform(patch(PRODUCT_URI + "/" + product.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void patchProduct_withAdminJWT_returnsOk() throws Exception {
        User admin = createDefaultAdmin();

        String token = loginAndGetToken(
                admin.getEmail(),
                VALID_PASSWORD
        );

        Product product = createDefaultProduct();

        ProductPatchRequest request = createPatchProductRequest(
                "PS5",
                null,
                BigDecimal.valueOf(399.99),
                null
        );

        mockMvc.perform(patch(PRODUCT_URI + "/" + product.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void deleteProduct_whenUnauthenticated_returnsUnauthorized() throws Exception {
        Product product = createDefaultProduct();

        mockMvc.perform(delete(PRODUCT_URI + "/" + product.getId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteProduct_withCustomerJWT_returnsForbidden() throws Exception {
        User user = createDefaultCustomer();

        String token = loginAndGetToken(
                user.getEmail(),
                VALID_PASSWORD
        );

        Product product = createDefaultProduct();

        mockMvc.perform(delete(PRODUCT_URI + "/" + product.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteProduct_withAdminJWT_returnsNoContent() throws Exception {
        User admin = createDefaultAdmin();

        String token = loginAndGetToken(
                admin.getEmail(),
                VALID_PASSWORD
        );

        Product product = createDefaultProduct();

        mockMvc.perform(delete(PRODUCT_URI + "/" + product.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }
}
