package com.namnguyen.ecommerce_platform.product;

import com.namnguyen.ecommerce_platform.common.exception.NoResourceFoundException;
import com.namnguyen.ecommerce_platform.product.dto.*;
import com.namnguyen.ecommerce_platform.product.entity.Product;
import com.namnguyen.ecommerce_platform.product.enums.ProductStatus;
import com.namnguyen.ecommerce_platform.product.repository.ProductRepository;
import com.namnguyen.ecommerce_platform.product.service.ProductServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    private static final String NO_RESOURCE_FOUND_EXCEPTION_MESSAGE = "Product not found with id: ";

    private Product createProduct(
            Long id,
            String name,
            String description,
            BigDecimal price,
            Integer quantity
    ) {
        Product product = new Product();
        product.setId(id);
        product.setName(name);
        product.setDescription(description);
        product.setPrice(price);
        product.setQuantity(quantity);
        product.updateStatusBasedOnQuantity();
        return product;
    }

    @Test
    void createProduct_returnsProductResponse() {
        Long productId = 1L;

        ProductCreateRequest request = new ProductCreateRequest(
                "PS5",
                "Playstation 5 console",
                BigDecimal.valueOf(499.9),
                10
        );

        when(productRepository.save(any(Product.class)))
                .thenAnswer(inv -> {
                    Product productToSave = inv.getArgument(0);
                    productToSave.setId(productId);
                    return productToSave;
                });

        ProductResponse response = productService.createProduct(request);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(productId);
        assertThat(response.name()).isEqualTo("PS5");
        assertThat(response.description()).isEqualTo("Playstation 5 console");
        assertThat(response.price()).isEqualByComparingTo(BigDecimal.valueOf(499.9));
        assertThat(response.quantity()).isEqualTo(10);
        assertThat(response.status()).isEqualTo(ProductStatus.ACTIVE);

        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(productCaptor.capture());

        Product savedProduct = productCaptor.getValue();

        assertThat(savedProduct.getName()).isEqualTo("PS5");
        assertThat(savedProduct.getDescription()).isEqualTo("Playstation 5 console");
        assertThat(savedProduct.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(499.9));
        assertThat(savedProduct.getQuantity()).isEqualTo(10);
        assertThat(savedProduct.getStatus()).isEqualTo(ProductStatus.ACTIVE);

        verifyNoMoreInteractions(productRepository);
    }

    @Test
    void createProduct_whenQuantityIsZero_returnsOutOfStockProductResponse() {
        Long productId = 1L;

        ProductCreateRequest request = new ProductCreateRequest(
                "PS5",
                "Playstation 5 console",
                BigDecimal.valueOf(499.9),
                0
        );

        when(productRepository.save(any(Product.class)))
                .thenAnswer(inv -> {
                    Product productToSave = inv.getArgument(0);
                    productToSave.setId(productId);
                    return productToSave;
                });

        ProductResponse response = productService.createProduct(request);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(productId);
        assertThat(response.name()).isEqualTo("PS5");
        assertThat(response.quantity()).isEqualTo(0);
        assertThat(response.status()).isEqualTo(ProductStatus.OUT_OF_STOCK);

        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(productCaptor.capture());

        Product savedProduct = productCaptor.getValue();

        assertThat(savedProduct.getQuantity()).isEqualTo(0);
        assertThat(savedProduct.getStatus()).isEqualTo(ProductStatus.OUT_OF_STOCK);

        verifyNoMoreInteractions(productRepository);
    }

    @Test
    void getProductById_whenProductExists_returnsProductResponse() {
        Long productId = 1L;

        Product product = createProduct(
                productId,
                "PS5",
                "Playstation 5 console",
                BigDecimal.valueOf(499.9),
                10);

        when(productRepository.findById(productId))
                .thenReturn(Optional.of(product));

        ProductResponse response = productService.getProductById(productId);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(productId);
        assertThat(response.name()).isEqualTo("PS5");
        assertThat(response.description()).isEqualTo("Playstation 5 console");
        assertThat(response.price()).isEqualByComparingTo(BigDecimal.valueOf(499.9));
        assertThat(response.quantity()).isEqualTo(10);
        assertThat(response.status()).isEqualTo(ProductStatus.ACTIVE);

        verify(productRepository).findById(productId);
        verifyNoMoreInteractions(productRepository);
    }

    @Test
    void getProductById_whenProductDoesNotExist_throwsNoResourceFoundException() {
        Long productId = 999L;
        when(productRepository.findById(productId))
                .thenReturn(Optional.empty());

        NoResourceFoundException ex = assertThrows(
                NoResourceFoundException.class,
                () -> productService.getProductById(productId)
        );

        assertThat(ex).isNotNull();
        assertThat(ex.getMessage()).isEqualTo(NO_RESOURCE_FOUND_EXCEPTION_MESSAGE + productId);

        verify(productRepository).findById(productId);
        verifyNoMoreInteractions(productRepository);
    }

    @Test
    void getAllProducts_whenProductsExist_returnsPagedProductResponses() {
        Long productId = 1L;
        Product product = createProduct(
                productId,
                "PS5",
                "Playstation 5 console",
                BigDecimal.valueOf(499.9),
                10);

        Long productId2 = 2L;
        Product product1 = createProduct(
                productId2,
                "XBOX",
                "XBOX console",
                BigDecimal.valueOf(499.9),
                15);

        List<Product> products = List.of(
                product, product1
        );
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> productPage = new PageImpl<>(products, pageable, products.size());

        when(productRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(productPage);

        ProductFilterRequest request = new ProductFilterRequest(null, null, null, null);

        Page<ProductResponse> response = productService.getAllProducts(request, pageable);

        assertThat(response).isNotNull();
        assertThat(response.getTotalElements()).isEqualTo(2);
        assertThat(response.getNumberOfElements()).isEqualTo(2);
        assertThat(response.getTotalPages()).isEqualTo(1);
        assertThat(response.getSize()).isEqualTo(10);
        assertThat(response.getNumber()).isEqualTo(0);

        assertThat(response.getContent()).hasSize(2);

        ProductResponse firstProduct = response.getContent().getFirst();

        assertThat(firstProduct.id()).isEqualTo(productId);
        assertThat(firstProduct.name()).isEqualTo("PS5");
        assertThat(firstProduct.description()).isEqualTo("Playstation 5 console");
        assertThat(firstProduct.price()).isEqualByComparingTo(BigDecimal.valueOf(499.9));
        assertThat(firstProduct.quantity()).isEqualTo(10);
        assertThat(firstProduct.status()).isEqualTo(ProductStatus.ACTIVE);

        ProductResponse secondProduct = response.getContent().get(1);

        assertThat(secondProduct.id()).isEqualTo(productId2);
        assertThat(secondProduct.name()).isEqualTo("XBOX");
        assertThat(secondProduct.description()).isEqualTo("XBOX console");
        assertThat(secondProduct.price()).isEqualByComparingTo(BigDecimal.valueOf(499.9));
        assertThat(secondProduct.quantity()).isEqualTo(15);
        assertThat(secondProduct.status()).isEqualTo(ProductStatus.ACTIVE);

        verify(productRepository).findAll(any(Specification.class), eq(pageable));
        verifyNoMoreInteractions(productRepository);
    }

    @Test
    void getAllProducts_whenNoProductsExist_returnsEmptyPage() {

        List<Product> products = List.of();
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> productPage = new PageImpl<>(products, pageable, products.size());

        when(productRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(productPage);

        ProductFilterRequest request = new ProductFilterRequest(null, null, null, null);

        Page<ProductResponse> response = productService.getAllProducts(request, pageable);

        assertThat(response).isNotNull();
        assertThat(response.getTotalElements()).isEqualTo(0);
        assertThat(response.getNumberOfElements()).isEqualTo(0);
        assertThat(response.getTotalPages()).isEqualTo(0);
        assertThat(response.getSize()).isEqualTo(10);
        assertThat(response.getNumber()).isEqualTo(0);

        assertThat(response.getContent()).hasSize(0);

        verify(productRepository).findAll(any(Specification.class), eq(pageable));
        verifyNoMoreInteractions(productRepository);
    }

    @Test
    void putProduct_productExists_returnsProductResponse() {
        Long productId = 1L;

        Product product = createProduct(
                productId,
                "PS5",
                "Playstation 5 console",
                BigDecimal.valueOf(499.9),
                10);

        ProductPutRequest request = new ProductPutRequest(
                "PS5 update",
                "Playstation 5 console update",
                BigDecimal.valueOf(400),
                20
        );

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        ProductResponse response = productService.putProduct(productId, request);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(productId);
        assertThat(response.name()).isEqualTo("PS5 update");
        assertThat(response.description()).isEqualTo("Playstation 5 console update");
        assertThat(response.price()).isEqualByComparingTo(BigDecimal.valueOf(400));
        assertThat(response.quantity()).isEqualTo(20);
        assertThat(response.status()).isEqualTo(ProductStatus.ACTIVE);

        verify(productRepository).findById(productId);
        verifyNoMoreInteractions(productRepository);
    }

    @Test
    void putProduct_whenQuantityIsZero_returnsOutOfStockProductResponse() {
        Long productId = 1L;

        Product product = createProduct(
                productId,
                "PS5",
                "Playstation 5 console",
                BigDecimal.valueOf(499.9),
                10);

        ProductPutRequest request = new ProductPutRequest(
                "PS5 update",
                "Playstation 5 console update",
                BigDecimal.valueOf(400),
                0
        );

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        ProductResponse response = productService.putProduct(productId, request);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(productId);
        assertThat(response.name()).isEqualTo("PS5 update");
        assertThat(response.description()).isEqualTo("Playstation 5 console update");
        assertThat(response.price()).isEqualByComparingTo(BigDecimal.valueOf(400));
        assertThat(response.quantity()).isEqualTo(0);
        assertThat(response.status()).isEqualTo(ProductStatus.OUT_OF_STOCK);

        verify(productRepository).findById(productId);
        verifyNoMoreInteractions(productRepository);
    }

    @Test
    void putProduct_productNotExists_throwsNoResourceFoundException() {
        Long productId = 999L;

        ProductPutRequest request = new ProductPutRequest(
                "PS5 update",
                "Playstation 5 console update",
                BigDecimal.valueOf(400),
                20
        );

        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        NoResourceFoundException ex = assertThrows(
                NoResourceFoundException.class,
                () -> productService.putProduct(productId, request));

        assertThat(ex.getMessage()).isEqualTo(NO_RESOURCE_FOUND_EXCEPTION_MESSAGE + productId);

        verify(productRepository).findById(productId);
        verifyNoMoreInteractions(productRepository);
    }

    @Test
    void patchProduct_productExists_returnsProductResponse() {
        Long productId = 1L;

        Product product = createProduct(
                productId,
                "PS5",
                "Playstation 5 console",
                BigDecimal.valueOf(499.9),
                10);

        ProductPatchRequest request = new ProductPatchRequest(
                null,
                "Playstation 5 console update",
                null,
                20
        );

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        ProductResponse response = productService.patchProduct(productId, request);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(productId);
        assertThat(response.name()).isEqualTo("PS5");
        assertThat(response.description()).isEqualTo("Playstation 5 console update");
        assertThat(response.price()).isEqualByComparingTo(BigDecimal.valueOf(499.9));
        assertThat(response.quantity()).isEqualTo(20);
        assertThat(response.status()).isEqualTo(ProductStatus.ACTIVE);

        verify(productRepository).findById(productId);
        verifyNoMoreInteractions(productRepository);
    }

    @Test
    void patchProduct_whenAllFieldsAreNull_returnsProductResponse() {
        Long productId = 1L;

        Product product = createProduct(
                productId,
                "PS5",
                "Playstation 5 console",
                BigDecimal.valueOf(499.9),
                10);

        ProductPatchRequest request = new ProductPatchRequest(
                null,
                null,
                null,
                null
        );

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        ProductResponse response = productService.patchProduct(productId, request);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(productId);
        assertThat(response.name()).isEqualTo("PS5");
        assertThat(response.description()).isEqualTo("Playstation 5 console");
        assertThat(response.price()).isEqualByComparingTo(BigDecimal.valueOf(499.9));
        assertThat(response.quantity()).isEqualTo(10);
        assertThat(response.status()).isEqualTo(ProductStatus.ACTIVE);

        verify(productRepository).findById(productId);
        verifyNoMoreInteractions(productRepository);
    }

    @Test
    void patchProduct_productExists_zeroQuantity_returnsProductResponse() {
        Long productId = 1L;

        Product product = createProduct(
                productId,
                "PS5",
                "Playstation 5 console",
                BigDecimal.valueOf(499.9),
                10);

        ProductPatchRequest request = new ProductPatchRequest(
                null,
                "Playstation 5 console update",
                null,
                0
        );

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        ProductResponse response = productService.patchProduct(productId, request);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(productId);
        assertThat(response.name()).isEqualTo("PS5");
        assertThat(response.description()).isEqualTo("Playstation 5 console update");
        assertThat(response.price()).isEqualByComparingTo(BigDecimal.valueOf(499.9));
        assertThat(response.quantity()).isEqualTo(0);
        assertThat(response.status()).isEqualTo(ProductStatus.OUT_OF_STOCK);

        verify(productRepository).findById(productId);
        verifyNoMoreInteractions(productRepository);
    }

    @Test
    void patchProduct_productNotExists_throwsNoResourceFoundException() {
        Long productId = 999L;

        ProductPatchRequest request = new ProductPatchRequest(
                null,
                "Playstation 5 console update",
                null,
                20
        );

        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        NoResourceFoundException ex = assertThrows(
                NoResourceFoundException.class,
                () -> productService.patchProduct(productId, request));

        assertThat(ex.getMessage()).isEqualTo(NO_RESOURCE_FOUND_EXCEPTION_MESSAGE + productId);

        verify(productRepository).findById(productId);
        verifyNoMoreInteractions(productRepository);
    }

    @Test
    void deleteProduct_whenProductExists_deletesProduct() {
        Long productId = 1L;

        Product product = createProduct(
                productId,
                "PS5",
                "Playstation 5 console",
                BigDecimal.valueOf(499.9),
                10);

        when(productRepository.findById(productId))
                .thenReturn(Optional.of(product));

        productService.deleteProduct(productId);

        verify(productRepository).findById(productId);
        verify(productRepository).delete(product);
        verifyNoMoreInteractions(productRepository);
    }

    @Test
    void deleteProduct_whenProductDoesNotExist_throwsNoResourceFoundException() {
        Long productId = 999L;

        when(productRepository.findById(productId))
                .thenReturn(Optional.empty());

        NoResourceFoundException ex = assertThrows(
                NoResourceFoundException.class,
                () -> productService.deleteProduct(productId)
        );

        assertThat(ex.getMessage()).isEqualTo(NO_RESOURCE_FOUND_EXCEPTION_MESSAGE + productId);

        verify(productRepository).findById(productId);
        verify(productRepository, never()).delete(any(Product.class));
        verifyNoMoreInteractions(productRepository);
    }
}
