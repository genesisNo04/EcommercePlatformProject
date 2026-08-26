package com.namnguyen.ecommerce_platform.integration.security;

import com.namnguyen.ecommerce_platform.integration.BaseSecurityIntegrationTest;
import com.namnguyen.ecommerce_platform.product.dto.ProductCreateRequest;
import com.namnguyen.ecommerce_platform.product.dto.ProductPatchRequest;
import com.namnguyen.ecommerce_platform.product.dto.ProductPutRequest;
import com.namnguyen.ecommerce_platform.product.entity.Product;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

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
    @WithMockUser(roles = "CUSTOMER")
    void createProduct_withCustomerRole_returnsForbidden() throws Exception {
        ProductCreateRequest request = createDefaultProductCreateRequest();

        mockMvc.perform(post(PRODUCT_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createProduct_withAdminRole_returnsCreated() throws Exception {
        ProductCreateRequest request = createDefaultProductCreateRequest();

        mockMvc.perform(post(PRODUCT_URI)
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
    @WithMockUser(roles = "CUSTOMER")
    void putProduct_withCustomerRole_returnsForbidden() throws Exception {
        Product product = createDefaultProduct();

        ProductPutRequest request = createDefaultPutProductRequest();

        mockMvc.perform(put(PRODUCT_URI + "/" + product.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void putProduct_withAdminRole_returnsOk() throws Exception {
        Product product = createDefaultProduct();

        ProductPutRequest request = createDefaultPutProductRequest();

        mockMvc.perform(put(PRODUCT_URI + "/" + product.getId())
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
    @WithMockUser(roles = "CUSTOMER")
    void patchProduct_withCustomerRole_returnsForbidden() throws Exception {
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
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void patchProduct_withAdminRole_returnsOk() throws Exception {
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
                .andExpect(status().isOk());
    }

    @Test
    void deleteProduct_whenUnauthenticated_returnsUnauthorized() throws Exception {
        Product product = createDefaultProduct();

        mockMvc.perform(delete(PRODUCT_URI + "/" + product.getId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void deleteProduct_withCustomerRole_returnsForbidden() throws Exception {
        Product product = createDefaultProduct();

        mockMvc.perform(delete(PRODUCT_URI + "/" + product.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteProduct_withAdminRole_returnsNoContent() throws Exception {
        Product product = createDefaultProduct();

        mockMvc.perform(delete(PRODUCT_URI + "/" + product.getId()))
                .andExpect(status().isNoContent());
    }
}
