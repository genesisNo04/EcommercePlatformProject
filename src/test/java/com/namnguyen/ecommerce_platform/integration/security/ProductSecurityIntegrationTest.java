package com.namnguyen.ecommerce_platform.integration.security;

import com.namnguyen.ecommerce_platform.integration.BaseSecurityIntegrationTest;
import com.namnguyen.ecommerce_platform.product.dto.ProductCreateRequest;
import com.namnguyen.ecommerce_platform.product.dto.ProductPatchRequest;
import com.namnguyen.ecommerce_platform.product.dto.ProductPutRequest;
import com.namnguyen.ecommerce_platform.product.entity.Product;
import com.namnguyen.ecommerce_platform.user.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static com.namnguyen.ecommerce_platform.testutil.TestDataFactory.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class ProductSecurityIntegrationTest extends BaseSecurityIntegrationTest {

    @Test
    void createProduct_withoutJwt_returnsUnauthorized() throws Exception {
        ProductCreateRequest productCreateRequest = createDefaultProductCreateRequest();

        mockMvc.perform(post(PRODUCT_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(productCreateRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createProduct_withCustomerJwt_returnsForbidden() throws Exception {
        User user = persistDefaultCustomer();

        String token = loginAndGetToken(
                user.getEmail(),
                VALID_PASSWORD
        );

        ProductCreateRequest productCreateRequest = createDefaultProductCreateRequest();

        mockMvc.perform(post(PRODUCT_URI)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productCreateRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createProduct_withAdminJwt_returnsCreated() throws Exception {
        User admin = persistDefaultAdmin();

        String token = loginAndGetToken(
                admin.getEmail(),
                VALID_PASSWORD
        );

        ProductCreateRequest productCreateRequest = createDefaultProductCreateRequest();

        mockMvc.perform(post(PRODUCT_URI)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productCreateRequest)))
                .andExpect(status().isCreated());
    }

    @Test
    void getAllProducts_whenUnauthenticated_returnsOk() throws Exception {
        mockMvc.perform(get(PRODUCT_URI))
                .andExpect(status().isOk());
    }

    @Test
    void getProductById_whenUnauthenticated_returnsOk() throws Exception {
        Product product = persistDefaultProduct();

        mockMvc.perform(get(productUri(product.getId())))
                .andExpect(status().isOk());
    }

    @Test
    void putProduct_withoutJwt_returnsUnauthorized() throws Exception {
        ProductPutRequest request = createDefaultProductPutRequest();

        mockMvc.perform(put(productUri(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void putProduct_withCustomerJwt_returnsForbidden() throws Exception {
        User user = persistDefaultCustomer();

        String token = loginAndGetToken(
                user.getEmail(),
                VALID_PASSWORD
        );

        Product product = persistDefaultProduct();

        ProductPutRequest productPutRequest = createDefaultProductPutRequest();

        mockMvc.perform(put(productUri(product.getId()))
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productPutRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    void putProduct_withAdminJwt_returnsOk() throws Exception {
        User admin = persistDefaultAdmin();

        String token = loginAndGetToken(
                admin.getEmail(),
                VALID_PASSWORD
        );

        Product product = persistDefaultProduct();

        ProductPutRequest productPutRequest = createDefaultProductPutRequest();

        mockMvc.perform(put(productUri(product.getId()))
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productPutRequest)))
                .andExpect(status().isOk());
    }

    @Test
    void patchProduct_withoutJwt_returnsUnauthorized() throws Exception {
        ProductPatchRequest productPatchRequest = createDefaultProductPatchRequest();

        mockMvc.perform(patch(productUri(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productPatchRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void patchProduct_withCustomerJwt_returnsForbidden() throws Exception {
        User user = persistDefaultCustomer();

        String token = loginAndGetToken(
                user.getEmail(),
                VALID_PASSWORD
        );

        Product product = persistDefaultProduct();

        ProductPatchRequest productPatchRequest = createDefaultProductPatchRequest();

        mockMvc.perform(patch(productUri(product.getId()))
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productPatchRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    void patchProduct_withAdminJwt_returnsOk() throws Exception {
        User admin = persistDefaultAdmin();

        String token = loginAndGetToken(
                admin.getEmail(),
                VALID_PASSWORD
        );

        Product product = persistDefaultProduct();

        ProductPatchRequest productPatchRequest = createDefaultProductPatchRequest();

        mockMvc.perform(patch(productUri(product.getId()))
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productPatchRequest)))
                .andExpect(status().isOk());
    }

    @Test
    void deleteProduct_withoutJwt_returnsUnauthorized() throws Exception {
        mockMvc.perform(delete(productUri(1L)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteProduct_withCustomerJwt_returnsForbidden() throws Exception {
        User user = persistDefaultCustomer();

        String token = loginAndGetToken(
                user.getEmail(),
                VALID_PASSWORD
        );

        Product product = persistDefaultProduct();

        mockMvc.perform(delete(productUri(product.getId()))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteProduct_withAdminJwt_returnsNoContent() throws Exception {
        User admin = persistDefaultAdmin();

        String token = loginAndGetToken(
                admin.getEmail(),
                VALID_PASSWORD
        );

        Product product = persistDefaultProduct();

        mockMvc.perform(delete(productUri(product.getId()))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }
}
