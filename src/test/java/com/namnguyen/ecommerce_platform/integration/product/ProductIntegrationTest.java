package com.namnguyen.ecommerce_platform.integration.product;

import com.namnguyen.ecommerce_platform.integration.BaseIntegrationTest;
import com.namnguyen.ecommerce_platform.product.dto.ProductCreateRequest;
import com.namnguyen.ecommerce_platform.product.dto.ProductPatchRequest;
import com.namnguyen.ecommerce_platform.product.dto.ProductPutRequest;
import com.namnguyen.ecommerce_platform.product.dto.ProductResponse;
import com.namnguyen.ecommerce_platform.product.entity.Product;
import com.namnguyen.ecommerce_platform.product.enums.ProductStatus;
import com.namnguyen.ecommerce_platform.product.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static com.namnguyen.ecommerce_platform.testutil.TestDataFactory.*;
import static com.namnguyen.ecommerce_platform.testutil.TestMessages.*;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import static org.assertj.core.api.Assertions.assertThat;

public class ProductIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private ProductRepository productRepository;

    @Test
    void getProductById_whenProductExists_returnsProductFromDataBase() throws Exception {
        Product product = Product.builder()
                .name("Keyboard")
                .description("Mechanical keyboard")
                .price(BigDecimal.valueOf(99.99))
                .quantity(10)
                .status(ProductStatus.ACTIVE)
                .build();

        Product savedProduct = productRepository.save(product);

        mockMvc.perform(get(PRODUCT_URI + "/" + savedProduct.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedProduct.getId()))
                .andExpect(jsonPath("$.name").value("Keyboard"))
                .andExpect(jsonPath("$.description").value("Mechanical keyboard"))
                .andExpect(jsonPath("$.price").value(BigDecimal.valueOf(99.99).doubleValue()))
                .andExpect(jsonPath("$.quantity").value(10))
                .andExpect(jsonPath("$.status").value(ProductStatus.ACTIVE.name()));
    }

    @Test
    void getProductById_whenProductNotExists_returnsNotFoundResponse() throws Exception {
        Long productId = 999L;

        mockMvc.perform(get(PRODUCT_URI + "/" + productId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.NOT_FOUND.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(productNotFound(productId)))
                .andExpect(jsonPath("$.uri").value(PRODUCT_URI + "/" + productId));
    }

    @Test
    void getAllProducts_whenProductsExistsWithDefaultPagination_returnsListOfProductsFromDatabase() throws Exception {
        Product product = Product.builder()
                .name("Keyboard")
                .description("Mechanical keyboard")
                .price(BigDecimal.valueOf(99.99))
                .quantity(10)
                .status(ProductStatus.ACTIVE)
                .build();

        Product product1 = Product.builder()
                .name("PS5")
                .description("Playstation")
                .price(BigDecimal.valueOf(499.99))
                .quantity(12)
                .status(ProductStatus.ACTIVE)
                .build();

        Product product2 = Product.builder()
                .name("XBOX")
                .description("XBox")
                .price(BigDecimal.valueOf(499.99))
                .quantity(11)
                .status(ProductStatus.ACTIVE)
                .build();

        Product savedProduct = productRepository.save(product);
        Product savedProduct1 = productRepository.save(product1);
        Product savedProduct2 = productRepository.save(product2);

        mockMvc.perform(get(PRODUCT_URI))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.content[0].id").value(savedProduct.getId()))
                .andExpect(jsonPath("$.content[0].name").value("Keyboard"))
                .andExpect(jsonPath("$.content[0].description").value("Mechanical keyboard"))
                .andExpect(jsonPath("$.content[0].price").value(BigDecimal.valueOf(99.99).doubleValue()))
                .andExpect(jsonPath("$.content[0].quantity").value(10))
                .andExpect(jsonPath("$.content[0].status").value(ProductStatus.ACTIVE.name()))
                .andExpect(jsonPath("$.content[0].createdAt").exists())
                .andExpect(jsonPath("$.content[0].updatedAt").exists())

                .andExpect(jsonPath("$.content[1].id").value(savedProduct1.getId()))
                .andExpect(jsonPath("$.content[1].name").value("PS5"))
                .andExpect(jsonPath("$.content[1].description").value("Playstation"))
                .andExpect(jsonPath("$.content[1].price").value(BigDecimal.valueOf(499.99).doubleValue()))
                .andExpect(jsonPath("$.content[1].quantity").value(12))
                .andExpect(jsonPath("$.content[1].status").value(ProductStatus.ACTIVE.name()))
                .andExpect(jsonPath("$.content[1].createdAt").exists())
                .andExpect(jsonPath("$.content[1].updatedAt").exists())

                .andExpect(jsonPath("$.content[2].id").value(savedProduct2.getId()))
                .andExpect(jsonPath("$.content[2].name").value("XBOX"))
                .andExpect(jsonPath("$.content[2].description").value("XBox"))
                .andExpect(jsonPath("$.content[2].price").value(BigDecimal.valueOf(499.99).doubleValue()))
                .andExpect(jsonPath("$.content[2].quantity").value(11))
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
        Product product = Product.builder()
                .name("Keyboard")
                .description("Mechanical keyboard")
                .price(BigDecimal.valueOf(99.99))
                .quantity(10)
                .status(ProductStatus.ACTIVE)
                .build();

        Product product1 = Product.builder()
                .name("PS5")
                .description("Playstation")
                .price(BigDecimal.valueOf(499.99))
                .quantity(12)
                .status(ProductStatus.ACTIVE)
                .build();

        Product product2 = Product.builder()
                .name("XBOX")
                .description("XBox")
                .price(BigDecimal.valueOf(499.99))
                .quantity(11)
                .status(ProductStatus.ACTIVE)
                .build();

        Product savedProduct = productRepository.save(product);
        Product savedProduct1 = productRepository.save(product1);
        Product savedProduct2 = productRepository.save(product2);

        mockMvc.perform(get(PRODUCT_URI)
                        .param("page", "1")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(savedProduct2.getId()))
                .andExpect(jsonPath("$.content[0].name").value("XBOX"))
                .andExpect(jsonPath("$.content[0].description").value("XBox"))
                .andExpect(jsonPath("$.content[0].price").value(BigDecimal.valueOf(499.99).doubleValue()))
                .andExpect(jsonPath("$.content[0].quantity").value(11))
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
        Product product = Product.builder()
                .name("Keyboard")
                .description("Mechanical keyboard")
                .price(BigDecimal.valueOf(99.99))
                .quantity(10)
                .status(ProductStatus.ACTIVE)
                .build();

        Product product1 = Product.builder()
                .name("PS5")
                .description("Playstation")
                .price(BigDecimal.valueOf(499.99))
                .quantity(0)
                .status(ProductStatus.INACTIVE)
                .build();

        Product product2 = Product.builder()
                .name("XBOX")
                .description("XBox")
                .price(BigDecimal.valueOf(499.99))
                .quantity(11)
                .status(ProductStatus.ACTIVE)
                .build();

        Product savedProduct = productRepository.save(product);
        Product savedProduct1 = productRepository.save(product1);
        Product savedProduct2 = productRepository.save(product2);

        mockMvc.perform(get(PRODUCT_URI)
                        .param("status", ProductStatus.ACTIVE.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].id").value(savedProduct.getId()))
                .andExpect(jsonPath("$.content[0].name").value("Keyboard"))
                .andExpect(jsonPath("$.content[0].description").value("Mechanical keyboard"))
                .andExpect(jsonPath("$.content[0].price").value(BigDecimal.valueOf(99.99).doubleValue()))
                .andExpect(jsonPath("$.content[0].quantity").value(10))
                .andExpect(jsonPath("$.content[0].status").value(ProductStatus.ACTIVE.name()))
                .andExpect(jsonPath("$.content[0].createdAt").exists())
                .andExpect(jsonPath("$.content[0].updatedAt").exists())

                .andExpect(jsonPath("$.content[1].id").value(savedProduct2.getId()))
                .andExpect(jsonPath("$.content[1].name").value("XBOX"))
                .andExpect(jsonPath("$.content[1].description").value("XBox"))
                .andExpect(jsonPath("$.content[1].price").value(BigDecimal.valueOf(499.99).doubleValue()))
                .andExpect(jsonPath("$.content[1].quantity").value(11))
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
        Product product = Product.builder()
                .name("Keyboard")
                .description("Mechanical keyboard")
                .price(BigDecimal.valueOf(99.99))
                .quantity(10)
                .status(ProductStatus.ACTIVE)
                .build();

        Product product1 = Product.builder()
                .name("PS5")
                .description("Playstation")
                .price(BigDecimal.valueOf(499.99))
                .quantity(0)
                .status(ProductStatus.INACTIVE)
                .build();

        Product product2 = Product.builder()
                .name("XBOX")
                .description("XBox")
                .price(BigDecimal.valueOf(499.99))
                .quantity(11)
                .status(ProductStatus.ACTIVE)
                .build();

        productRepository.save(product);
        productRepository.save(product2);
        Product savedProduct1 = productRepository.save(product1);

        mockMvc.perform(get(PRODUCT_URI)
                        .param("keyword", "PS5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(savedProduct1.getId()))
                .andExpect(jsonPath("$.content[0].name").value("PS5"))
                .andExpect(jsonPath("$.content[0].description").value("Playstation"))
                .andExpect(jsonPath("$.content[0].price").value(BigDecimal.valueOf(499.99).doubleValue()))
                .andExpect(jsonPath("$.content[0].quantity").value(0))
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
        Product product = Product.builder()
                .name("Keyboard")
                .description("Mechanical keyboard")
                .price(BigDecimal.valueOf(99.99))
                .quantity(10)
                .status(ProductStatus.ACTIVE)
                .build();

        Product product1 = Product.builder()
                .name("PS5")
                .description("Playstation")
                .price(BigDecimal.valueOf(499.99))
                .quantity(0)
                .status(ProductStatus.INACTIVE)
                .build();

        Product product2 = Product.builder()
                .name("console")
                .description("XBox gaming console")
                .price(BigDecimal.valueOf(499.99))
                .quantity(11)
                .status(ProductStatus.ACTIVE)
                .build();

        productRepository.save(product);
        productRepository.save(product1);
        Product savedProduct2 = productRepository.save(product2);

        mockMvc.perform(get(PRODUCT_URI)
                        .param("keyword", "xBox"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(savedProduct2.getId()))
                .andExpect(jsonPath("$.content[0].name").value("console"))
                .andExpect(jsonPath("$.content[0].description").value("XBox gaming console"))
                .andExpect(jsonPath("$.content[0].price").value(BigDecimal.valueOf(499.99).doubleValue()))
                .andExpect(jsonPath("$.content[0].quantity").value(11))
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
        Product product = Product.builder()
                .name("Keyboard")
                .description("Mechanical keyboard")
                .price(BigDecimal.valueOf(99.99))
                .quantity(10)
                .status(ProductStatus.ACTIVE)
                .build();

        Product product1 = Product.builder()
                .name("PS5")
                .description("Playstation")
                .price(BigDecimal.valueOf(399.99))
                .quantity(3)
                .status(ProductStatus.ACTIVE)
                .build();

        Product product2 = Product.builder()
                .name("XBOX")
                .description("XBox")
                .price(BigDecimal.valueOf(499.99))
                .quantity(11)
                .status(ProductStatus.ACTIVE)
                .build();

        productRepository.save(product);
        Product savedProduct1 =productRepository.save(product1);
        Product savedProduct2 = productRepository.save(product2);

        mockMvc.perform(get(PRODUCT_URI)
                        .param("minPrice", String.valueOf(BigDecimal.valueOf(300.00)))
                        .param("maxPrice", String.valueOf(BigDecimal.valueOf(500.00))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].id").value(savedProduct1.getId()))
                .andExpect(jsonPath("$.content[0].name").value("PS5"))
                .andExpect(jsonPath("$.content[0].description").value("Playstation"))
                .andExpect(jsonPath("$.content[0].price").value(BigDecimal.valueOf(399.99).doubleValue()))
                .andExpect(jsonPath("$.content[0].quantity").value(3))
                .andExpect(jsonPath("$.content[0].status").value(ProductStatus.ACTIVE.name()))
                .andExpect(jsonPath("$.content[0].createdAt").exists())
                .andExpect(jsonPath("$.content[0].updatedAt").exists())

                .andExpect(jsonPath("$.content[1].id").value(savedProduct2.getId()))
                .andExpect(jsonPath("$.content[1].name").value("XBOX"))
                .andExpect(jsonPath("$.content[1].description").value("XBox"))
                .andExpect(jsonPath("$.content[1].price").value(BigDecimal.valueOf(499.99).doubleValue()))
                .andExpect(jsonPath("$.content[1].quantity").value(11))
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
        Product product = Product.builder()
                .name("Keyboard")
                .description("Mechanical keyboard")
                .price(BigDecimal.valueOf(99.99))
                .quantity(10)
                .status(ProductStatus.ACTIVE)
                .build();

        Product product1 = Product.builder()
                .name("PS5")
                .description("Playstation")
                .price(BigDecimal.valueOf(499.99))
                .quantity(12)
                .status(ProductStatus.ACTIVE)
                .build();

        Product product2 = Product.builder()
                .name("XBOX")
                .description("XBox")
                .price(BigDecimal.valueOf(499.99))
                .quantity(11)
                .status(ProductStatus.ACTIVE)
                .build();

        Product savedProduct = productRepository.save(product);
        Product savedProduct1 = productRepository.save(product1);
        Product savedProduct2 = productRepository.save(product2);

        mockMvc.perform(get(PRODUCT_URI)
                        .param("sort", "id,DESC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.content[2].id").value(savedProduct.getId()))
                .andExpect(jsonPath("$.content[2].name").value("Keyboard"))
                .andExpect(jsonPath("$.content[2].description").value("Mechanical keyboard"))
                .andExpect(jsonPath("$.content[2].price").value(BigDecimal.valueOf(99.99).doubleValue()))
                .andExpect(jsonPath("$.content[2].quantity").value(10))
                .andExpect(jsonPath("$.content[2].status").value(ProductStatus.ACTIVE.name()))
                .andExpect(jsonPath("$.content[2].createdAt").exists())
                .andExpect(jsonPath("$.content[2].updatedAt").exists())

                .andExpect(jsonPath("$.content[1].id").value(savedProduct1.getId()))
                .andExpect(jsonPath("$.content[1].name").value("PS5"))
                .andExpect(jsonPath("$.content[1].description").value("Playstation"))
                .andExpect(jsonPath("$.content[1].price").value(BigDecimal.valueOf(499.99).doubleValue()))
                .andExpect(jsonPath("$.content[1].quantity").value(12))
                .andExpect(jsonPath("$.content[1].status").value(ProductStatus.ACTIVE.name()))
                .andExpect(jsonPath("$.content[1].createdAt").exists())
                .andExpect(jsonPath("$.content[1].updatedAt").exists())

                .andExpect(jsonPath("$.content[0].id").value(savedProduct2.getId()))
                .andExpect(jsonPath("$.content[0].name").value("XBOX"))
                .andExpect(jsonPath("$.content[0].description").value("XBox"))
                .andExpect(jsonPath("$.content[0].price").value(BigDecimal.valueOf(499.99).doubleValue()))
                .andExpect(jsonPath("$.content[0].quantity").value(11))
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
        Product product = Product.builder()
                .name("Keyboard")
                .description("Mechanical keyboard")
                .price(BigDecimal.valueOf(99.99))
                .quantity(10)
                .status(ProductStatus.ACTIVE)
                .build();

        Product product1 = Product.builder()
                .name("PS5")
                .description("Playstation")
                .price(BigDecimal.valueOf(399.99))
                .quantity(12)
                .status(ProductStatus.ACTIVE)
                .build();

        Product product2 = Product.builder()
                .name("XBOX")
                .description("XBox")
                .price(BigDecimal.valueOf(499.99))
                .quantity(11)
                .status(ProductStatus.ACTIVE)
                .build();

        productRepository.save(product);
        productRepository.save(product2);
        Product savedProduct1 = productRepository.save(product1);

        mockMvc.perform(get(PRODUCT_URI)
                        .param("sort", "id,DESC")
                        .param("keyword", "PS5")
                        .param("minPrice", String.valueOf(BigDecimal.valueOf(300.00)))
                        .param("maxPrice", String.valueOf(BigDecimal.valueOf(400.00)))
                        .param("status", ProductStatus.ACTIVE.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(savedProduct1.getId()))
                .andExpect(jsonPath("$.content[0].name").value("PS5"))
                .andExpect(jsonPath("$.content[0].description").value("Playstation"))
                .andExpect(jsonPath("$.content[0].price").value(BigDecimal.valueOf(399.99).doubleValue()))
                .andExpect(jsonPath("$.content[0].quantity").value(12))
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

        ProductCreateRequest request = new ProductCreateRequest(
                "PS5",
                "Playstation",
                BigDecimal.valueOf(399.99),
                12
        );

        MvcResult result = mockMvc.perform(post(PRODUCT_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("PS5"))
                .andExpect(jsonPath("$.description").value("Playstation"))
                .andExpect(jsonPath("$.price").value(BigDecimal.valueOf(399.99).doubleValue()))
                .andExpect(jsonPath("$.quantity").value(12))
                .andExpect(jsonPath("$.status").value(ProductStatus.ACTIVE.name()))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();

        ProductResponse response = objectMapper.readValue(responseBody, ProductResponse.class);

        Product savedProduct = productRepository.findById(response.id()).orElseThrow();

        assertThat(savedProduct.getName()).isEqualTo("PS5");
        assertThat(savedProduct.getDescription()).isEqualTo("Playstation");
        assertThat(savedProduct.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(399.99));
        assertThat(savedProduct.getQuantity()).isEqualTo(12);
        assertThat(savedProduct.getStatus()).isEqualTo(ProductStatus.ACTIVE);
        assertThat(savedProduct.getId()).isEqualTo(response.id());
        assertThat(productRepository.count()).isEqualTo(1);
    }

    @Test
    void putProduct_whenPutRequestIsValid_saveProductToDatabase() throws Exception {

        Product product = Product.builder()
                .name("PS5")
                .description("Playstation")
                .price(BigDecimal.valueOf(399.99))
                .quantity(12)
                .status(ProductStatus.ACTIVE)
                .build();

        Product savedProduct = productRepository.save(product);

        ProductPutRequest request = new ProductPutRequest(
                "PS5 update",
                "Playstation update",
                BigDecimal.valueOf(499.99),
                20
        );

        MvcResult result = mockMvc.perform(put(PRODUCT_URI + "/" + savedProduct.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("PS5 update"))
                .andExpect(jsonPath("$.description").value("Playstation update"))
                .andExpect(jsonPath("$.price").value(BigDecimal.valueOf(499.99).doubleValue()))
                .andExpect(jsonPath("$.quantity").value(20))
                .andExpect(jsonPath("$.status").value(ProductStatus.ACTIVE.name()))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();

        ProductResponse response = objectMapper.readValue(responseBody, ProductResponse.class);

        Product savedUpdateProduct = productRepository.findById(response.id()).orElseThrow();

        assertThat(savedUpdateProduct.getName()).isEqualTo("PS5 update");
        assertThat(savedUpdateProduct.getDescription()).isEqualTo("Playstation update");
        assertThat(savedUpdateProduct.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(499.99));
        assertThat(savedUpdateProduct.getQuantity()).isEqualTo(20);
        assertThat(savedUpdateProduct.getStatus()).isEqualTo(ProductStatus.ACTIVE);
        assertThat(savedUpdateProduct.getId()).isEqualTo(response.id());
        assertThat(productRepository.count()).isEqualTo(1);
    }

    @Test
    void patchProduct_whenPartiallyPatch_saveProductToDatabase() throws Exception {

        Product product = Product.builder()
                .name("PS5")
                .description("Playstation")
                .price(BigDecimal.valueOf(399.99))
                .quantity(12)
                .status(ProductStatus.ACTIVE)
                .build();

        Product savedProduct = productRepository.save(product);

        ProductPatchRequest request = new ProductPatchRequest(
                "PS5 update",
                null,
                BigDecimal.valueOf(499.99),
                null
        );

        MvcResult result = mockMvc.perform(patch(PRODUCT_URI + "/" + savedProduct.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("PS5 update"))
                .andExpect(jsonPath("$.description").value("Playstation"))
                .andExpect(jsonPath("$.price").value(BigDecimal.valueOf(499.99).doubleValue()))
                .andExpect(jsonPath("$.quantity").value(12))
                .andExpect(jsonPath("$.status").value(ProductStatus.ACTIVE.name()))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();

        ProductResponse response = objectMapper.readValue(responseBody, ProductResponse.class);

        Product savedUpdateProduct = productRepository.findById(response.id()).orElseThrow();

        assertThat(savedUpdateProduct.getName()).isEqualTo("PS5 update");
        assertThat(savedUpdateProduct.getDescription()).isEqualTo("Playstation");
        assertThat(savedUpdateProduct.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(499.99));
        assertThat(savedUpdateProduct.getQuantity()).isEqualTo(12);
        assertThat(savedUpdateProduct.getStatus()).isEqualTo(ProductStatus.ACTIVE);
        assertThat(savedUpdateProduct.getId()).isEqualTo(response.id());
        assertThat(productRepository.count()).isEqualTo(1);
    }

    @Test
    void patchProduct_whenPatchAllFieldsNull_saveProductToDatabase() throws Exception {

        Product product = Product.builder()
                .name("PS5")
                .description("Playstation")
                .price(BigDecimal.valueOf(399.99))
                .quantity(12)
                .status(ProductStatus.ACTIVE)
                .build();

        Product savedProduct = productRepository.save(product);

        ProductPatchRequest request = new ProductPatchRequest(
                null,
                null,
                null,
                null
        );

        MvcResult result = mockMvc.perform(patch(PRODUCT_URI + "/" + savedProduct.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("PS5"))
                .andExpect(jsonPath("$.description").value("Playstation"))
                .andExpect(jsonPath("$.price").value(BigDecimal.valueOf(399.99).doubleValue()))
                .andExpect(jsonPath("$.quantity").value(12))
                .andExpect(jsonPath("$.status").value(ProductStatus.ACTIVE.name()))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();

        ProductResponse response = objectMapper.readValue(responseBody, ProductResponse.class);

        Product savedUpdateProduct = productRepository.findById(response.id()).orElseThrow();

        assertThat(savedUpdateProduct.getName()).isEqualTo("PS5");
        assertThat(savedUpdateProduct.getDescription()).isEqualTo("Playstation");
        assertThat(savedUpdateProduct.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(399.99));
        assertThat(savedUpdateProduct.getQuantity()).isEqualTo(12);
        assertThat(savedUpdateProduct.getStatus()).isEqualTo(ProductStatus.ACTIVE);
        assertThat(savedUpdateProduct.getId()).isEqualTo(response.id());
        assertThat(productRepository.count()).isEqualTo(1);
    }

    @Test
    void deleteProduct_whenProductFound_productDeletedFromDatabase() throws Exception {
        Product product = Product.builder()
                .name("PS5")
                .description("Playstation")
                .price(BigDecimal.valueOf(399.99))
                .quantity(12)
                .status(ProductStatus.ACTIVE)
                .build();

        Product savedProduct = productRepository.save(product);

        mockMvc.perform(delete(PRODUCT_URI + "/" + savedProduct.getId()))
                .andExpect(status().isNoContent());

        assertThat(productRepository.existsById(savedProduct.getId())).isFalse();
    }

}
