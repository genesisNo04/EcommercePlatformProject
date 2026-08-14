package com.namnguyen.ecommerce_platform.integration.security;

import com.namnguyen.ecommerce_platform.integration.BaseSecurityIntegrationTest;
import com.namnguyen.ecommerce_platform.product.dto.ProductCreateRequest;
import com.namnguyen.ecommerce_platform.product.dto.ProductPatchRequest;
import com.namnguyen.ecommerce_platform.product.dto.ProductPutRequest;
import com.namnguyen.ecommerce_platform.product.entity.Product;
import com.namnguyen.ecommerce_platform.product.enums.ProductStatus;
import com.namnguyen.ecommerce_platform.product.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import java.math.BigDecimal;

import static com.namnguyen.ecommerce_platform.testutil.TestDataFactory.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class ProductSecurityIntegrationTest extends BaseSecurityIntegrationTest {

    @Autowired
    private ProductRepository productRepository;

    @Test
    void createProduct_whenUnauthenticated_returnsUnauthorized() throws Exception {
        ProductCreateRequest request = new ProductCreateRequest(
                "PS5",
                "Playstation",
                BigDecimal.valueOf(399.99),
                12
        );

        mockMvc.perform(post(PRODUCT_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    void createProduct_whenUserRole_returnsForbidden() throws Exception {
        ProductCreateRequest request = new ProductCreateRequest(
                "PS5",
                "Playstation",
                BigDecimal.valueOf(399.99),
                12
        );

        mockMvc.perform(post(PRODUCT_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createProduct_whenAdminRole_returnsCreated() throws Exception {
        ProductCreateRequest request = new ProductCreateRequest(
                "PS5",
                "Playstation",
                BigDecimal.valueOf(399.99),
                12
        );

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
        Product product = Product.builder()
                .name("Keyboard")
                .description("Mechanical keyboard")
                .price(BigDecimal.valueOf(99.99))
                .quantity(10)
                .status(ProductStatus.ACTIVE)
                .build();

        Product savedProduct = productRepository.save(product);

        mockMvc.perform(get(PRODUCT_URI + "/" + savedProduct.getId()))
                .andExpect(status().isOk());
    }

    @Test
    void putProduct_whenUnauthenticated_returnsUnauthorized() throws Exception {
        Product product = Product.builder()
                .name("Keyboard")
                .description("Mechanical keyboard")
                .price(BigDecimal.valueOf(99.99))
                .quantity(10)
                .status(ProductStatus.ACTIVE)
                .build();

        Product savedProduct = productRepository.save(product);

        ProductPutRequest request = new ProductPutRequest(
                "PS5",
                "Playstation",
                BigDecimal.valueOf(399.99),
                12
        );

        mockMvc.perform(put(PRODUCT_URI + "/" + savedProduct.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    void putProduct_whenUserRole_returnsForbidden() throws Exception {
        Product product = Product.builder()
                .name("Keyboard")
                .description("Mechanical keyboard")
                .price(BigDecimal.valueOf(99.99))
                .quantity(10)
                .status(ProductStatus.ACTIVE)
                .build();

        Product savedProduct = productRepository.save(product);

        ProductPutRequest request = new ProductPutRequest(
                "PS5",
                "Playstation",
                BigDecimal.valueOf(399.99),
                12
        );

        mockMvc.perform(put(PRODUCT_URI + "/" + savedProduct.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void putProduct_whenAdminRole_returnsOk() throws Exception {
        Product product = Product.builder()
                .name("Keyboard")
                .description("Mechanical keyboard")
                .price(BigDecimal.valueOf(99.99))
                .quantity(10)
                .status(ProductStatus.ACTIVE)
                .build();

        Product savedProduct = productRepository.save(product);

        ProductPutRequest request = new ProductPutRequest(
                "PS5",
                "Playstation",
                BigDecimal.valueOf(399.99),
                12
        );

        mockMvc.perform(put(PRODUCT_URI + "/" + savedProduct.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void patchProduct_whenUnauthenticated_returnsUnauthorized() throws Exception {
        Product product = Product.builder()
                .name("Keyboard")
                .description("Mechanical keyboard")
                .price(BigDecimal.valueOf(99.99))
                .quantity(10)
                .status(ProductStatus.ACTIVE)
                .build();

        Product savedProduct = productRepository.save(product);

        ProductPatchRequest request = new ProductPatchRequest(
                "PS5",
                null,
                BigDecimal.valueOf(399.99),
                null
        );

        mockMvc.perform(patch(PRODUCT_URI + "/" + savedProduct.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    void patchProduct_whenUserRole_returnsForbidden() throws Exception {
        Product product = Product.builder()
                .name("Keyboard")
                .description("Mechanical keyboard")
                .price(BigDecimal.valueOf(99.99))
                .quantity(10)
                .status(ProductStatus.ACTIVE)
                .build();

        Product savedProduct = productRepository.save(product);

        ProductPatchRequest request = new ProductPatchRequest(
                "PS5",
                null,
                BigDecimal.valueOf(399.99),
                null
        );

        mockMvc.perform(patch(PRODUCT_URI + "/" + savedProduct.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void patchProduct_whenAdminRole_returnsOk() throws Exception {
        Product product = Product.builder()
                .name("Keyboard")
                .description("Mechanical keyboard")
                .price(BigDecimal.valueOf(99.99))
                .quantity(10)
                .status(ProductStatus.ACTIVE)
                .build();

        Product savedProduct = productRepository.save(product);


        ProductPatchRequest request = new ProductPatchRequest(
                "PS5",
                null,
                BigDecimal.valueOf(399.99),
                null
        );

        mockMvc.perform(patch(PRODUCT_URI + "/" + savedProduct.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void deleteProduct_whenUnauthenticated_returnsUnauthorized() throws Exception {
        Product product = Product.builder()
                .name("Keyboard")
                .description("Mechanical keyboard")
                .price(BigDecimal.valueOf(99.99))
                .quantity(10)
                .status(ProductStatus.ACTIVE)
                .build();

        Product savedProduct = productRepository.save(product);

        mockMvc.perform(delete(PRODUCT_URI + "/" + savedProduct.getId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    void deleteProduct_whenUserRole_returnsForbidden() throws Exception {
        Product product = Product.builder()
                .name("Keyboard")
                .description("Mechanical keyboard")
                .price(BigDecimal.valueOf(99.99))
                .quantity(10)
                .status(ProductStatus.ACTIVE)
                .build();

        Product savedProduct = productRepository.save(product);

        mockMvc.perform(delete(PRODUCT_URI + "/" + savedProduct.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteProduct_whenAdminRole_returnsOk() throws Exception {
        Product product = Product.builder()
                .name("Keyboard")
                .description("Mechanical keyboard")
                .price(BigDecimal.valueOf(99.99))
                .quantity(10)
                .status(ProductStatus.ACTIVE)
                .build();

        Product savedProduct = productRepository.save(product);

        mockMvc.perform(delete(PRODUCT_URI + "/" + savedProduct.getId()))
                .andExpect(status().isNoContent());
    }
}
