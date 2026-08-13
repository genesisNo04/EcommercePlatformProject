package com.namnguyen.ecommerce_platform.product.controller;

import com.namnguyen.ecommerce_platform.common.exception.NoResourceFoundException;
import com.namnguyen.ecommerce_platform.common.rate_limit.RateLimitService;
import com.namnguyen.ecommerce_platform.product.dto.*;
import com.namnguyen.ecommerce_platform.product.enums.ProductStatus;
import com.namnguyen.ecommerce_platform.product.service.ProductService;
import com.namnguyen.ecommerce_platform.security.jwt.JwtService;
import com.namnguyen.ecommerce_platform.security.user.CustomUserDetailsService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static com.namnguyen.ecommerce_platform.testutil.TestDataFactory.*;
import static com.namnguyen.ecommerce_platform.testutil.TestMessages.*;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
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

    @MockitoBean
    private RateLimitService rateLimitService;


    @Test
    void createProduct_validRequest_returnsProductResponse() throws Exception {
        Long productId = 1L;

        ProductCreateRequest request = new ProductCreateRequest(
                VALID_PRODUCT_NAME,
                VALID_PRODUCT_DESCRIPTION,
                VALID_PRODUCT_PRICE,
                VALID_PRODUCT_QUANTITY
        );

        ProductResponse response = new ProductResponse(
                productId,
                VALID_PRODUCT_NAME,
                VALID_PRODUCT_DESCRIPTION,
                VALID_PRODUCT_PRICE,
                VALID_PRODUCT_QUANTITY,
                VALID_PRODUCT_STATUS,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(productService.createProduct(request)).thenReturn(response);

        mockMvc.perform(post(PRODUCT_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(productId))
                .andExpect(jsonPath("$.name").value(VALID_PRODUCT_NAME))
                .andExpect(jsonPath("$.description").value(VALID_PRODUCT_DESCRIPTION))
                .andExpect(jsonPath("$.price").value(VALID_PRODUCT_PRICE))
                .andExpect(jsonPath("$.quantity").value(VALID_PRODUCT_QUANTITY))
                .andExpect(jsonPath("$.status").value(VALID_PRODUCT_STATUS.name()))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());

        ArgumentCaptor<ProductCreateRequest> captor = ArgumentCaptor.forClass(ProductCreateRequest.class);
        verify(productService).createProduct(captor.capture());

        ProductCreateRequest requestCaptor = captor.getValue();

        assertThat(requestCaptor.name()).isEqualTo(VALID_PRODUCT_NAME);
        assertThat(requestCaptor.description()).isEqualTo(VALID_PRODUCT_DESCRIPTION);
        assertThat(requestCaptor.price()).isEqualByComparingTo(VALID_PRODUCT_PRICE);
        assertThat(requestCaptor.quantity()).isEqualTo(VALID_PRODUCT_QUANTITY);

        verifyNoMoreInteractions(productService);
    }

    @Test
    void createProduct_whenNameIsEmpty_returnsBadRequest() throws Exception {
        ProductCreateRequest request = new ProductCreateRequest(
                "",
                VALID_PRODUCT_DESCRIPTION,
                VALID_PRODUCT_PRICE,
                VALID_PRODUCT_QUANTITY
        );

        mockMvc.perform(post(PRODUCT_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.uri").value(PRODUCT_URI))
                .andExpect(jsonPath("$.fieldErrors.name", containsInAnyOrder(productNameIsRequired())));

        verifyNoInteractions(productService);
    }

    @Test
    void createProduct_whenNameIsNull_returnsBadRequest() throws Exception {
        ProductCreateRequest request = new ProductCreateRequest(
                null,
                VALID_PRODUCT_DESCRIPTION,
                VALID_PRODUCT_PRICE,
                VALID_PRODUCT_QUANTITY
        );

        mockMvc.perform(post(PRODUCT_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.uri").value(PRODUCT_URI))
                .andExpect(jsonPath("$.fieldErrors.name", containsInAnyOrder(productNameIsRequired())));

        verifyNoInteractions(productService);
    }

    @Test
    void createProduct_whenNameIsMoreThan100_returnsBadRequest() throws Exception {
        ProductCreateRequest request = new ProductCreateRequest(
                INVALID_PRODUCT_NAME_MORE_THAN_LIMIT,
                VALID_PRODUCT_DESCRIPTION,
                VALID_PRODUCT_PRICE,
                VALID_PRODUCT_QUANTITY
        );

        mockMvc.perform(post(PRODUCT_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.uri").value(PRODUCT_URI))
                .andExpect(jsonPath("$.fieldErrors.name", containsInAnyOrder(productNameLength())));

        verifyNoInteractions(productService);
    }

    @Test
    void createProduct_whenDescriptionIsEmpty_returnsBadRequest() throws Exception {
        ProductCreateRequest request = new ProductCreateRequest(
                VALID_PRODUCT_NAME,
                "",
                VALID_PRODUCT_PRICE,
                VALID_PRODUCT_QUANTITY
        );

        mockMvc.perform(post(PRODUCT_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.uri").value(PRODUCT_URI))
                .andExpect(jsonPath("$.fieldErrors.description", containsInAnyOrder(
                        productDescriptionLength(),
                        productDescriptionIsRequired())));

        verifyNoInteractions(productService);
    }

    @Test
    void createProduct_whenDescriptionIsNull_returnsBadRequest() throws Exception {
        ProductCreateRequest request = new ProductCreateRequest(
                VALID_PRODUCT_NAME,
                null,
                VALID_PRODUCT_PRICE,
                VALID_PRODUCT_QUANTITY
        );

        mockMvc.perform(post(PRODUCT_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.uri").value(PRODUCT_URI))
                .andExpect(jsonPath("$.fieldErrors.description", containsInAnyOrder(productDescriptionIsRequired())));

        verifyNoInteractions(productService);
    }

    @Test
    void createProduct_whenDescriptionIsLessThan5_returnsBadRequest() throws Exception {
        ProductCreateRequest request = new ProductCreateRequest(
                VALID_PRODUCT_NAME,
                INVALID_PRODUCT_DESCRIPTION_LESS_THAN_LIMIT,
                VALID_PRODUCT_PRICE,
                VALID_PRODUCT_QUANTITY
        );

        mockMvc.perform(post(PRODUCT_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.uri").value(PRODUCT_URI))
                .andExpect(jsonPath("$.fieldErrors.description", containsInAnyOrder(productDescriptionLength())));

        verifyNoInteractions(productService);
    }

    @Test
    void createProduct_whenDescriptionIsMoreThan1000_returnsBadRequest() throws Exception {
        ProductCreateRequest request = new ProductCreateRequest(
                VALID_PRODUCT_NAME,
                INVALID_PRODUCT_DESCRIPTION_MORE_THAN_LIMIT,
                VALID_PRODUCT_PRICE,
                VALID_PRODUCT_QUANTITY
        );

        mockMvc.perform(post(PRODUCT_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.uri").value(PRODUCT_URI))
                .andExpect(jsonPath("$.fieldErrors.description", containsInAnyOrder(productDescriptionLength())));

        verifyNoInteractions(productService);
    }

    @Test
    void createProduct_whenPriceIsNull_returnsBadRequest() throws Exception {
        ProductCreateRequest request = new ProductCreateRequest(
                VALID_PRODUCT_NAME,
                VALID_PRODUCT_DESCRIPTION,
                null,
                VALID_PRODUCT_QUANTITY
        );

        mockMvc.perform(post(PRODUCT_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.uri").value(PRODUCT_URI))
                .andExpect(jsonPath("$.fieldErrors.price", containsInAnyOrder(productPriceIsRequired())));

        verifyNoInteractions(productService);
    }

    @Test
    void createProduct_whenPriceIsZero_returnsBadRequest() throws Exception {
        ProductCreateRequest request = new ProductCreateRequest(
                VALID_PRODUCT_NAME,
                VALID_PRODUCT_DESCRIPTION,
                INVALID_PRODUCT_PRICE_ZERO,
                VALID_PRODUCT_QUANTITY
        );

        mockMvc.perform(post(PRODUCT_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.uri").value(PRODUCT_URI))
                .andExpect(jsonPath("$.fieldErrors.price", containsInAnyOrder(productPriceZero())));

        verifyNoInteractions(productService);
    }

    @Test
    void createProduct_whenQuantityIsNull_returnsBadRequest() throws Exception {
        ProductCreateRequest request = new ProductCreateRequest(
                VALID_PRODUCT_NAME,
                VALID_PRODUCT_DESCRIPTION,
                VALID_PRODUCT_PRICE,
                null
        );

        mockMvc.perform(post(PRODUCT_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.uri").value(PRODUCT_URI))
                .andExpect(jsonPath("$.fieldErrors.quantity", containsInAnyOrder(productQuantityIsRequired())));

        verifyNoInteractions(productService);
    }

    @Test
    void createProduct_whenQuantityIsNegative_returnsBadRequest() throws Exception {
        ProductCreateRequest request = new ProductCreateRequest(
                VALID_PRODUCT_NAME,
                VALID_PRODUCT_DESCRIPTION,
                VALID_PRODUCT_PRICE,
                INVALID_PRODUCT_NEGATIVE_QUANTITY
        );

        mockMvc.perform(post(PRODUCT_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.uri").value(PRODUCT_URI))
                .andExpect(jsonPath("$.fieldErrors.quantity", containsInAnyOrder(productNegativeQuantity())));

        verifyNoInteractions(productService);
    }

    @Test
    void getProductById_whenProductExists_returnsProductResponse() throws Exception {
        Long productId = 1L;

        ProductResponse response = new ProductResponse(
                productId,
                VALID_PRODUCT_NAME,
                VALID_PRODUCT_DESCRIPTION,
                VALID_PRODUCT_PRICE,
                VALID_PRODUCT_QUANTITY,
                VALID_PRODUCT_STATUS,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(productService.getProductById(productId)).thenReturn(response);

        mockMvc.perform(get(PRODUCT_URI + "/" + productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(productId))
                .andExpect(jsonPath("$.name").value(VALID_PRODUCT_NAME))
                .andExpect(jsonPath("$.description").value(VALID_PRODUCT_DESCRIPTION))
                .andExpect(jsonPath("$.price").value(VALID_PRODUCT_PRICE))
                .andExpect(jsonPath("$.quantity").value(VALID_PRODUCT_QUANTITY))
                .andExpect(jsonPath("$.status").value(VALID_PRODUCT_STATUS.name()))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());

        verify(productService).getProductById(productId);
        verifyNoMoreInteractions(productService);
    }

    @Test
    void getProductById_whenProductIdIsInvalid_returnsBadRequest() throws Exception {
        String productId = INVALID_ID;

        mockMvc.perform(get(PRODUCT_URI + "/" + productId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(invalidParameter("id")))
                .andExpect(jsonPath("$.uri").value(PRODUCT_URI + "/" + productId));

        verifyNoInteractions(productService);
    }

    @Test
    void getProductById_whenProductNotExists_returnsNotFound() throws Exception {
        Long productId = 1L;

        when(productService.getProductById(productId))
                .thenThrow(new NoResourceFoundException(productNotFound(productId)));

        mockMvc.perform(get(PRODUCT_URI + "/" + productId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.NOT_FOUND.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(productNotFound(productId)))
                .andExpect(jsonPath("$.uri").value(PRODUCT_URI + "/" + productId));


        verify(productService).getProductById(productId);
        verifyNoMoreInteractions(productService);
    }

    @Test
    void getAllProducts_whenProductsExist_returnsPageOfProducts() throws Exception {
        Long productId = 1L;
        Long productId1 = 2L;

        ProductResponse response = new ProductResponse(
                productId,
                VALID_PRODUCT_NAME,
                VALID_PRODUCT_DESCRIPTION,
                VALID_PRODUCT_PRICE,
                VALID_PRODUCT_QUANTITY,
                VALID_PRODUCT_STATUS,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        ProductResponse response1 = new ProductResponse(
                productId1,
                VALID_PRODUCT_NAME + "1",
                VALID_PRODUCT_DESCRIPTION+ "1",
                VALID_PRODUCT_PRICE,
                VALID_PRODUCT_QUANTITY + 1,
                VALID_PRODUCT_STATUS,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        List<ProductResponse> responses = List.of(response, response1);
        Pageable pageable = PageRequest.of(0, 10);
        Page<ProductResponse> pageResponse = new PageImpl<>(responses, pageable, responses.size());

        when(productService.getAllProducts(any(ProductFilterRequest.class), any(Pageable.class))).thenReturn(pageResponse);

        mockMvc.perform(get(PRODUCT_URI)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].id").value(productId))
                .andExpect(jsonPath("$.content[0].name").value(VALID_PRODUCT_NAME))
                .andExpect(jsonPath("$.content[0].description").value(VALID_PRODUCT_DESCRIPTION))
                .andExpect(jsonPath("$.content[0].price").value(VALID_PRODUCT_PRICE))
                .andExpect(jsonPath("$.content[0].quantity").value(VALID_PRODUCT_QUANTITY))
                .andExpect(jsonPath("$.content[0].status").value(VALID_PRODUCT_STATUS.name()))
                .andExpect(jsonPath("$.content[0].createdAt").exists())
                .andExpect(jsonPath("$.content[0].updatedAt").exists())
                .andExpect(jsonPath("$.content[1].id").value(productId1))
                .andExpect(jsonPath("$.content[1].name").value(VALID_PRODUCT_NAME+ "1"))
                .andExpect(jsonPath("$.content[1].description").value(VALID_PRODUCT_DESCRIPTION+ "1"))
                .andExpect(jsonPath("$.content[1].price").value(VALID_PRODUCT_PRICE))
                .andExpect(jsonPath("$.content[1].quantity").value(VALID_PRODUCT_QUANTITY + 1))
                .andExpect(jsonPath("$.content[1].status").value(VALID_PRODUCT_STATUS.name()))
                .andExpect(jsonPath("$.content[1].createdAt").exists())
                .andExpect(jsonPath("$.content[1].updatedAt").exists())
                .andExpect(jsonPath("$.numberOfElements").value(2))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1));

        ArgumentCaptor<ProductFilterRequest> captorFilter = ArgumentCaptor.forClass(ProductFilterRequest.class);
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(productService).getAllProducts(captorFilter.capture(), captor.capture());

        ProductFilterRequest capturedFilter = captorFilter.getValue();
        Pageable pageableCaptor = captor.getValue();

        assertThat(capturedFilter.status()).isNull();
        assertThat(capturedFilter.keyword()).isNull();
        assertThat(capturedFilter.minPrice()).isNull();
        assertThat(capturedFilter.maxPrice()).isNull();
        assertThat(pageableCaptor.getPageSize()).isEqualTo(10);
        assertThat(pageableCaptor.getPageNumber()).isEqualTo(0);
        assertThat(pageableCaptor.getSort()).contains(Sort.Order.asc("id"));

        verifyNoMoreInteractions(productService);
    }

    @Test
    void getAllProducts_whenFilterAvailable_returnsPageOfProducts() throws Exception {
        Long productId = 1L;
        Long productId1 = 2L;

        ProductResponse response = new ProductResponse(
                productId,
                VALID_PRODUCT_NAME,
                VALID_PRODUCT_DESCRIPTION,
                VALID_PRODUCT_PRICE,
                VALID_PRODUCT_QUANTITY,
                VALID_PRODUCT_STATUS,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        ProductResponse response1 = new ProductResponse(
                productId1,
                VALID_PRODUCT_NAME + "1",
                VALID_PRODUCT_DESCRIPTION+ "1",
                VALID_PRODUCT_PRICE,
                VALID_PRODUCT_QUANTITY + 1,
                VALID_PRODUCT_STATUS,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        List<ProductResponse> responses = List.of(response, response1);
        Pageable pageable = PageRequest.of(0, 10);
        Page<ProductResponse> pageResponse = new PageImpl<>(responses, pageable, responses.size());

        when(productService.getAllProducts(any(ProductFilterRequest.class), any(Pageable.class))).thenReturn(pageResponse);

        mockMvc.perform(get(PRODUCT_URI)
                        .param("page", "0")
                        .param("size", "10")
                        .param("status", VALID_PRODUCT_STATUS.name())
                        .param("keyword", "Test")
                        .param("minPrice", "0.0")
                        .param("maxPrice", "20.0" ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].id").value(productId))
                .andExpect(jsonPath("$.content[0].name").value(VALID_PRODUCT_NAME))
                .andExpect(jsonPath("$.content[0].description").value(VALID_PRODUCT_DESCRIPTION))
                .andExpect(jsonPath("$.content[0].price").value(VALID_PRODUCT_PRICE))
                .andExpect(jsonPath("$.content[0].quantity").value(VALID_PRODUCT_QUANTITY))
                .andExpect(jsonPath("$.content[0].status").value(VALID_PRODUCT_STATUS.name()))
                .andExpect(jsonPath("$.content[0].createdAt").exists())
                .andExpect(jsonPath("$.content[0].updatedAt").exists())
                .andExpect(jsonPath("$.content[1].id").value(productId1))
                .andExpect(jsonPath("$.content[1].name").value(VALID_PRODUCT_NAME+ "1"))
                .andExpect(jsonPath("$.content[1].description").value(VALID_PRODUCT_DESCRIPTION+ "1"))
                .andExpect(jsonPath("$.content[1].price").value(VALID_PRODUCT_PRICE))
                .andExpect(jsonPath("$.content[1].quantity").value(VALID_PRODUCT_QUANTITY + 1))
                .andExpect(jsonPath("$.content[1].status").value(VALID_PRODUCT_STATUS.name()))
                .andExpect(jsonPath("$.content[1].createdAt").exists())
                .andExpect(jsonPath("$.content[1].updatedAt").exists())
                .andExpect(jsonPath("$.numberOfElements").value(2))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1));

        ArgumentCaptor<ProductFilterRequest> captorFilter = ArgumentCaptor.forClass(ProductFilterRequest.class);
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(productService).getAllProducts(captorFilter.capture(), captor.capture());

        ProductFilterRequest capturedFilter = captorFilter.getValue();
        Pageable pageableCaptor = captor.getValue();

        assertThat(capturedFilter.status()).isEqualTo(VALID_PRODUCT_STATUS);
        assertThat(capturedFilter.keyword()).isEqualTo("Test");
        assertThat(capturedFilter.minPrice()).isEqualByComparingTo(BigDecimal.valueOf(0.0));
        assertThat(capturedFilter.maxPrice()).isEqualByComparingTo(BigDecimal.valueOf(20.0));
        assertThat(pageableCaptor.getPageSize()).isEqualTo(10);
        assertThat(pageableCaptor.getPageNumber()).isEqualTo(0);
        assertThat(pageableCaptor.getSort()).contains(Sort.Order.asc("id"));

        verifyNoMoreInteractions(productService);
    }

    @Test
    void getAllProducts_whenStatusFilterIsInvalid_returnsBadRequest() throws Exception {
        mockMvc.perform(get(PRODUCT_URI)
                        .param("page", "0")
                        .param("size", "10")
                        .param("status", "BAD_STATUS")
                        .param("keyword", "Test")
                        .param("minPrice", "0.0")
                        .param("maxPrice", "20.0" ))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.uri").value(PRODUCT_URI))
                .andExpect(jsonPath("$.fieldErrors.status", containsInAnyOrder(invalidParameter("status"))));

        verifyNoInteractions(productService);
    }

    @Test
    void getAllProducts_whenMinPriceIsInvalid_returnsBadRequest() throws Exception {
        mockMvc.perform(get(PRODUCT_URI)
                        .param("page", "0")
                        .param("size", "10")
                        .param("status", VALID_PRODUCT_STATUS.name())
                        .param("keyword", "Test")
                        .param("minPrice", "abc")
                        .param("maxPrice", "20.0" ))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.uri").value(PRODUCT_URI))
                .andExpect(jsonPath("$.fieldErrors.minPrice", containsInAnyOrder(invalidParameter("minPrice"))));

        verifyNoInteractions(productService);
    }

    @Test
    void getAllProducts_whenMaxPriceIsInvalid_returnsBadRequest() throws Exception {
        mockMvc.perform(get(PRODUCT_URI)
                        .param("page", "0")
                        .param("size", "10")
                        .param("status", VALID_PRODUCT_STATUS.name())
                        .param("keyword", "Test")
                        .param("minPrice", "0.0")
                        .param("maxPrice", "abc" ))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.uri").value(PRODUCT_URI))
                .andExpect(jsonPath("$.fieldErrors.maxPrice", containsInAnyOrder(invalidParameter("maxPrice"))));

        verifyNoInteractions(productService);
    }

    @Test
    void getAllProducts_whenNoProductsExist_returnsEmptyPage() throws Exception {
        List<ProductResponse> responses = List.of();
        Pageable pageable = PageRequest.of(0, 10);
        Page<ProductResponse> pageResponse = new PageImpl<>(responses, pageable, responses.size());

        when(productService.getAllProducts(any(ProductFilterRequest.class), any(Pageable.class))).thenReturn(pageResponse);

        mockMvc.perform(get(PRODUCT_URI)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.numberOfElements").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0));

        ArgumentCaptor<ProductFilterRequest> captorFilter = ArgumentCaptor.forClass(ProductFilterRequest.class);
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(productService).getAllProducts(captorFilter.capture(), captor.capture());

        ProductFilterRequest capturedFilter = captorFilter.getValue();
        Pageable pageableCaptor = captor.getValue();

        assertThat(capturedFilter.status()).isNull();
        assertThat(capturedFilter.keyword()).isNull();
        assertThat(capturedFilter.minPrice()).isNull();
        assertThat(capturedFilter.maxPrice()).isNull();
        assertThat(pageableCaptor.getPageSize()).isEqualTo(10);
        assertThat(pageableCaptor.getPageNumber()).isEqualTo(0);

        verifyNoMoreInteractions(productService);
    }

    @Test
    void putProduct_whenValidRequest_returnsProductResponse() throws Exception {
        Long productId = 1L;

        String updateName = VALID_PRODUCT_NAME + "update";
        String updateDescription = VALID_PRODUCT_DESCRIPTION + "update";
        int updateQuantity = VALID_PRODUCT_QUANTITY + 10;

        ProductResponse response = new ProductResponse(
                productId,
                updateName,
                updateDescription,
                VALID_PRODUCT_PRICE,
                updateQuantity,
                VALID_PRODUCT_STATUS,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        ProductPutRequest request = new ProductPutRequest(
                updateName,
                updateDescription,
                VALID_PRODUCT_PRICE,
                updateQuantity
        );

        when(productService.putProduct(productId, request)).thenReturn(response);

        mockMvc.perform(put(PRODUCT_URI + "/" + productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(productId))
                .andExpect(jsonPath("$.name").value(updateName))
                .andExpect(jsonPath("$.description").value(updateDescription))
                .andExpect(jsonPath("$.quantity").value(updateQuantity))
                .andExpect(jsonPath("$.status").value(VALID_PRODUCT_STATUS.name()))
                .andExpect(jsonPath("$.price").value(VALID_PRODUCT_PRICE))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());

        ArgumentCaptor<ProductPutRequest> captor = ArgumentCaptor.forClass(ProductPutRequest.class);
        verify(productService).putProduct(eq(productId), captor.capture());

        ProductPutRequest capturedRequest = captor.getValue();

        assertThat(capturedRequest.name()).isEqualTo(request.name());
        assertThat(capturedRequest.description()).isEqualTo(request.description());
        assertThat(capturedRequest.price()).isEqualTo(request.price());
        assertThat(capturedRequest.quantity()).isEqualTo(request.quantity());

        verifyNoMoreInteractions(productService);
    }

    @Test
    void putProduct_whenProductIdIsInvalid_returnsBadRequest() throws Exception {
        String productId = INVALID_ID;

        ProductPutRequest request = new ProductPutRequest(
                VALID_PRODUCT_NAME,
                VALID_PRODUCT_DESCRIPTION,
                VALID_PRODUCT_PRICE,
                VALID_PRODUCT_QUANTITY
        );

        mockMvc.perform(put(PRODUCT_URI + "/" + productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(invalidParameter("id")))
                .andExpect(jsonPath("$.uri").value(PRODUCT_URI + "/" + productId));

        verifyNoInteractions(productService);
    }

    @Test
    void putProduct_whenProductNameIsBlank_returnsBadRequest() throws Exception {
        Long productId = 1L;

        ProductPutRequest request = new ProductPutRequest(
                "",
                VALID_PRODUCT_DESCRIPTION,
                VALID_PRODUCT_PRICE,
                VALID_PRODUCT_QUANTITY
        );

        mockMvc.perform(put(PRODUCT_URI + "/" + productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.uri").value(PRODUCT_URI + "/" + productId))
                .andExpect(jsonPath("$.fieldErrors.name", containsInAnyOrder(
                        productNameIsRequired(),
                        productNameLength())));

        verifyNoInteractions(productService);
    }

    @Test
    void putProduct_whenProductNameIsNull_returnsBadRequest() throws Exception {
        Long productId = 1L;

        ProductPutRequest request = new ProductPutRequest(
                null,
                VALID_PRODUCT_DESCRIPTION,
                VALID_PRODUCT_PRICE,
                VALID_PRODUCT_QUANTITY
        );

        mockMvc.perform(put(PRODUCT_URI + "/" + productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.uri").value(PRODUCT_URI + "/" + productId))
                .andExpect(jsonPath("$.fieldErrors.name", containsInAnyOrder(productNameIsRequired())));

        verifyNoInteractions(productService);
    }

    @Test
    void putProduct_whenProductNameIsMoreThan100Chars_returnsBadRequest() throws Exception {
        Long productId = 1L;

        ProductPutRequest request = new ProductPutRequest(
                INVALID_PRODUCT_NAME_MORE_THAN_LIMIT,
                VALID_PRODUCT_DESCRIPTION,
                VALID_PRODUCT_PRICE,
                VALID_PRODUCT_QUANTITY
        );

        mockMvc.perform(put(PRODUCT_URI + "/" + productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.uri").value(PRODUCT_URI + "/" + productId))
                .andExpect(jsonPath("$.fieldErrors.name", containsInAnyOrder(productNameLength())));

        verifyNoInteractions(productService);
    }

    @Test
    void putProduct_whenDescriptionIsBlank_returnsBadRequest() throws Exception {
        Long productId = 1L;

        ProductPutRequest request = new ProductPutRequest(
                VALID_PRODUCT_NAME,
                "",
                VALID_PRODUCT_PRICE,
                VALID_PRODUCT_QUANTITY
        );

        mockMvc.perform(put(PRODUCT_URI + "/" + productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.uri").value(PRODUCT_URI + "/" + productId))
                .andExpect(jsonPath("$.fieldErrors.description", containsInAnyOrder(
                        productDescriptionLength(),
                        productDescriptionIsRequired())));

        verifyNoInteractions(productService);
    }

    @Test
    void putProduct_whenDescriptionIsNull_returnsBadRequest() throws Exception {
        Long productId = 1L;

        ProductPutRequest request = new ProductPutRequest(
                VALID_PRODUCT_NAME,
                null,
                VALID_PRODUCT_PRICE,
                VALID_PRODUCT_QUANTITY
        );

        mockMvc.perform(put(PRODUCT_URI + "/" + productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.uri").value(PRODUCT_URI + "/" + productId))
                .andExpect(jsonPath("$.fieldErrors.description", containsInAnyOrder(productDescriptionIsRequired())));

        verifyNoInteractions(productService);
    }

    @Test
    void putProduct_whenDescriptionLengthIsLessThan5_returnsBadRequest() throws Exception {
        Long productId = 1L;

        ProductPutRequest request = new ProductPutRequest(
                VALID_PRODUCT_NAME,
                INVALID_PRODUCT_DESCRIPTION_LESS_THAN_LIMIT,
                VALID_PRODUCT_PRICE,
                VALID_PRODUCT_QUANTITY
        );

        mockMvc.perform(put(PRODUCT_URI + "/" + productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.uri").value(PRODUCT_URI + "/" + productId))
                .andExpect(jsonPath("$.fieldErrors.description", containsInAnyOrder(productDescriptionLength())));

        verifyNoInteractions(productService);
    }

    @Test
    void putProduct_whenDescriptionLengthIsMoreThan1000_returnsBadRequest() throws Exception {
        Long productId = 1L;

        ProductPutRequest request = new ProductPutRequest(
                VALID_PRODUCT_NAME,
                INVALID_PRODUCT_DESCRIPTION_MORE_THAN_LIMIT,
                VALID_PRODUCT_PRICE,
                VALID_PRODUCT_QUANTITY
        );

        mockMvc.perform(put(PRODUCT_URI + "/" + productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.uri").value(PRODUCT_URI + "/" + productId))
                .andExpect(jsonPath("$.fieldErrors.description", containsInAnyOrder(productDescriptionLength())));

        verifyNoInteractions(productService);
    }

    @Test
    void putProduct_whenPriceIsNull_returnsBadRequest() throws Exception {
        Long productId = 1L;

        ProductPutRequest request = new ProductPutRequest(
                VALID_PRODUCT_NAME,
                VALID_PRODUCT_DESCRIPTION,
                null,
                VALID_PRODUCT_QUANTITY
        );

        mockMvc.perform(put(PRODUCT_URI + "/" + productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.uri").value(PRODUCT_URI + "/" + productId))
                .andExpect(jsonPath("$.fieldErrors.price", containsInAnyOrder(productPriceIsRequired())));

        verifyNoInteractions(productService);
    }

    @Test
    void putProduct_whenPriceIsZero_returnsBadRequest() throws Exception {
        Long productId = 1L;

        ProductPutRequest request = new ProductPutRequest(
                VALID_PRODUCT_NAME,
                VALID_PRODUCT_DESCRIPTION,
                BigDecimal.ZERO,
                VALID_PRODUCT_QUANTITY
        );

        mockMvc.perform(put(PRODUCT_URI + "/" + productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.uri").value(PRODUCT_URI + "/" + productId))
                .andExpect(jsonPath("$.fieldErrors.price", containsInAnyOrder(productPriceZero())));

        verifyNoInteractions(productService);
    }

    @Test
    void putProduct_whenQuantityIsNull_returnsBadRequest() throws Exception {
        Long productId = 1L;

        ProductPutRequest request = new ProductPutRequest(
                VALID_PRODUCT_NAME,
                VALID_PRODUCT_DESCRIPTION,
                VALID_PRODUCT_PRICE,
                null
        );

        mockMvc.perform(put(PRODUCT_URI + "/" + productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.uri").value(PRODUCT_URI + "/" + productId))
                .andExpect(jsonPath("$.fieldErrors.quantity", containsInAnyOrder(productQuantityIsRequired())));

        verifyNoInteractions(productService);
    }

    @Test
    void putProduct_whenQuantityIsNegative_returnsBadRequest() throws Exception {
        Long productId = 1L;

        ProductPutRequest request = new ProductPutRequest(
                VALID_PRODUCT_NAME,
                VALID_PRODUCT_DESCRIPTION,
                VALID_PRODUCT_PRICE,
                -1
        );

        mockMvc.perform(put(PRODUCT_URI + "/" + productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.uri").value(PRODUCT_URI + "/" + productId))
                .andExpect(jsonPath("$.fieldErrors.quantity", containsInAnyOrder(productNegativeQuantity())));

        verifyNoInteractions(productService);
    }

    @Test
    void putProduct_whenProductNotFound_returnsNotFound() throws Exception {
        Long productId = 1L;

        ProductPutRequest request = new ProductPutRequest(
                VALID_PRODUCT_NAME,
                VALID_PRODUCT_DESCRIPTION,
                VALID_PRODUCT_PRICE,
                VALID_PRODUCT_QUANTITY
        );

        doThrow(new NoResourceFoundException(productNotFound(productId)))
                .when(productService).putProduct(productId, request);

        mockMvc.perform(put(PRODUCT_URI + "/" + productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.NOT_FOUND.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(productNotFound(productId)))
                .andExpect(jsonPath("$.uri").value(PRODUCT_URI + "/" + productId));

        verify(productService).putProduct(productId, request);
        verifyNoMoreInteractions(productService);
    }


    @Test
    void patchProduct_whenRequestValid_returnsProductResponse() throws Exception {
        Long productId = 1L;

        ProductResponse productResponse = new ProductResponse(
                productId,
                VALID_PRODUCT_NAME,
                VALID_PRODUCT_DESCRIPTION,
                VALID_PRODUCT_PRICE,
                VALID_PRODUCT_QUANTITY,
                VALID_PRODUCT_STATUS,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        ProductPatchRequest request = new ProductPatchRequest(
                VALID_PRODUCT_NAME,
                VALID_PRODUCT_DESCRIPTION,
                VALID_PRODUCT_PRICE,
                VALID_PRODUCT_QUANTITY
        );

        when(productService.patchProduct(productId, request)).thenReturn(productResponse);

        mockMvc.perform(patch(PRODUCT_URI + "/" + productId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(productId))
                .andExpect(jsonPath("$.name").value(request.name()))
                .andExpect(jsonPath("$.description").value(request.description()))
                .andExpect(jsonPath("$.price").value(request.price()))
                .andExpect(jsonPath("$.status").value(VALID_PRODUCT_STATUS.name()))
                .andExpect(jsonPath("$.quantity").value(request.quantity()))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());

        ArgumentCaptor<ProductPatchRequest> captor = ArgumentCaptor.forClass(ProductPatchRequest.class);
        verify(productService).patchProduct(eq(productId), captor.capture());

        ProductPatchRequest capturedRequest = captor.getValue();

        assertThat(capturedRequest.name()).isEqualTo(request.name());
        assertThat(capturedRequest.description()).isEqualTo(request.description());
        assertThat(capturedRequest.price()).isEqualByComparingTo(request.price());
        assertThat(capturedRequest.quantity()).isEqualTo(request.quantity());

        verifyNoMoreInteractions(productService);
    }

    @Test
    void patchProduct_whenRequestHasAllFieldsNull_returnsProductResponse() throws Exception {
        Long productId = 1L;

        ProductResponse productResponse = new ProductResponse(
                productId,
                VALID_PRODUCT_NAME,
                VALID_PRODUCT_DESCRIPTION,
                VALID_PRODUCT_PRICE,
                VALID_PRODUCT_QUANTITY,
                VALID_PRODUCT_STATUS,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        ProductPatchRequest request = new ProductPatchRequest(
                null,
                null,
                null,
                null
        );

        when(productService.patchProduct(productId, request)).thenReturn(productResponse);

        mockMvc.perform(patch(PRODUCT_URI + "/" + productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(productId))
                .andExpect(jsonPath("$.name").value(VALID_PRODUCT_NAME))
                .andExpect(jsonPath("$.description").value(VALID_PRODUCT_DESCRIPTION))
                .andExpect(jsonPath("$.price").value(VALID_PRODUCT_PRICE))
                .andExpect(jsonPath("$.status").value(ProductStatus.ACTIVE.name()))
                .andExpect(jsonPath("$.quantity").value(VALID_PRODUCT_QUANTITY))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());

        ArgumentCaptor<ProductPatchRequest> captor = ArgumentCaptor.forClass(ProductPatchRequest.class);
        verify(productService).patchProduct(eq(productId), captor.capture());

        ProductPatchRequest capturedRequest = captor.getValue();

        assertThat(capturedRequest.name()).isNull();
        assertThat(capturedRequest.description()).isNull();
        assertThat(capturedRequest.price()).isNull();
        assertThat(capturedRequest.quantity()).isNull();

        verifyNoMoreInteractions(productService);
    }

    @Test
    void patchProduct_whenRequestHasPartialFields_returnsProductResponse() throws Exception {
        Long productId = 1L;

        ProductResponse productResponse = new ProductResponse(
                productId,
                VALID_PRODUCT_NAME,
                VALID_PRODUCT_DESCRIPTION,
                VALID_PRODUCT_PRICE,
                VALID_PRODUCT_QUANTITY,
                VALID_PRODUCT_STATUS,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        ProductPatchRequest request = new ProductPatchRequest(
                VALID_PRODUCT_NAME,
                null,
                VALID_PRODUCT_PRICE,
                null
        );

        when(productService.patchProduct(productId, request)).thenReturn(productResponse);

        mockMvc.perform(patch(PRODUCT_URI + "/" + productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(productId))
                .andExpect(jsonPath("$.name").value(VALID_PRODUCT_NAME))
                .andExpect(jsonPath("$.description").value(VALID_PRODUCT_DESCRIPTION))
                .andExpect(jsonPath("$.price").value(VALID_PRODUCT_PRICE))
                .andExpect(jsonPath("$.status").value(ProductStatus.ACTIVE.name()))
                .andExpect(jsonPath("$.quantity").value(VALID_PRODUCT_QUANTITY))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());

        ArgumentCaptor<ProductPatchRequest> captor = ArgumentCaptor.forClass(ProductPatchRequest.class);
        verify(productService).patchProduct(eq(productId), captor.capture());

        ProductPatchRequest capturedRequest = captor.getValue();

        assertThat(capturedRequest.name()).isEqualTo(VALID_PRODUCT_NAME);
        assertThat(capturedRequest.description()).isNull();
        assertThat(capturedRequest.price()).isEqualByComparingTo(VALID_PRODUCT_PRICE);
        assertThat(capturedRequest.quantity()).isNull();

        verifyNoMoreInteractions(productService);
    }

    @Test
    void patchProduct_whenInvalidId_returnsBadRequest() throws Exception {
        String productId = INVALID_ID;

        ProductPatchRequest request = new ProductPatchRequest(
                VALID_PRODUCT_NAME,
                VALID_PRODUCT_DESCRIPTION,
                VALID_PRODUCT_PRICE,
                VALID_PRODUCT_QUANTITY
        );

        mockMvc.perform(patch(PRODUCT_URI + "/" + productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(invalidParameter("id")))
                .andExpect(jsonPath("$.uri").value(PRODUCT_URI + "/" + productId));

        verifyNoInteractions(productService);
    }

    @Test
    void patchProduct_whenProductNameIsBlank_returnsBadRequest() throws Exception {
        Long productId = 1L;

        ProductPatchRequest request = new ProductPatchRequest(
                "",
                VALID_PRODUCT_DESCRIPTION,
                VALID_PRODUCT_PRICE,
                VALID_PRODUCT_QUANTITY
        );

        mockMvc.perform(patch(PRODUCT_URI + "/" + productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.uri").value(PRODUCT_URI + "/" + productId))
                .andExpect(jsonPath("$.fieldErrors.name", containsInAnyOrder(
                        productNameLength(),
                        productNameIsEmpty()
                )));

        verifyNoInteractions(productService);
    }

    @Test
    void patchProduct_whenProductNameIsMoreThan100Chars_returnsBadRequest() throws Exception {
        Long productId = 1L;

        ProductPatchRequest request = new ProductPatchRequest(
                INVALID_PRODUCT_NAME_MORE_THAN_LIMIT,
                VALID_PRODUCT_DESCRIPTION,
                VALID_PRODUCT_PRICE,
                VALID_PRODUCT_QUANTITY
        );

        mockMvc.perform(patch(PRODUCT_URI + "/" + productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.uri").value(PRODUCT_URI + "/" + productId))
                .andExpect(jsonPath("$.fieldErrors.name", containsInAnyOrder(
                        productNameLength()
                )));

        verifyNoInteractions(productService);
    }

    @Test
    void patchProduct_whenDescriptionIsEmpty_returnsBadRequest() throws Exception {
        Long productId = 1L;

        ProductPatchRequest request = new ProductPatchRequest(
                VALID_PRODUCT_NAME,
                "",
                VALID_PRODUCT_PRICE,
                VALID_PRODUCT_QUANTITY
        );

        mockMvc.perform(patch(PRODUCT_URI + "/" + productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.uri").value(PRODUCT_URI + "/" + productId))
                .andExpect(jsonPath("$.fieldErrors.description", containsInAnyOrder(
                        productDescriptionLength(),
                        productDescriptionIsEmpty()
                )));

        verifyNoInteractions(productService);
    }

    @Test
    void patchProduct_whenDescriptionIsLessThan5Chars_returnsBadRequest() throws Exception {
        Long productId = 1L;

        ProductPatchRequest request = new ProductPatchRequest(
                VALID_PRODUCT_NAME,
                INVALID_PRODUCT_DESCRIPTION_LESS_THAN_LIMIT,
                VALID_PRODUCT_PRICE,
                VALID_PRODUCT_QUANTITY
        );

        mockMvc.perform(patch(PRODUCT_URI + "/" + productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.uri").value(PRODUCT_URI + "/" + productId))
                .andExpect(jsonPath("$.fieldErrors.description", containsInAnyOrder(
                        productDescriptionLength()
                )));

        verifyNoInteractions(productService);
    }

    @Test
    void patchProduct_whenDescriptionIsMoreThan1000Chars_returnsBadRequest() throws Exception {
        Long productId = 1L;

        ProductPatchRequest request = new ProductPatchRequest(
                VALID_PRODUCT_NAME,
                INVALID_PRODUCT_DESCRIPTION_MORE_THAN_LIMIT,
                VALID_PRODUCT_PRICE,
                VALID_PRODUCT_QUANTITY
        );

        mockMvc.perform(patch(PRODUCT_URI + "/" + productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.uri").value(PRODUCT_URI + "/" + productId))
                .andExpect(jsonPath("$.fieldErrors.description", containsInAnyOrder(
                        productDescriptionLength()
                )));

        verifyNoInteractions(productService);
    }

    @Test
    void patchProduct_whenPriceIsZero_returnsBadRequest() throws Exception {
        Long productId = 1L;

        ProductPatchRequest request = new ProductPatchRequest(
                VALID_PRODUCT_NAME,
                VALID_PRODUCT_DESCRIPTION,
                BigDecimal.ZERO,
                VALID_PRODUCT_QUANTITY
        );

        mockMvc.perform(patch(PRODUCT_URI + "/" + productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.uri").value(PRODUCT_URI + "/" + productId))
                .andExpect(jsonPath("$.fieldErrors.price", containsInAnyOrder(
                        productPriceZero()
                )));

        verifyNoInteractions(productService);
    }

    @Test
    void patchProduct_whenQuantityIsNegative_returnsBadRequest() throws Exception {
        Long productId = 1L;

        ProductPatchRequest request = new ProductPatchRequest(
                VALID_PRODUCT_NAME,
                VALID_PRODUCT_DESCRIPTION,
                VALID_PRODUCT_PRICE,
                INVALID_PRODUCT_NEGATIVE_QUANTITY
        );

        mockMvc.perform(patch(PRODUCT_URI + "/" + productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.uri").value(PRODUCT_URI + "/" + productId))
                .andExpect(jsonPath("$.fieldErrors.quantity", containsInAnyOrder(
                        productNegativeQuantity()
                )));

        verifyNoInteractions(productService);
    }

    @Test
    void patchProduct_whenProductNotFound_returnsNotFound() throws Exception {
        Long productId = 1L;

        ProductPatchRequest request = new ProductPatchRequest(
                VALID_PRODUCT_NAME,
                VALID_PRODUCT_DESCRIPTION,
                VALID_PRODUCT_PRICE,
                VALID_PRODUCT_QUANTITY
        );

        doThrow(new NoResourceFoundException(productNotFound(productId)))
                .when(productService).patchProduct(productId, request);

        mockMvc.perform(patch(PRODUCT_URI + "/" + productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.NOT_FOUND.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(productNotFound(productId)))
                .andExpect(jsonPath("$.uri").value(PRODUCT_URI + "/" + productId));

        verify(productService).patchProduct(productId, request);
        verifyNoMoreInteractions(productService);
    }

    @Test
    void deleteProduct_whenProductIdIsValid_returnsNoContent() throws Exception {
        Long productId = 1L;

        mockMvc.perform(delete(PRODUCT_URI + "/" + productId))
                .andExpect(status().isNoContent());

        verify(productService).deleteProduct(productId);
        verifyNoMoreInteractions(productService);
    }

    @Test
    void deleteProduct_whenProductIdIsInvalid_returnsBadRequest() throws Exception {
        String productId = INVALID_ID;

        mockMvc.perform(delete(PRODUCT_URI + "/" + productId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(invalidParameter("id")))
                .andExpect(jsonPath("$.uri").value(PRODUCT_URI + "/" + productId));

        verifyNoInteractions(productService);
    }

    @Test
    void deleteProduct_whenProductIdIsNotFound_returnsNotFound() throws Exception {
        Long productId = 1L;

        doThrow(new NoResourceFoundException(productNotFound(productId)))
                .when(productService).deleteProduct(productId);

        mockMvc.perform(delete(PRODUCT_URI + "/" + productId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.NOT_FOUND.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(productNotFound(productId)))
                .andExpect(jsonPath("$.uri").value(PRODUCT_URI + "/" + productId));

        verify(productService).deleteProduct(productId);
        verifyNoMoreInteractions(productService);
    }
}
