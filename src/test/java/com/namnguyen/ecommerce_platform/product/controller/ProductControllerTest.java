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
import static com.namnguyen.ecommerce_platform.testutil.messages.CommonTestMessages.VALIDATION_FAILED;
import static com.namnguyen.ecommerce_platform.testutil.messages.CommonTestMessages.invalidParameter;
import static com.namnguyen.ecommerce_platform.testutil.messages.ProductTestMessages.*;
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

        ProductCreateRequest productCreateRequest = new ProductCreateRequest(
                VALID_PRODUCT_NAME,
                VALID_PRODUCT_DESCRIPTION,
                VALID_PRODUCT_PRICE,
                VALID_PRODUCT_QUANTITY
        );

        ProductResponse productResponse = new ProductResponse(
                productId,
                VALID_PRODUCT_NAME,
                VALID_PRODUCT_DESCRIPTION,
                VALID_PRODUCT_PRICE,
                VALID_PRODUCT_QUANTITY,
                ProductStatus.ACTIVE,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(productService.createProduct(productCreateRequest)).thenReturn(productResponse);

        mockMvc.perform(post(PRODUCT_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(productCreateRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(productId))
                .andExpect(jsonPath("$.name").value(VALID_PRODUCT_NAME))
                .andExpect(jsonPath("$.description").value(VALID_PRODUCT_DESCRIPTION))
                .andExpect(jsonPath("$.price").value(VALID_PRODUCT_PRICE.doubleValue()))
                .andExpect(jsonPath("$.quantity").value(VALID_PRODUCT_QUANTITY))
                .andExpect(jsonPath("$.status").value(ProductStatus.ACTIVE.name()))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());

        ArgumentCaptor<ProductCreateRequest> productCreateRequestCaptor = ArgumentCaptor.forClass(ProductCreateRequest.class);
        verify(productService).createProduct(productCreateRequestCaptor.capture());

        ProductCreateRequest capturedProductRequest = productCreateRequestCaptor.getValue();

        assertThat(capturedProductRequest.name()).isEqualTo(VALID_PRODUCT_NAME);
        assertThat(capturedProductRequest.description()).isEqualTo(VALID_PRODUCT_DESCRIPTION);
        assertThat(capturedProductRequest.price()).isEqualByComparingTo(VALID_PRODUCT_PRICE);
        assertThat(capturedProductRequest.quantity()).isEqualTo(VALID_PRODUCT_QUANTITY);

        verifyNoMoreInteractions(productService);
    }

    @Test
    void createProduct_whenNameIsEmpty_returnsBadRequest() throws Exception {
        ProductCreateRequest productCreateRequest = new ProductCreateRequest(
                "",
                VALID_PRODUCT_DESCRIPTION,
                VALID_PRODUCT_PRICE,
                VALID_PRODUCT_QUANTITY
        );

        mockMvc.perform(post(PRODUCT_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productCreateRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                .andExpect(jsonPath("$.uri").value(PRODUCT_URI))
                .andExpect(jsonPath("$.fieldErrors.name",
                        containsInAnyOrder(
                                PRODUCT_NAME_IS_REQUIRED,
                                PRODUCT_NAME_IS_INVALID)));

        verifyNoInteractions(productService);
    }

    @Test
    void createProduct_whenNameIsNull_returnsBadRequest() throws Exception {
        ProductCreateRequest productCreateRequest = new ProductCreateRequest(
                null,
                VALID_PRODUCT_DESCRIPTION,
                VALID_PRODUCT_PRICE,
                VALID_PRODUCT_QUANTITY
        );

        mockMvc.perform(post(PRODUCT_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productCreateRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                .andExpect(jsonPath("$.uri").value(PRODUCT_URI))
                .andExpect(jsonPath("$.fieldErrors.name", containsInAnyOrder(PRODUCT_NAME_IS_REQUIRED)));

        verifyNoInteractions(productService);
    }

    @Test
    void createProduct_whenNameIsMoreThan100_returnsBadRequest() throws Exception {
        ProductCreateRequest productCreateRequest = new ProductCreateRequest(
                INVALID_PRODUCT_NAME_MORE_THAN_LIMIT,
                VALID_PRODUCT_DESCRIPTION,
                VALID_PRODUCT_PRICE,
                VALID_PRODUCT_QUANTITY
        );

        mockMvc.perform(post(PRODUCT_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productCreateRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                .andExpect(jsonPath("$.uri").value(PRODUCT_URI))
                .andExpect(jsonPath("$.fieldErrors.name", containsInAnyOrder(PRODUCT_NAME_IS_INVALID)));

        verifyNoInteractions(productService);
    }

    @Test
    void createProduct_whenDescriptionIsEmpty_returnsBadRequest() throws Exception {
        ProductCreateRequest productCreateRequest = new ProductCreateRequest(
                VALID_PRODUCT_NAME,
                "",
                VALID_PRODUCT_PRICE,
                VALID_PRODUCT_QUANTITY
        );

        mockMvc.perform(post(PRODUCT_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productCreateRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                .andExpect(jsonPath("$.uri").value(PRODUCT_URI))
                .andExpect(jsonPath("$.fieldErrors.description", containsInAnyOrder(
                        PRODUCT_DESCRIPTION_IS_INVALID,
                        PRODUCT_DESCRIPTION_IS_REQUIRED)));

        verifyNoInteractions(productService);
    }

    @Test
    void createProduct_whenDescriptionIsNull_returnsBadRequest() throws Exception {
        ProductCreateRequest productCreateRequest = new ProductCreateRequest(
                VALID_PRODUCT_NAME,
                null,
                VALID_PRODUCT_PRICE,
                VALID_PRODUCT_QUANTITY
        );

        mockMvc.perform(post(PRODUCT_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productCreateRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                .andExpect(jsonPath("$.uri").value(PRODUCT_URI))
                .andExpect(jsonPath("$.fieldErrors.description", containsInAnyOrder(PRODUCT_DESCRIPTION_IS_REQUIRED)));

        verifyNoInteractions(productService);
    }

    @Test
    void createProduct_whenDescriptionIsLessThan5_returnsBadRequest() throws Exception {
        ProductCreateRequest productCreateRequest = new ProductCreateRequest(
                VALID_PRODUCT_NAME,
                INVALID_PRODUCT_DESCRIPTION_LESS_THAN_LIMIT,
                VALID_PRODUCT_PRICE,
                VALID_PRODUCT_QUANTITY
        );

        mockMvc.perform(post(PRODUCT_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productCreateRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                .andExpect(jsonPath("$.uri").value(PRODUCT_URI))
                .andExpect(jsonPath("$.fieldErrors.description", containsInAnyOrder(PRODUCT_DESCRIPTION_IS_INVALID)));

        verifyNoInteractions(productService);
    }

    @Test
    void createProduct_whenDescriptionIsMoreThan1000_returnsBadRequest() throws Exception {
        ProductCreateRequest productCreateRequest = new ProductCreateRequest(
                VALID_PRODUCT_NAME,
                INVALID_PRODUCT_DESCRIPTION_MORE_THAN_LIMIT,
                VALID_PRODUCT_PRICE,
                VALID_PRODUCT_QUANTITY
        );

        mockMvc.perform(post(PRODUCT_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productCreateRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                .andExpect(jsonPath("$.uri").value(PRODUCT_URI))
                .andExpect(jsonPath("$.fieldErrors.description", containsInAnyOrder(PRODUCT_DESCRIPTION_IS_INVALID)));

        verifyNoInteractions(productService);
    }

    @Test
    void createProduct_whenPriceIsNull_returnsBadRequest() throws Exception {
        ProductCreateRequest productCreateRequest = new ProductCreateRequest(
                VALID_PRODUCT_NAME,
                VALID_PRODUCT_DESCRIPTION,
                null,
                VALID_PRODUCT_QUANTITY
        );

        mockMvc.perform(post(PRODUCT_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productCreateRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                .andExpect(jsonPath("$.uri").value(PRODUCT_URI))
                .andExpect(jsonPath("$.fieldErrors.price", containsInAnyOrder(PRODUCT_PRICE_IS_REQUIRED)));

        verifyNoInteractions(productService);
    }

    @Test
    void createProduct_whenPriceIsZero_returnsBadRequest() throws Exception {
        ProductCreateRequest productCreateRequest = new ProductCreateRequest(
                VALID_PRODUCT_NAME,
                VALID_PRODUCT_DESCRIPTION,
                INVALID_PRODUCT_PRICE_ZERO,
                VALID_PRODUCT_QUANTITY
        );

        mockMvc.perform(post(PRODUCT_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productCreateRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                .andExpect(jsonPath("$.uri").value(PRODUCT_URI))
                .andExpect(jsonPath("$.fieldErrors.price", containsInAnyOrder(PRODUCT_PRICE_IS_INVALID)));

        verifyNoInteractions(productService);
    }

    @Test
    void createProduct_whenQuantityIsNull_returnsBadRequest() throws Exception {
        ProductCreateRequest productCreateRequest = new ProductCreateRequest(
                VALID_PRODUCT_NAME,
                VALID_PRODUCT_DESCRIPTION,
                VALID_PRODUCT_PRICE,
                null
        );

        mockMvc.perform(post(PRODUCT_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productCreateRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                .andExpect(jsonPath("$.uri").value(PRODUCT_URI))
                .andExpect(jsonPath("$.fieldErrors.quantity", containsInAnyOrder(PRODUCT_QUANTITY_IS_REQUIRED)));

        verifyNoInteractions(productService);
    }

    @Test
    void createProduct_whenQuantityIsNegative_returnsBadRequest() throws Exception {
        ProductCreateRequest productCreateRequest = new ProductCreateRequest(
                VALID_PRODUCT_NAME,
                VALID_PRODUCT_DESCRIPTION,
                VALID_PRODUCT_PRICE,
                INVALID_PRODUCT_NEGATIVE_QUANTITY
        );

        mockMvc.perform(post(PRODUCT_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productCreateRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                .andExpect(jsonPath("$.uri").value(PRODUCT_URI))
                .andExpect(jsonPath("$.fieldErrors.quantity", containsInAnyOrder(PRODUCT_QUANTITY_IS_INVALID)));

        verifyNoInteractions(productService);
    }

    @Test
    void getProductById_whenProductExists_returnsProductResponse() throws Exception {
        Long productId = 1L;

        ProductResponse productResponse = new ProductResponse(
                productId,
                VALID_PRODUCT_NAME,
                VALID_PRODUCT_DESCRIPTION,
                VALID_PRODUCT_PRICE,
                VALID_PRODUCT_QUANTITY,
                ProductStatus.ACTIVE,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(productService.getProductById(productId)).thenReturn(productResponse);

        mockMvc.perform(get(productUri(productId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(productId))
                .andExpect(jsonPath("$.name").value(VALID_PRODUCT_NAME))
                .andExpect(jsonPath("$.description").value(VALID_PRODUCT_DESCRIPTION))
                .andExpect(jsonPath("$.price").value(VALID_PRODUCT_PRICE.doubleValue()))
                .andExpect(jsonPath("$.quantity").value(VALID_PRODUCT_QUANTITY))
                .andExpect(jsonPath("$.status").value(ProductStatus.ACTIVE.name()))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());

        verify(productService).getProductById(productId);
        verifyNoMoreInteractions(productService);
    }

    @Test
    void getProductById_whenProductIdIsInvalid_returnsBadRequest() throws Exception {
        String productId = INVALID_ID;

        mockMvc.perform(get(productUri(productId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(invalidParameter("id")))
                .andExpect(jsonPath("$.uri").value(productUri(productId)));

        verifyNoInteractions(productService);
    }

    @Test
    void getProductById_whenProductNotExists_returnsNotFound() throws Exception {
        Long productId = 1L;

        when(productService.getProductById(productId))
                .thenThrow(new NoResourceFoundException(productNotFoundWithId(productId)));

        mockMvc.perform(get(productUri(productId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.NOT_FOUND.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(productNotFoundWithId(productId)))
                .andExpect(jsonPath("$.uri").value(productUri(productId)));


        verify(productService).getProductById(productId);
        verifyNoMoreInteractions(productService);
    }

    @Test
    void getAllProducts_whenProductsExist_returnsPageOfProducts() throws Exception {
        Long firstProductId = 1L;
        Long secondProductId = 2L;

        ProductResponse firstProductResponse = new ProductResponse(
                firstProductId,
                VALID_PRODUCT_NAME,
                VALID_PRODUCT_DESCRIPTION,
                VALID_PRODUCT_PRICE,
                VALID_PRODUCT_QUANTITY,
                ProductStatus.ACTIVE,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        ProductResponse secondProductResponse = new ProductResponse(
                secondProductId,
                VALID_PRODUCT_NAME + "1",
                VALID_PRODUCT_DESCRIPTION+ "1",
                VALID_PRODUCT_PRICE,
                VALID_PRODUCT_QUANTITY + 1,
                ProductStatus.ACTIVE,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        List<ProductResponse> productResponses = List.of(firstProductResponse, secondProductResponse);
        Pageable pageable = PageRequest.of(0, 10);
        Page<ProductResponse> productPageResponse = new PageImpl<>(productResponses, pageable, productResponses.size());

        when(productService.getAllProducts(any(ProductFilterRequest.class), any(Pageable.class))).thenReturn(productPageResponse);

        mockMvc.perform(get(PRODUCT_URI)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].id").value(firstProductId))
                .andExpect(jsonPath("$.content[0].name").value(VALID_PRODUCT_NAME))
                .andExpect(jsonPath("$.content[0].description").value(VALID_PRODUCT_DESCRIPTION))
                .andExpect(jsonPath("$.content[0].price").value(VALID_PRODUCT_PRICE.doubleValue()))
                .andExpect(jsonPath("$.content[0].quantity").value(VALID_PRODUCT_QUANTITY))
                .andExpect(jsonPath("$.content[0].status").value(ProductStatus.ACTIVE.name()))
                .andExpect(jsonPath("$.content[0].createdAt").exists())
                .andExpect(jsonPath("$.content[0].updatedAt").exists())
                .andExpect(jsonPath("$.content[1].id").value(secondProductId))
                .andExpect(jsonPath("$.content[1].name").value(VALID_PRODUCT_NAME+ "1"))
                .andExpect(jsonPath("$.content[1].description").value(VALID_PRODUCT_DESCRIPTION+ "1"))
                .andExpect(jsonPath("$.content[1].price").value(VALID_PRODUCT_PRICE.doubleValue()))
                .andExpect(jsonPath("$.content[1].quantity").value(VALID_PRODUCT_QUANTITY + 1))
                .andExpect(jsonPath("$.content[1].status").value(ProductStatus.ACTIVE.name()))
                .andExpect(jsonPath("$.content[1].createdAt").exists())
                .andExpect(jsonPath("$.content[1].updatedAt").exists())
                .andExpect(jsonPath("$.numberOfElements").value(2))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1));

        ArgumentCaptor<ProductFilterRequest> productFilterRequestCaptor = ArgumentCaptor.forClass(ProductFilterRequest.class);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(productService).getAllProducts(productFilterRequestCaptor.capture(), pageableCaptor.capture());

        ProductFilterRequest capturedProductFilter = productFilterRequestCaptor.getValue();
        Pageable capturedPageable = pageableCaptor.getValue();

        assertThat(capturedProductFilter.status()).isNull();
        assertThat(capturedProductFilter.keyword()).isNull();
        assertThat(capturedProductFilter.minPrice()).isNull();
        assertThat(capturedProductFilter.maxPrice()).isNull();
        assertThat(capturedPageable.getPageSize()).isEqualTo(10);
        assertThat(capturedPageable.getPageNumber()).isEqualTo(0);
        assertThat(capturedPageable.getSort()).contains(Sort.Order.asc("id"));

        verifyNoMoreInteractions(productService);
    }

    @Test
    void getAllProducts_whenFilterAvailable_returnsPageOfProducts() throws Exception {
        Long firstProductId = 1L;
        Long secondProductId = 2L;

        ProductResponse firstProductResponse = new ProductResponse(
                firstProductId,
                VALID_PRODUCT_NAME,
                VALID_PRODUCT_DESCRIPTION,
                VALID_PRODUCT_PRICE,
                VALID_PRODUCT_QUANTITY,
                ProductStatus.ACTIVE,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        ProductResponse secondProductResponse = new ProductResponse(
                secondProductId,
                VALID_PRODUCT_NAME + "1",
                VALID_PRODUCT_DESCRIPTION+ "1",
                VALID_PRODUCT_PRICE,
                VALID_PRODUCT_QUANTITY + 1,
                ProductStatus.ACTIVE,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        List<ProductResponse> productResponses = List.of(firstProductResponse, secondProductResponse);
        Pageable pageable = PageRequest.of(0, 10);
        Page<ProductResponse> productPageResponse = new PageImpl<>(productResponses, pageable, productResponses.size());

        when(productService.getAllProducts(any(ProductFilterRequest.class), any(Pageable.class))).thenReturn(productPageResponse);

        mockMvc.perform(get(PRODUCT_URI)
                        .param("page", "0")
                        .param("size", "10")
                        .param("status", ProductStatus.ACTIVE.name())
                        .param("keyword", "Test")
                        .param("minPrice", "0.0")
                        .param("maxPrice", "20.0" ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].id").value(firstProductId))
                .andExpect(jsonPath("$.content[0].name").value(VALID_PRODUCT_NAME))
                .andExpect(jsonPath("$.content[0].description").value(VALID_PRODUCT_DESCRIPTION))
                .andExpect(jsonPath("$.content[0].price").value(VALID_PRODUCT_PRICE.doubleValue()))
                .andExpect(jsonPath("$.content[0].quantity").value(VALID_PRODUCT_QUANTITY))
                .andExpect(jsonPath("$.content[0].status").value(ProductStatus.ACTIVE.name()))
                .andExpect(jsonPath("$.content[0].createdAt").exists())
                .andExpect(jsonPath("$.content[0].updatedAt").exists())
                .andExpect(jsonPath("$.content[1].id").value(secondProductId))
                .andExpect(jsonPath("$.content[1].name").value(VALID_PRODUCT_NAME+ "1"))
                .andExpect(jsonPath("$.content[1].description").value(VALID_PRODUCT_DESCRIPTION+ "1"))
                .andExpect(jsonPath("$.content[1].price").value(VALID_PRODUCT_PRICE.doubleValue()))
                .andExpect(jsonPath("$.content[1].quantity").value(VALID_PRODUCT_QUANTITY + 1))
                .andExpect(jsonPath("$.content[1].status").value(ProductStatus.ACTIVE.name()))
                .andExpect(jsonPath("$.content[1].createdAt").exists())
                .andExpect(jsonPath("$.content[1].updatedAt").exists())
                .andExpect(jsonPath("$.numberOfElements").value(2))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1));

        ArgumentCaptor<ProductFilterRequest> productFilterRequestCaptor = ArgumentCaptor.forClass(ProductFilterRequest.class);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(productService).getAllProducts(productFilterRequestCaptor.capture(), pageableCaptor.capture());

        ProductFilterRequest capturedProductFilter = productFilterRequestCaptor.getValue();
        Pageable capturedPageable = pageableCaptor.getValue();

        assertThat(capturedProductFilter.status()).isEqualTo(ProductStatus.ACTIVE);
        assertThat(capturedProductFilter.keyword()).isEqualTo("Test");
        assertThat(capturedProductFilter.minPrice()).isEqualByComparingTo(BigDecimal.valueOf(0.0));
        assertThat(capturedProductFilter.maxPrice()).isEqualByComparingTo(BigDecimal.valueOf(20.0));
        assertThat(capturedPageable.getPageSize()).isEqualTo(10);
        assertThat(capturedPageable.getPageNumber()).isEqualTo(0);
        assertThat(capturedPageable.getSort()).contains(Sort.Order.asc("id"));

        verifyNoMoreInteractions(productService);
    }

    @Test
    void getAllProducts_whenStatusFilterIsInvalid_returnsBadRequest() throws Exception {
        mockMvc.perform(get(PRODUCT_URI)
                        .param("page", "0")
                        .param("size", "10")
                        .param("status", INVALID_ENUM_VALUE)
                        .param("keyword", "Test")
                        .param("minPrice", "0.0")
                        .param("maxPrice", "20.0" ))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                .andExpect(jsonPath("$.uri").value(PRODUCT_URI))
                .andExpect(jsonPath("$.fieldErrors.status", containsInAnyOrder(invalidParameter("status"))));

        verifyNoInteractions(productService);
    }

    @Test
    void getAllProducts_whenMinPriceIsInvalid_returnsBadRequest() throws Exception {
        mockMvc.perform(get(PRODUCT_URI)
                        .param("page", "0")
                        .param("size", "10")
                        .param("status", ProductStatus.ACTIVE.name())
                        .param("keyword", "Test")
                        .param("minPrice", "abc")
                        .param("maxPrice", "20.0" ))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                .andExpect(jsonPath("$.uri").value(PRODUCT_URI))
                .andExpect(jsonPath("$.fieldErrors.minPrice", containsInAnyOrder(invalidParameter("minPrice"))));

        verifyNoInteractions(productService);
    }

    @Test
    void getAllProducts_whenMaxPriceIsInvalid_returnsBadRequest() throws Exception {
        mockMvc.perform(get(PRODUCT_URI)
                        .param("page", "0")
                        .param("size", "10")
                        .param("status", ProductStatus.ACTIVE.name())
                        .param("keyword", "Test")
                        .param("minPrice", "0.0")
                        .param("maxPrice", "abc" ))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                .andExpect(jsonPath("$.uri").value(PRODUCT_URI))
                .andExpect(jsonPath("$.fieldErrors.maxPrice", containsInAnyOrder(invalidParameter("maxPrice"))));

        verifyNoInteractions(productService);
    }

    @Test
    void getAllProducts_whenNoProductsExist_returnsEmptyPage() throws Exception {
        List<ProductResponse> productResponses = List.of();
        Pageable pageable = PageRequest.of(0, 10);
        Page<ProductResponse> pageProductResponse = new PageImpl<>(productResponses, pageable, productResponses.size());

        when(productService.getAllProducts(any(ProductFilterRequest.class), any(Pageable.class))).thenReturn(pageProductResponse);

        mockMvc.perform(get(PRODUCT_URI)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.numberOfElements").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0));

        ArgumentCaptor<ProductFilterRequest> productFilterRequestCaptor = ArgumentCaptor.forClass(ProductFilterRequest.class);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(productService).getAllProducts(productFilterRequestCaptor.capture(), pageableCaptor.capture());

        ProductFilterRequest capturedProductFilter = productFilterRequestCaptor.getValue();
        Pageable capturedPageable = pageableCaptor.getValue();

        assertThat(capturedProductFilter.status()).isNull();
        assertThat(capturedProductFilter.keyword()).isNull();
        assertThat(capturedProductFilter.minPrice()).isNull();
        assertThat(capturedProductFilter.maxPrice()).isNull();
        assertThat(capturedPageable.getPageSize()).isEqualTo(10);
        assertThat(capturedPageable.getPageNumber()).isEqualTo(0);

        verifyNoMoreInteractions(productService);
    }

    @Test
    void putProduct_whenValidRequest_returnsProductResponse() throws Exception {
        Long productId = 1L;

        String updateName = VALID_PRODUCT_NAME + "update";
        String updateDescription = VALID_PRODUCT_DESCRIPTION + "update";
        int updateQuantity = VALID_PRODUCT_QUANTITY + 10;

        ProductResponse productResponse = new ProductResponse(
                productId,
                updateName,
                updateDescription,
                VALID_PRODUCT_PRICE,
                updateQuantity,
                ProductStatus.ACTIVE,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        ProductPutRequest productPutRequest = new ProductPutRequest(
                updateName,
                updateDescription,
                VALID_PRODUCT_PRICE,
                updateQuantity
        );

        when(productService.putProduct(productId, productPutRequest)).thenReturn(productResponse);

        mockMvc.perform(put(productUri(productId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productPutRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(productId))
                .andExpect(jsonPath("$.name").value(updateName))
                .andExpect(jsonPath("$.description").value(updateDescription))
                .andExpect(jsonPath("$.quantity").value(updateQuantity))
                .andExpect(jsonPath("$.status").value(ProductStatus.ACTIVE.name()))
                .andExpect(jsonPath("$.price").value(VALID_PRODUCT_PRICE.doubleValue()))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());

        ArgumentCaptor<ProductPutRequest> productPutRequestCaptor = ArgumentCaptor.forClass(ProductPutRequest.class);
        verify(productService).putProduct(eq(productId), productPutRequestCaptor.capture());

        ProductPutRequest capturedProductPutRequest = productPutRequestCaptor.getValue();

        assertThat(capturedProductPutRequest.name()).isEqualTo(productPutRequest.name());
        assertThat(capturedProductPutRequest.description()).isEqualTo(productPutRequest.description());
        assertThat(capturedProductPutRequest.price()).isEqualByComparingTo(productPutRequest.price());
        assertThat(capturedProductPutRequest.quantity()).isEqualTo(productPutRequest.quantity());

        verifyNoMoreInteractions(productService);
    }

    @Test
    void putProduct_whenProductIdIsInvalid_returnsBadRequest() throws Exception {
        String productId = INVALID_ID;

        ProductPutRequest productPutRequest = new ProductPutRequest(
                VALID_PRODUCT_NAME,
                VALID_PRODUCT_DESCRIPTION,
                VALID_PRODUCT_PRICE,
                VALID_PRODUCT_QUANTITY
        );

        mockMvc.perform(put(productUri(productId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productPutRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(invalidParameter("id")))
                .andExpect(jsonPath("$.uri").value(productUri(productId)));

        verifyNoInteractions(productService);
    }

    @Test
    void putProduct_whenProductNameIsBlank_returnsBadRequest() throws Exception {
        Long productId = 1L;

        ProductPutRequest productPutRequest = new ProductPutRequest(
                "",
                VALID_PRODUCT_DESCRIPTION,
                VALID_PRODUCT_PRICE,
                VALID_PRODUCT_QUANTITY
        );

        mockMvc.perform(put(productUri(productId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productPutRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                .andExpect(jsonPath("$.uri").value(productUri(productId)))
                .andExpect(jsonPath("$.fieldErrors.name", containsInAnyOrder(
                        PRODUCT_NAME_IS_REQUIRED,
                        PRODUCT_NAME_IS_INVALID)));

        verifyNoInteractions(productService);
    }

    @Test
    void putProduct_whenProductNameIsNull_returnsBadRequest() throws Exception {
        Long productId = 1L;

        ProductPutRequest productPutRequest = new ProductPutRequest(
                null,
                VALID_PRODUCT_DESCRIPTION,
                VALID_PRODUCT_PRICE,
                VALID_PRODUCT_QUANTITY
        );

        mockMvc.perform(put(productUri(productId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productPutRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                .andExpect(jsonPath("$.uri").value(productUri(productId)))
                .andExpect(jsonPath("$.fieldErrors.name", containsInAnyOrder(PRODUCT_NAME_IS_REQUIRED)));

        verifyNoInteractions(productService);
    }

    @Test
    void putProduct_whenProductNameIsMoreThan100Chars_returnsBadRequest() throws Exception {
        Long productId = 1L;

        ProductPutRequest productPutRequest = new ProductPutRequest(
                INVALID_PRODUCT_NAME_MORE_THAN_LIMIT,
                VALID_PRODUCT_DESCRIPTION,
                VALID_PRODUCT_PRICE,
                VALID_PRODUCT_QUANTITY
        );

        mockMvc.perform(put(productUri(productId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productPutRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                .andExpect(jsonPath("$.uri").value(productUri(productId)))
                .andExpect(jsonPath("$.fieldErrors.name", containsInAnyOrder(PRODUCT_NAME_IS_INVALID)));

        verifyNoInteractions(productService);
    }

    @Test
    void putProduct_whenDescriptionIsBlank_returnsBadRequest() throws Exception {
        Long productId = 1L;

        ProductPutRequest productPutRequest = new ProductPutRequest(
                VALID_PRODUCT_NAME,
                "",
                VALID_PRODUCT_PRICE,
                VALID_PRODUCT_QUANTITY
        );

        mockMvc.perform(put(productUri(productId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productPutRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                .andExpect(jsonPath("$.uri").value(productUri(productId)))
                .andExpect(jsonPath("$.fieldErrors.description", containsInAnyOrder(
                        PRODUCT_DESCRIPTION_IS_INVALID,
                        PRODUCT_DESCRIPTION_IS_REQUIRED)));

        verifyNoInteractions(productService);
    }

    @Test
    void putProduct_whenDescriptionIsNull_returnsBadRequest() throws Exception {
        Long productId = 1L;

        ProductPutRequest productPutRequest = new ProductPutRequest(
                VALID_PRODUCT_NAME,
                null,
                VALID_PRODUCT_PRICE,
                VALID_PRODUCT_QUANTITY
        );

        mockMvc.perform(put(productUri(productId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productPutRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                .andExpect(jsonPath("$.uri").value(productUri(productId)))
                .andExpect(jsonPath("$.fieldErrors.description", containsInAnyOrder(PRODUCT_DESCRIPTION_IS_REQUIRED)));

        verifyNoInteractions(productService);
    }

    @Test
    void putProduct_whenDescriptionLengthIsLessThan5_returnsBadRequest() throws Exception {
        Long productId = 1L;

        ProductPutRequest productPutRequest = new ProductPutRequest(
                VALID_PRODUCT_NAME,
                INVALID_PRODUCT_DESCRIPTION_LESS_THAN_LIMIT,
                VALID_PRODUCT_PRICE,
                VALID_PRODUCT_QUANTITY
        );

        mockMvc.perform(put(productUri(productId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productPutRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                .andExpect(jsonPath("$.uri").value(productUri(productId)))
                .andExpect(jsonPath("$.fieldErrors.description", containsInAnyOrder(PRODUCT_DESCRIPTION_IS_INVALID)));

        verifyNoInteractions(productService);
    }

    @Test
    void putProduct_whenDescriptionLengthIsMoreThan1000_returnsBadRequest() throws Exception {
        Long productId = 1L;

        ProductPutRequest productPutRequest = new ProductPutRequest(
                VALID_PRODUCT_NAME,
                INVALID_PRODUCT_DESCRIPTION_MORE_THAN_LIMIT,
                VALID_PRODUCT_PRICE,
                VALID_PRODUCT_QUANTITY
        );

        mockMvc.perform(put(productUri(productId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productPutRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                .andExpect(jsonPath("$.uri").value(productUri(productId)))
                .andExpect(jsonPath("$.fieldErrors.description", containsInAnyOrder(PRODUCT_DESCRIPTION_IS_INVALID)));

        verifyNoInteractions(productService);
    }

    @Test
    void putProduct_whenPriceIsNull_returnsBadRequest() throws Exception {
        Long productId = 1L;

        ProductPutRequest productPutRequest = new ProductPutRequest(
                VALID_PRODUCT_NAME,
                VALID_PRODUCT_DESCRIPTION,
                null,
                VALID_PRODUCT_QUANTITY
        );

        mockMvc.perform(put(productUri(productId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productPutRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                .andExpect(jsonPath("$.uri").value(productUri(productId)))
                .andExpect(jsonPath("$.fieldErrors.price", containsInAnyOrder(PRODUCT_PRICE_IS_REQUIRED)));

        verifyNoInteractions(productService);
    }

    @Test
    void putProduct_whenPriceIsZero_returnsBadRequest() throws Exception {
        Long productId = 1L;

        ProductPutRequest productPutRequest = new ProductPutRequest(
                VALID_PRODUCT_NAME,
                VALID_PRODUCT_DESCRIPTION,
                BigDecimal.ZERO,
                VALID_PRODUCT_QUANTITY
        );

        mockMvc.perform(put(productUri(productId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productPutRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                .andExpect(jsonPath("$.uri").value(productUri(productId)))
                .andExpect(jsonPath("$.fieldErrors.price", containsInAnyOrder(PRODUCT_PRICE_IS_INVALID)));

        verifyNoInteractions(productService);
    }

    @Test
    void putProduct_whenQuantityIsNull_returnsBadRequest() throws Exception {
        Long productId = 1L;

        ProductPutRequest productPutRequest = new ProductPutRequest(
                VALID_PRODUCT_NAME,
                VALID_PRODUCT_DESCRIPTION,
                VALID_PRODUCT_PRICE,
                null
        );

        mockMvc.perform(put(productUri(productId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productPutRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                .andExpect(jsonPath("$.uri").value(productUri(productId)))
                .andExpect(jsonPath("$.fieldErrors.quantity", containsInAnyOrder(PRODUCT_QUANTITY_IS_REQUIRED)));

        verifyNoInteractions(productService);
    }

    @Test
    void putProduct_whenQuantityIsNegative_returnsBadRequest() throws Exception {
        Long productId = 1L;

        ProductPutRequest productPutRequest = new ProductPutRequest(
                VALID_PRODUCT_NAME,
                VALID_PRODUCT_DESCRIPTION,
                VALID_PRODUCT_PRICE,
                -1
        );

        mockMvc.perform(put(productUri(productId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productPutRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                .andExpect(jsonPath("$.uri").value(productUri(productId)))
                .andExpect(jsonPath("$.fieldErrors.quantity", containsInAnyOrder(PRODUCT_QUANTITY_IS_INVALID)));

        verifyNoInteractions(productService);
    }

    @Test
    void putProduct_whenProductNotFound_returnsNotFound() throws Exception {
        Long productId = 1L;

        ProductPutRequest productPutRequest = new ProductPutRequest(
                VALID_PRODUCT_NAME,
                VALID_PRODUCT_DESCRIPTION,
                VALID_PRODUCT_PRICE,
                VALID_PRODUCT_QUANTITY
        );

        doThrow(new NoResourceFoundException(productNotFoundWithId(productId)))
                .when(productService).putProduct(productId, productPutRequest);

        mockMvc.perform(put(productUri(productId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productPutRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.NOT_FOUND.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(productNotFoundWithId(productId)))
                .andExpect(jsonPath("$.uri").value(productUri(productId)));

        verify(productService).putProduct(productId, productPutRequest);
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
                ProductStatus.ACTIVE,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        ProductPatchRequest productPatchRequest = new ProductPatchRequest(
                VALID_PRODUCT_NAME,
                VALID_PRODUCT_DESCRIPTION,
                VALID_PRODUCT_PRICE,
                VALID_PRODUCT_QUANTITY
        );

        when(productService.patchProduct(productId, productPatchRequest)).thenReturn(productResponse);

        mockMvc.perform(patch(productUri(productId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(productPatchRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(productId))
                .andExpect(jsonPath("$.name").value(productPatchRequest.name()))
                .andExpect(jsonPath("$.description").value(productPatchRequest.description()))
                .andExpect(jsonPath("$.price").value(productPatchRequest.price().doubleValue()))
                .andExpect(jsonPath("$.status").value(ProductStatus.ACTIVE.name()))
                .andExpect(jsonPath("$.quantity").value(productPatchRequest.quantity()))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());

        ArgumentCaptor<ProductPatchRequest> productPatchRequestCaptor = ArgumentCaptor.forClass(ProductPatchRequest.class);
        verify(productService).patchProduct(eq(productId), productPatchRequestCaptor.capture());

        ProductPatchRequest capturedProductPatchRequest = productPatchRequestCaptor.getValue();

        assertThat(capturedProductPatchRequest.name()).isEqualTo(productPatchRequest.name());
        assertThat(capturedProductPatchRequest.description()).isEqualTo(productPatchRequest.description());
        assertThat(capturedProductPatchRequest.price()).isEqualByComparingTo(productPatchRequest.price());
        assertThat(capturedProductPatchRequest.quantity()).isEqualTo(productPatchRequest.quantity());

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
                ProductStatus.ACTIVE,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        ProductPatchRequest productPatchRequest = new ProductPatchRequest(
                null,
                null,
                null,
                null
        );

        when(productService.patchProduct(productId, productPatchRequest)).thenReturn(productResponse);

        mockMvc.perform(patch(productUri(productId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productPatchRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(productId))
                .andExpect(jsonPath("$.name").value(VALID_PRODUCT_NAME))
                .andExpect(jsonPath("$.description").value(VALID_PRODUCT_DESCRIPTION))
                .andExpect(jsonPath("$.price").value(VALID_PRODUCT_PRICE.doubleValue()))
                .andExpect(jsonPath("$.status").value(ProductStatus.ACTIVE.name()))
                .andExpect(jsonPath("$.quantity").value(VALID_PRODUCT_QUANTITY))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());

        ArgumentCaptor<ProductPatchRequest> productPatchCaptor = ArgumentCaptor.forClass(ProductPatchRequest.class);
        verify(productService).patchProduct(eq(productId), productPatchCaptor.capture());

        ProductPatchRequest capturedPatchRequest = productPatchCaptor.getValue();

        assertThat(capturedPatchRequest.name()).isNull();
        assertThat(capturedPatchRequest.description()).isNull();
        assertThat(capturedPatchRequest.price()).isNull();
        assertThat(capturedPatchRequest.quantity()).isNull();

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
                ProductStatus.ACTIVE,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        ProductPatchRequest productPatchRequest = new ProductPatchRequest(
                VALID_PRODUCT_NAME,
                null,
                VALID_PRODUCT_PRICE,
                null
        );

        when(productService.patchProduct(productId, productPatchRequest)).thenReturn(productResponse);

        mockMvc.perform(patch(productUri(productId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productPatchRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(productId))
                .andExpect(jsonPath("$.name").value(VALID_PRODUCT_NAME))
                .andExpect(jsonPath("$.description").value(VALID_PRODUCT_DESCRIPTION))
                .andExpect(jsonPath("$.price").value(VALID_PRODUCT_PRICE.doubleValue()))
                .andExpect(jsonPath("$.status").value(ProductStatus.ACTIVE.name()))
                .andExpect(jsonPath("$.quantity").value(VALID_PRODUCT_QUANTITY))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());

        ArgumentCaptor<ProductPatchRequest> productPatchRequestCaptor = ArgumentCaptor.forClass(ProductPatchRequest.class);
        verify(productService).patchProduct(eq(productId), productPatchRequestCaptor.capture());

        ProductPatchRequest capturedPatchRequest = productPatchRequestCaptor.getValue();

        assertThat(capturedPatchRequest.name()).isEqualTo(VALID_PRODUCT_NAME);
        assertThat(capturedPatchRequest.description()).isNull();
        assertThat(capturedPatchRequest.price()).isEqualByComparingTo(VALID_PRODUCT_PRICE);
        assertThat(capturedPatchRequest.quantity()).isNull();

        verifyNoMoreInteractions(productService);
    }

    @Test
    void patchProduct_whenInvalidId_returnsBadRequest() throws Exception {
        String productId = INVALID_ID;

        ProductPatchRequest productPatchRequest = new ProductPatchRequest(
                VALID_PRODUCT_NAME,
                VALID_PRODUCT_DESCRIPTION,
                VALID_PRODUCT_PRICE,
                VALID_PRODUCT_QUANTITY
        );

        mockMvc.perform(patch(productUri(productId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productPatchRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(invalidParameter("id")))
                .andExpect(jsonPath("$.uri").value(productUri(productId)));

        verifyNoInteractions(productService);
    }

    @Test
    void patchProduct_whenProductNameIsBlank_returnsBadRequest() throws Exception {
        Long productId = 1L;

        ProductPatchRequest productPatchRequest = new ProductPatchRequest(
                "",
                VALID_PRODUCT_DESCRIPTION,
                VALID_PRODUCT_PRICE,
                VALID_PRODUCT_QUANTITY
        );

        mockMvc.perform(patch(productUri(productId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productPatchRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                .andExpect(jsonPath("$.uri").value(productUri(productId)))
                .andExpect(jsonPath("$.fieldErrors.name", containsInAnyOrder(
                        PRODUCT_NAME_IS_EMPTY,
                        PRODUCT_NAME_IS_INVALID
                )));

        verifyNoInteractions(productService);
    }

    @Test
    void patchProduct_whenProductNameIsMoreThan100Chars_returnsBadRequest() throws Exception {
        Long productId = 1L;

        ProductPatchRequest productPatchRequest = new ProductPatchRequest(
                INVALID_PRODUCT_NAME_MORE_THAN_LIMIT,
                VALID_PRODUCT_DESCRIPTION,
                VALID_PRODUCT_PRICE,
                VALID_PRODUCT_QUANTITY
        );

        mockMvc.perform(patch(productUri(productId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productPatchRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                .andExpect(jsonPath("$.uri").value(productUri(productId)))
                .andExpect(jsonPath("$.fieldErrors.name", containsInAnyOrder(
                        PRODUCT_NAME_IS_INVALID
                )));

        verifyNoInteractions(productService);
    }

    @Test
    void patchProduct_whenDescriptionIsEmpty_returnsBadRequest() throws Exception {
        Long productId = 1L;

        ProductPatchRequest productPatchRequest = new ProductPatchRequest(
                VALID_PRODUCT_NAME,
                "",
                VALID_PRODUCT_PRICE,
                VALID_PRODUCT_QUANTITY
        );

        mockMvc.perform(patch(productUri(productId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productPatchRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                .andExpect(jsonPath("$.uri").value(productUri(productId)))
                .andExpect(jsonPath("$.fieldErrors.description", containsInAnyOrder(
                        PRODUCT_DESCRIPTION_IS_INVALID,
                        PRODUCT_DESCRIPTION_IS_EMPTY
                )));

        verifyNoInteractions(productService);
    }

    @Test
    void patchProduct_whenDescriptionIsLessThan5Chars_returnsBadRequest() throws Exception {
        Long productId = 1L;

        ProductPatchRequest productPatchRequest = new ProductPatchRequest(
                VALID_PRODUCT_NAME,
                INVALID_PRODUCT_DESCRIPTION_LESS_THAN_LIMIT,
                VALID_PRODUCT_PRICE,
                VALID_PRODUCT_QUANTITY
        );

        mockMvc.perform(patch(productUri(productId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productPatchRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                .andExpect(jsonPath("$.uri").value(productUri(productId)))
                .andExpect(jsonPath("$.fieldErrors.description", containsInAnyOrder(
                        PRODUCT_DESCRIPTION_IS_INVALID
                )));

        verifyNoInteractions(productService);
    }

    @Test
    void patchProduct_whenDescriptionIsMoreThan1000Chars_returnsBadRequest() throws Exception {
        Long productId = 1L;

        ProductPatchRequest productPatchRequest = new ProductPatchRequest(
                VALID_PRODUCT_NAME,
                INVALID_PRODUCT_DESCRIPTION_MORE_THAN_LIMIT,
                VALID_PRODUCT_PRICE,
                VALID_PRODUCT_QUANTITY
        );

        mockMvc.perform(patch(productUri(productId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productPatchRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                .andExpect(jsonPath("$.uri").value(productUri(productId)))
                .andExpect(jsonPath("$.fieldErrors.description", containsInAnyOrder(
                        PRODUCT_DESCRIPTION_IS_INVALID
                )));

        verifyNoInteractions(productService);
    }

    @Test
    void patchProduct_whenPriceIsZero_returnsBadRequest() throws Exception {
        Long productId = 1L;

        ProductPatchRequest productPatchRequest = new ProductPatchRequest(
                VALID_PRODUCT_NAME,
                VALID_PRODUCT_DESCRIPTION,
                BigDecimal.ZERO,
                VALID_PRODUCT_QUANTITY
        );

        mockMvc.perform(patch(productUri(productId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productPatchRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                .andExpect(jsonPath("$.uri").value(productUri(productId)))
                .andExpect(jsonPath("$.fieldErrors.price", containsInAnyOrder(
                        PRODUCT_PRICE_IS_INVALID
                )));

        verifyNoInteractions(productService);
    }

    @Test
    void patchProduct_whenQuantityIsNegative_returnsBadRequest() throws Exception {
        Long productId = 1L;

        ProductPatchRequest productPatchRequest = new ProductPatchRequest(
                VALID_PRODUCT_NAME,
                VALID_PRODUCT_DESCRIPTION,
                VALID_PRODUCT_PRICE,
                INVALID_PRODUCT_NEGATIVE_QUANTITY
        );

        mockMvc.perform(patch(productUri(productId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productPatchRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                .andExpect(jsonPath("$.uri").value(productUri(productId)))
                .andExpect(jsonPath("$.fieldErrors.quantity", containsInAnyOrder(
                        PRODUCT_QUANTITY_IS_INVALID
                )));

        verifyNoInteractions(productService);
    }

    @Test
    void patchProduct_whenProductNotFound_returnsNotFound() throws Exception {
        Long productId = 1L;

        ProductPatchRequest productPatchRequest = new ProductPatchRequest(
                VALID_PRODUCT_NAME,
                VALID_PRODUCT_DESCRIPTION,
                VALID_PRODUCT_PRICE,
                VALID_PRODUCT_QUANTITY
        );

        doThrow(new NoResourceFoundException(productNotFoundWithId(productId)))
                .when(productService).patchProduct(productId, productPatchRequest);

        mockMvc.perform(patch(productUri(productId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productPatchRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.NOT_FOUND.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(productNotFoundWithId(productId)))
                .andExpect(jsonPath("$.uri").value(productUri(productId)));

        verify(productService).patchProduct(productId, productPatchRequest);
        verifyNoMoreInteractions(productService);
    }

    @Test
    void deleteProduct_whenProductIdIsValid_returnsNoContent() throws Exception {
        Long productId = 1L;

        mockMvc.perform(delete(productUri(productId)))
                .andExpect(status().isNoContent());

        verify(productService).deleteProduct(productId);
        verifyNoMoreInteractions(productService);
    }

    @Test
    void deleteProduct_whenProductIdIsInvalid_returnsBadRequest() throws Exception {
        String productId = INVALID_ID;

        mockMvc.perform(delete(productUri(productId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(invalidParameter("id")))
                .andExpect(jsonPath("$.uri").value(productUri(productId)));

        verifyNoInteractions(productService);
    }

    @Test
    void deleteProduct_whenProductIdIsNotFound_returnsNotFound() throws Exception {
        Long productId = 1L;

        doThrow(new NoResourceFoundException(productNotFoundWithId(productId)))
                .when(productService).deleteProduct(productId);

        mockMvc.perform(delete(productUri(productId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.NOT_FOUND.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(productNotFoundWithId(productId)))
                .andExpect(jsonPath("$.uri").value(productUri(productId)));

        verify(productService).deleteProduct(productId);
        verifyNoMoreInteractions(productService);
    }
}
