package com.namnguyen.ecommerce_platform.integration.product;

import com.namnguyen.ecommerce_platform.integration.BaseIntegrationTest;
import com.namnguyen.ecommerce_platform.product.dto.ProductCreateRequest;
import com.namnguyen.ecommerce_platform.product.dto.ProductPatchRequest;
import com.namnguyen.ecommerce_platform.product.dto.ProductPutRequest;
import com.namnguyen.ecommerce_platform.product.dto.ProductResponse;
import com.namnguyen.ecommerce_platform.product.entity.Product;
import com.namnguyen.ecommerce_platform.product.enums.ProductStatus;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static com.namnguyen.ecommerce_platform.testutil.TestDataFactory.*;
import static com.namnguyen.ecommerce_platform.testutil.messages.ProductTestMessages.*;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import static org.assertj.core.api.Assertions.assertThat;

public class ProductIntegrationTest extends BaseIntegrationTest {

    @Test
    void getProductById_whenProductExists_returnsProductFromDataBase() throws Exception {
        Product product = persistDefaultProduct();

        mockMvc.perform(get(productUri(product.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(product.getId()))
                .andExpect(jsonPath("$.name").value(product.getName()))
                .andExpect(jsonPath("$.description").value(product.getDescription()))
                .andExpect(jsonPath("$.price").value(product.getPrice().doubleValue()))
                .andExpect(jsonPath("$.quantity").value(product.getQuantity()))
                .andExpect(jsonPath("$.status").value(ProductStatus.ACTIVE.name()));
    }

    @Test
    void getProductById_whenProductNotExists_returnsNotFoundResponse() throws Exception {
        Long productId = 999L;

        mockMvc.perform(get(productUri(productId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.NOT_FOUND.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(productNotFoundWithId(productId)))
                .andExpect(jsonPath("$.uri").value(productUri(productId)));
    }

    @Test
    void getAllProducts_whenProductsExistsWithDefaultPagination_returnsListOfProductsFromDatabase() throws Exception {
        Product firstProduct = persistDefaultProduct();

        Product secondProduct = persistProduct(
                "second product",
                "second description",
                BigDecimal.valueOf(499.99),
                12,
                ProductStatus.ACTIVE);

        Product thirdProduct =
                persistProduct(
                        "third product",
                        "third description",
                        BigDecimal.valueOf(499.99),
                        11,
                        ProductStatus.ACTIVE);

        mockMvc.perform(get(PRODUCT_URI))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.content[0].id").value(firstProduct.getId()))
                .andExpect(jsonPath("$.content[0].name").value(firstProduct.getName()))
                .andExpect(jsonPath("$.content[0].description").value(firstProduct.getDescription()))
                .andExpect(jsonPath("$.content[0].price").value(firstProduct.getPrice().doubleValue()))
                .andExpect(jsonPath("$.content[0].quantity").value(firstProduct.getQuantity()))
                .andExpect(jsonPath("$.content[0].status").value(ProductStatus.ACTIVE.name()))
                .andExpect(jsonPath("$.content[0].createdAt").exists())
                .andExpect(jsonPath("$.content[0].updatedAt").exists())

                .andExpect(jsonPath("$.content[1].id").value(secondProduct.getId()))
                .andExpect(jsonPath("$.content[1].name").value(secondProduct.getName()))
                .andExpect(jsonPath("$.content[1].description").value(secondProduct.getDescription()))
                .andExpect(jsonPath("$.content[1].price").value(secondProduct.getPrice().doubleValue()))
                .andExpect(jsonPath("$.content[1].quantity").value(secondProduct.getQuantity()))
                .andExpect(jsonPath("$.content[1].status").value(ProductStatus.ACTIVE.name()))
                .andExpect(jsonPath("$.content[1].createdAt").exists())
                .andExpect(jsonPath("$.content[1].updatedAt").exists())

                .andExpect(jsonPath("$.content[2].id").value(thirdProduct.getId()))
                .andExpect(jsonPath("$.content[2].name").value(thirdProduct.getName()))
                .andExpect(jsonPath("$.content[2].description").value(thirdProduct.getDescription()))
                .andExpect(jsonPath("$.content[2].price").value(thirdProduct.getPrice().doubleValue()))
                .andExpect(jsonPath("$.content[2].quantity").value(thirdProduct.getQuantity()))
                .andExpect(jsonPath("$.content[2].status").value(ProductStatus.ACTIVE.name()))
                .andExpect(jsonPath("$.content[2].createdAt").exists())
                .andExpect(jsonPath("$.content[2].updatedAt").exists())

                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.numberOfElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void getAllProducts_whenProductsExistsWithCustomPaginationGetSecondPage_returnsListOfProductsFromDatabase() throws Exception {
        persistDefaultProduct();

        persistProduct(
                "second product",
                "second description",
                VALID_PRODUCT_PRICE,
                12,
                ProductStatus.ACTIVE);

        Product thirdProduct =
                persistProduct(
                        "third product",
                        "third description",
                        VALID_PRODUCT_PRICE,
                        11,
                        ProductStatus.ACTIVE);

        mockMvc.perform(get(PRODUCT_URI)
                        .param("page", "1")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(thirdProduct.getId()))
                .andExpect(jsonPath("$.content[0].name").value(thirdProduct.getName()))
                .andExpect(jsonPath("$.content[0].description").value(thirdProduct.getDescription()))
                .andExpect(jsonPath("$.content[0].price").value(thirdProduct.getPrice().doubleValue()))
                .andExpect(jsonPath("$.content[0].quantity").value(thirdProduct.getQuantity()))
                .andExpect(jsonPath("$.content[0].status").value(ProductStatus.ACTIVE.name()))
                .andExpect(jsonPath("$.content[0].createdAt").exists())
                .andExpect(jsonPath("$.content[0].updatedAt").exists())

                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.numberOfElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void getAllProducts_whenStatusFilterProvided_returnsOnlyThatStatus() throws Exception {
        Product firstProduct = persistDefaultProduct();

        persistProduct(
                "second product",
                "second description",
                VALID_PRODUCT_PRICE,
                0,
                ProductStatus.OUT_OF_STOCK);

        Product thirdProduct =
                persistProduct(
                        "third product",
                        "third description",
                        VALID_PRODUCT_PRICE,
                        11,
                        ProductStatus.ACTIVE);

        mockMvc.perform(get(PRODUCT_URI)
                        .param("status", ProductStatus.ACTIVE.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].id").value(firstProduct.getId()))
                .andExpect(jsonPath("$.content[0].name").value(firstProduct.getName()))
                .andExpect(jsonPath("$.content[0].description").value(firstProduct.getDescription()))
                .andExpect(jsonPath("$.content[0].price").value(firstProduct.getPrice().doubleValue()))
                .andExpect(jsonPath("$.content[0].quantity").value(firstProduct.getQuantity()))
                .andExpect(jsonPath("$.content[0].status").value(ProductStatus.ACTIVE.name()))
                .andExpect(jsonPath("$.content[0].createdAt").exists())
                .andExpect(jsonPath("$.content[0].updatedAt").exists())

                .andExpect(jsonPath("$.content[1].id").value(thirdProduct.getId()))
                .andExpect(jsonPath("$.content[1].name").value(thirdProduct.getName()))
                .andExpect(jsonPath("$.content[1].description").value(thirdProduct.getDescription()))
                .andExpect(jsonPath("$.content[1].price").value(thirdProduct.getPrice().doubleValue()))
                .andExpect(jsonPath("$.content[1].quantity").value(thirdProduct.getQuantity()))
                .andExpect(jsonPath("$.content[1].status").value(ProductStatus.ACTIVE.name()))
                .andExpect(jsonPath("$.content[1].createdAt").exists())
                .andExpect(jsonPath("$.content[1].updatedAt").exists())

                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.numberOfElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void getAllProducts_whenKeywordMatchesName_returnsMatchingProducts() throws Exception {
        persistDefaultProduct();

        Product secondProduct = persistProduct(
                "second product",
                "second description",
                VALID_PRODUCT_PRICE,
                0,
                ProductStatus.OUT_OF_STOCK);

        persistProduct(
                "third product",
                "third description",
                VALID_PRODUCT_PRICE,
                11,
                ProductStatus.ACTIVE);

        mockMvc.perform(get(PRODUCT_URI)
                        .param("keyword", "second"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(secondProduct.getId()))
                .andExpect(jsonPath("$.content[0].name").value(secondProduct.getName()))
                .andExpect(jsonPath("$.content[0].description").value(secondProduct.getDescription()))
                .andExpect(jsonPath("$.content[0].price").value(secondProduct.getPrice().doubleValue()))
                .andExpect(jsonPath("$.content[0].quantity").value(secondProduct.getQuantity()))
                .andExpect(jsonPath("$.content[0].status").value(ProductStatus.OUT_OF_STOCK.name()))
                .andExpect(jsonPath("$.content[0].createdAt").exists())
                .andExpect(jsonPath("$.content[0].updatedAt").exists())

                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.numberOfElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void getAllProducts_whenKeywordMatchesDescription_returnsMatchingProducts() throws Exception {
        persistDefaultProduct();

        persistProduct(
                "second product",
                "second description",
                VALID_PRODUCT_PRICE,
                0,
                ProductStatus.OUT_OF_STOCK);

        Product thirdProduct =
                persistProduct(
                        "console",
                        "XBox gaming console",
                        VALID_PRODUCT_PRICE,
                        11,
                        ProductStatus.ACTIVE);

        mockMvc.perform(get(PRODUCT_URI)
                        .param("keyword", "XBox gaming console"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(thirdProduct.getId()))
                .andExpect(jsonPath("$.content[0].name").value(thirdProduct.getName()))
                .andExpect(jsonPath("$.content[0].description").value(thirdProduct.getDescription()))
                .andExpect(jsonPath("$.content[0].price").value(thirdProduct.getPrice().doubleValue()))
                .andExpect(jsonPath("$.content[0].quantity").value(thirdProduct.getQuantity()))
                .andExpect(jsonPath("$.content[0].status").value(ProductStatus.ACTIVE.name()))
                .andExpect(jsonPath("$.content[0].createdAt").exists())
                .andExpect(jsonPath("$.content[0].updatedAt").exists())

                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.numberOfElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void getAllProducts_whenPriceRangeProvided_returnsProductsInsideRange() throws Exception {
        persistDefaultProduct();

        Product secondProduct = persistProduct(
                VALID_PRODUCT_NAME,
                VALID_PRODUCT_DESCRIPTION,
                BigDecimal.valueOf(399.99),
                3,
                ProductStatus.ACTIVE);

        Product thirdProduct =
                persistProduct(
                        VALID_PRODUCT_NAME,
                        VALID_PRODUCT_DESCRIPTION,
                        BigDecimal.valueOf(499.99),
                        11,
                        ProductStatus.ACTIVE);

        mockMvc.perform(get(PRODUCT_URI)
                        .param("minPrice", String.valueOf(BigDecimal.valueOf(300.00)))
                        .param("maxPrice", String.valueOf(BigDecimal.valueOf(500.00))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].id").value(secondProduct.getId()))
                .andExpect(jsonPath("$.content[0].name").value(secondProduct.getName()))
                .andExpect(jsonPath("$.content[0].description").value(secondProduct.getDescription()))
                .andExpect(jsonPath("$.content[0].price").value(secondProduct.getPrice().doubleValue()))
                .andExpect(jsonPath("$.content[0].quantity").value(secondProduct.getQuantity()))
                .andExpect(jsonPath("$.content[0].status").value(ProductStatus.ACTIVE.name()))
                .andExpect(jsonPath("$.content[0].createdAt").exists())
                .andExpect(jsonPath("$.content[0].updatedAt").exists())

                .andExpect(jsonPath("$.content[1].id").value(thirdProduct.getId()))
                .andExpect(jsonPath("$.content[1].name").value(thirdProduct.getName()))
                .andExpect(jsonPath("$.content[1].description").value(thirdProduct.getDescription()))
                .andExpect(jsonPath("$.content[1].price").value(thirdProduct.getPrice().doubleValue()))
                .andExpect(jsonPath("$.content[1].quantity").value(thirdProduct.getQuantity()))
                .andExpect(jsonPath("$.content[1].status").value(ProductStatus.ACTIVE.name()))
                .andExpect(jsonPath("$.content[1].createdAt").exists())
                .andExpect(jsonPath("$.content[1].updatedAt").exists())

                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.numberOfElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void getAllProducts_whenProductsExistsWithSortDesc_returnsListOfProductsFromDatabase() throws Exception {
        Product firstProduct = persistDefaultProduct();

        Product secondProduct = persistProduct(
                "second product",
                "second description",
                VALID_PRODUCT_PRICE,
                12,
                ProductStatus.ACTIVE);

        Product thirdProduct =
                persistProduct(
                        "third product",
                        "third description",
                        VALID_PRODUCT_PRICE,
                        11,
                        ProductStatus.ACTIVE);

        mockMvc.perform(get(PRODUCT_URI)
                        .param("sort", "id,DESC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.content[2].id").value(firstProduct.getId()))
                .andExpect(jsonPath("$.content[2].name").value(firstProduct.getName()))
                .andExpect(jsonPath("$.content[2].description").value(firstProduct.getDescription()))
                .andExpect(jsonPath("$.content[2].price").value(firstProduct.getPrice().doubleValue()))
                .andExpect(jsonPath("$.content[2].quantity").value(firstProduct.getQuantity()))
                .andExpect(jsonPath("$.content[2].status").value(ProductStatus.ACTIVE.name()))
                .andExpect(jsonPath("$.content[2].createdAt").exists())
                .andExpect(jsonPath("$.content[2].updatedAt").exists())

                .andExpect(jsonPath("$.content[1].id").value(secondProduct.getId()))
                .andExpect(jsonPath("$.content[1].name").value(secondProduct.getName()))
                .andExpect(jsonPath("$.content[1].description").value(secondProduct.getDescription()))
                .andExpect(jsonPath("$.content[1].price").value(secondProduct.getPrice().doubleValue()))
                .andExpect(jsonPath("$.content[1].quantity").value(secondProduct.getQuantity()))
                .andExpect(jsonPath("$.content[1].status").value(ProductStatus.ACTIVE.name()))
                .andExpect(jsonPath("$.content[1].createdAt").exists())
                .andExpect(jsonPath("$.content[1].updatedAt").exists())

                .andExpect(jsonPath("$.content[0].id").value(thirdProduct.getId()))
                .andExpect(jsonPath("$.content[0].name").value(thirdProduct.getName()))
                .andExpect(jsonPath("$.content[0].description").value(thirdProduct.getDescription()))
                .andExpect(jsonPath("$.content[0].price").value(thirdProduct.getPrice().doubleValue()))
                .andExpect(jsonPath("$.content[0].quantity").value(thirdProduct.getQuantity()))
                .andExpect(jsonPath("$.content[0].status").value(ProductStatus.ACTIVE.name()))
                .andExpect(jsonPath("$.content[0].createdAt").exists())
                .andExpect(jsonPath("$.content[0].updatedAt").exists())

                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.numberOfElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void getAllProducts_whenMultipleFiltersProvided_returnsListOfProductsFromDatabase() throws Exception {
        persistDefaultProduct();

        Product secondProduct = persistProduct(
                "second product",
                "second description",
                BigDecimal.valueOf(399.99),
                12,
                ProductStatus.ACTIVE);

        persistProduct(
                "third product",
                "third description",
                        BigDecimal.valueOf(499.99),
                        11,
                        ProductStatus.ACTIVE);

        mockMvc.perform(get(PRODUCT_URI)
                        .param("sort", "id,DESC")
                        .param("keyword", "second")
                        .param("minPrice", String.valueOf(BigDecimal.valueOf(300.00)))
                        .param("maxPrice", String.valueOf(BigDecimal.valueOf(400.00)))
                        .param("status", ProductStatus.ACTIVE.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(secondProduct.getId()))
                .andExpect(jsonPath("$.content[0].name").value(secondProduct.getName()))
                .andExpect(jsonPath("$.content[0].description").value(secondProduct.getDescription()))
                .andExpect(jsonPath("$.content[0].price").value(secondProduct.getPrice().doubleValue()))
                .andExpect(jsonPath("$.content[0].quantity").value(secondProduct.getQuantity()))
                .andExpect(jsonPath("$.content[0].status").value(ProductStatus.ACTIVE.name()))
                .andExpect(jsonPath("$.content[0].createdAt").exists())
                .andExpect(jsonPath("$.content[0].updatedAt").exists())

                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.numberOfElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void createProduct_whenCreateRequestIsValid_saveProductToDatabase() throws Exception {

        ProductCreateRequest productCreateRequest = createDefaultProductCreateRequest();

        MvcResult result = mockMvc.perform(post(PRODUCT_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(productCreateRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value(VALID_PRODUCT_NAME))
                .andExpect(jsonPath("$.description").value(VALID_PRODUCT_DESCRIPTION))
                .andExpect(jsonPath("$.price").value(VALID_PRODUCT_PRICE.doubleValue()))
                .andExpect(jsonPath("$.quantity").value(VALID_PRODUCT_QUANTITY))
                .andExpect(jsonPath("$.status").value(ProductStatus.ACTIVE.name()))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();

        ProductResponse productResponse = objectMapper.readValue(responseBody, ProductResponse.class);

        Product savedProduct = productRepository.findById(productResponse.id()).orElseThrow();

        assertThat(savedProduct.getName()).isEqualTo(VALID_PRODUCT_NAME);
        assertThat(savedProduct.getDescription()).isEqualTo(VALID_PRODUCT_DESCRIPTION);
        assertThat(savedProduct.getPrice()).isEqualByComparingTo(VALID_PRODUCT_PRICE);
        assertThat(savedProduct.getQuantity()).isEqualTo(VALID_PRODUCT_QUANTITY);
        assertThat(savedProduct.getStatus()).isEqualTo(ProductStatus.ACTIVE);
        assertThat(savedProduct.getId()).isEqualTo(productResponse.id());
        assertThat(productRepository.count()).isEqualTo(1);
    }

    @Test
    void createProduct_whenQuantityIsZero_savesProductAsOutOfStock() throws Exception {
        ProductCreateRequest request = createProductCreateRequest(
                VALID_PRODUCT_NAME,
                VALID_PRODUCT_DESCRIPTION,
                VALID_PRODUCT_PRICE,
                0
        );

        MvcResult result = mockMvc.perform(post(PRODUCT_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.quantity").value(0))
                .andExpect(jsonPath("$.status").value(ProductStatus.OUT_OF_STOCK.name()))
                .andReturn();

        ProductResponse productResponse = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                ProductResponse.class
        );

        Product savedProduct =
                productRepository.findById(productResponse.id()).orElseThrow();

        assertThat(savedProduct.getQuantity()).isZero();
        assertThat(savedProduct.getStatus())
                .isEqualTo(ProductStatus.OUT_OF_STOCK);
    }

    @Test
    void putProduct_whenPutRequestIsValid_saveProductToDatabase() throws Exception {
        Product product = persistDefaultProduct();

        ProductPutRequest request = createDefaultProductPutRequest();

        MvcResult result = mockMvc.perform(put(productUri(product.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value(VALID_UPDATE_PRODUCT_NAME))
                .andExpect(jsonPath("$.description").value(VALID_UPDATE_PRODUCT_DESCRIPTION))
                .andExpect(jsonPath("$.price").value(VALID_UPDATE_PRODUCT_PRICE.doubleValue()))
                .andExpect(jsonPath("$.quantity").value(VALID_UPDATE_PRODUCT_QUANTITY))
                .andExpect(jsonPath("$.status").value(ProductStatus.ACTIVE.name()))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();

        ProductResponse productResponse = objectMapper.readValue(responseBody, ProductResponse.class);

        Product savedUpdateProduct = productRepository.findById(productResponse.id()).orElseThrow();

        assertThat(savedUpdateProduct.getName()).isEqualTo(VALID_UPDATE_PRODUCT_NAME);
        assertThat(savedUpdateProduct.getDescription()).isEqualTo(VALID_UPDATE_PRODUCT_DESCRIPTION);
        assertThat(savedUpdateProduct.getPrice()).isEqualByComparingTo(VALID_UPDATE_PRODUCT_PRICE);
        assertThat(savedUpdateProduct.getQuantity()).isEqualTo(VALID_UPDATE_PRODUCT_QUANTITY);
        assertThat(savedUpdateProduct.getStatus()).isEqualTo(ProductStatus.ACTIVE);
        assertThat(savedUpdateProduct.getId()).isEqualTo(productResponse.id());

        assertThat(productResponse.id()).isEqualTo(product.getId());
        assertThat(productRepository.count()).isEqualTo(1);
    }

    @Test
    void putProduct_whenQuantityIsZero_savesProductAsOutOfStock() throws Exception {
        Product product = persistProduct(
                VALID_PRODUCT_NAME,
                VALID_PRODUCT_DESCRIPTION,
                VALID_PRODUCT_PRICE,
                VALID_PRODUCT_QUANTITY,
                ProductStatus.ACTIVE);

        ProductPutRequest request = createProductPutRequest(
                VALID_UPDATE_PRODUCT_NAME,
                VALID_UPDATE_PRODUCT_DESCRIPTION,
                VALID_PRODUCT_PRICE,
                0
        );

        MvcResult result = mockMvc.perform(put(productUri(product.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(0))
                .andExpect(jsonPath("$.status").value(ProductStatus.OUT_OF_STOCK.name()))
                .andReturn();

        ProductResponse productResponse = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                ProductResponse.class
        );

        Product savedProduct =
                productRepository.findById(productResponse.id()).orElseThrow();

        assertThat(savedProduct.getQuantity()).isZero();
        assertThat(savedProduct.getStatus())
                .isEqualTo(ProductStatus.OUT_OF_STOCK);
    }

    @Test
    void putProduct_whenProductNotFound_returnsNotFound() throws Exception {
        Long productId = 999_999L;

        ProductPutRequest request = createDefaultProductPutRequest();

        mockMvc.perform(put(productUri(productId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());

        assertThat(productRepository.count()).isZero();
    }

    @Test
    void patchProduct_whenPartiallyPatch_saveProductToDatabase() throws Exception {
        Product product = persistDefaultProduct();

        ProductPatchRequest request = createProductPatchRequest(
                VALID_UPDATE_PRODUCT_NAME,
                null,
                VALID_UPDATE_PRODUCT_PRICE,
                null
        );

        String originalProductDescription = product.getDescription();
        int originalProductQuantity = product.getQuantity();

        MvcResult result = mockMvc.perform(patch(productUri(product.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value(VALID_UPDATE_PRODUCT_NAME))
                .andExpect(jsonPath("$.description").value(originalProductDescription))
                .andExpect(jsonPath("$.price").value(VALID_UPDATE_PRODUCT_PRICE.doubleValue()))
                .andExpect(jsonPath("$.quantity").value(originalProductQuantity))
                .andExpect(jsonPath("$.status").value(ProductStatus.ACTIVE.name()))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();

        ProductResponse productResponse = objectMapper.readValue(responseBody, ProductResponse.class);

        Product savedUpdateProduct = productRepository.findById(productResponse.id()).orElseThrow();

        assertThat(savedUpdateProduct.getName()).isEqualTo(VALID_UPDATE_PRODUCT_NAME);
        assertThat(savedUpdateProduct.getDescription()).isEqualTo(originalProductDescription);
        assertThat(savedUpdateProduct.getPrice()).isEqualByComparingTo(VALID_UPDATE_PRODUCT_PRICE);
        assertThat(savedUpdateProduct.getQuantity()).isEqualTo(originalProductQuantity);
        assertThat(savedUpdateProduct.getStatus()).isEqualTo(ProductStatus.ACTIVE);
        assertThat(savedUpdateProduct.getId()).isEqualTo(productResponse.id());

        assertThat(productResponse.id()).isEqualTo(product.getId());
        assertThat(productRepository.count()).isEqualTo(1);
    }

    @Test
    void patchProduct_whenQuantityIsZero_savesProductAsOutOfStock() throws Exception {
        Product product = persistDefaultProduct();

        ProductPatchRequest productPatchRequest = createProductPatchRequest(
                VALID_UPDATE_PRODUCT_NAME,
                VALID_UPDATE_PRODUCT_DESCRIPTION,
                VALID_UPDATE_PRODUCT_PRICE,
                0
        );

        MvcResult result = mockMvc.perform(patch(productUri(product.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productPatchRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(0))
                .andExpect(jsonPath("$.status").value(ProductStatus.OUT_OF_STOCK.name()))
                .andReturn();

        ProductResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                ProductResponse.class
        );

        Product savedProduct =
                productRepository.findById(response.id()).orElseThrow();

        assertThat(savedProduct.getQuantity()).isZero();
        assertThat(savedProduct.getStatus())
                .isEqualTo(ProductStatus.OUT_OF_STOCK);
    }

    @Test
    void patchProduct_whenProductNotFound_returnsNotFound() throws Exception {
        Long productId = 999_999L;

        ProductPatchRequest request = createDefaultProductPatchRequest();

        mockMvc.perform(patch(productUri(productId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());

        assertThat(productRepository.count()).isZero();
    }

    @Test
    void deleteProduct_whenProductFound_productDeletedFromDatabase() throws Exception {
        Product savedProduct = persistDefaultProduct();

        mockMvc.perform(delete(productUri(savedProduct.getId())))
                .andExpect(status().isNoContent());

        assertThat(productRepository.existsById(savedProduct.getId())).isFalse();
    }

    @Test
    void deleteProduct_whenProductNotFound_returnsNotFound() throws Exception {
        Long productId = 999_999L;

        mockMvc.perform(delete(productUri(productId)))
                .andExpect(status().isNotFound());

        assertThat(productRepository.count()).isZero();
    }

}
