package com.namnguyen.ecommerce_platform.product.controller;

import com.namnguyen.ecommerce_platform.product.dto.ProductCreateRequest;
import com.namnguyen.ecommerce_platform.product.dto.ProductResponse;
import com.namnguyen.ecommerce_platform.product.service.ProductService;
import com.namnguyen.ecommerce_platform.security.jwt.JwtService;
import com.namnguyen.ecommerce_platform.security.user.CustomUserDetailsService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;

import static com.namnguyen.ecommerce_platform.testutil.TestDataFactory.*;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static com.namnguyen.ecommerce_platform.testutil.TestMessages.*;
import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.containsInAnyOrder;

@WebMvcTest(ProductController.class)
@AutoConfigureMockMvc(addFilters = false)
public class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;


    @Test
    void createProduct_validRequest_returnsProductResponse() throws Exception {
        Long productId = 1L;

        ProductCreateRequest request = new ProductCreateRequest(
                PRODUCT_NAME,
                PRODUCT_DESCRIPTION,
                PRODUCT_PRICE,
                PRODUCT_QUANTITY
        );

        ProductResponse response = new ProductResponse(
                productId,
                PRODUCT_NAME,
                PRODUCT_DESCRIPTION,
                PRODUCT_PRICE,
                PRODUCT_QUANTITY,
                PRODUCT_STATUS,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(productService.createProduct(request)).thenReturn(response);

        mockMvc.perform(post(PRODUCT_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(productId))
                .andExpect(jsonPath("$.name").value(PRODUCT_NAME))
                .andExpect(jsonPath("$.description").value(PRODUCT_DESCRIPTION))
                .andExpect(jsonPath("$.price").value(PRODUCT_PRICE))
                .andExpect(jsonPath("$.quantity").value(PRODUCT_QUANTITY))
                .andExpect(jsonPath("$.status").value(PRODUCT_STATUS.name()))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());

        ArgumentCaptor<ProductCreateRequest> captor = ArgumentCaptor.forClass(ProductCreateRequest.class);
        verify(productService).createProduct(captor.capture());

        ProductCreateRequest requestCaptor = captor.getValue();

        assertThat(requestCaptor.name()).isEqualTo(PRODUCT_NAME);
        assertThat(requestCaptor.description()).isEqualTo(PRODUCT_DESCRIPTION);
        assertThat(requestCaptor.price()).isEqualByComparingTo(PRODUCT_PRICE);
        assertThat(requestCaptor.quantity()).isEqualTo(PRODUCT_QUANTITY);

        verifyNoMoreInteractions(productService);
    }
}
