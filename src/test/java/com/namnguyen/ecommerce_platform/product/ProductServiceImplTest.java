package com.namnguyen.ecommerce_platform.product;

import com.namnguyen.ecommerce_platform.common.exception.NoResourceFoundException;
import com.namnguyen.ecommerce_platform.product.dto.ProductCreateRequest;
import com.namnguyen.ecommerce_platform.product.dto.ProductPatchRequest;
import com.namnguyen.ecommerce_platform.product.dto.ProductPutRequest;
import com.namnguyen.ecommerce_platform.product.dto.ProductResponse;
import com.namnguyen.ecommerce_platform.product.entity.Product;
import com.namnguyen.ecommerce_platform.product.repository.ProductRepository;
import com.namnguyen.ecommerce_platform.product.service.ProductServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
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

    private String NO_RESOURCE_FOUND_EXCEPTION_MESSAGE = "Product not found with id: ";

    @Test
    void getProductById_whenProductExists_returnsProductResponse() {
        Long productId = 1L;

        Product product = new Product();
        product.setId(productId);
        product.setName("PS5");
        product.setDescription("Playstation 5 console");
        product.setPrice(BigDecimal.valueOf(499.9));
        product.setQuantity(10);
        product.updateStatusBasedOnQuantity();

        when(productRepository.findById(productId))
                .thenReturn(Optional.of(product));

        ProductResponse response = productService.getProductById(productId);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(productId);
        assertThat(response.name()).isEqualTo("PS5");
        assertThat(response.description()).isEqualTo("Playstation 5 console");
        assertThat(response.price()).isEqualByComparingTo(BigDecimal.valueOf(499.9));
        assertThat(response.quantity()).isEqualTo(10);

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

        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(productCaptor.capture());

        Product savedProduct = productCaptor.getValue();

        assertThat(savedProduct.getName()).isEqualTo("PS5");
        assertThat(savedProduct.getDescription()).isEqualTo("Playstation 5 console");
        assertThat(savedProduct.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(499.9));
        assertThat(savedProduct.getQuantity()).isEqualTo(10);

        verifyNoMoreInteractions(productRepository);
    }

    @Test
    void putProduct_productExists_returnsProductResponse() {
        Long productId = 1L;

        Product product = new Product();
        product.setId(productId);
        product.setName("PS5");
        product.setDescription("Playstation 5 console");
        product.setPrice(BigDecimal.valueOf(499.9));
        product.setQuantity(10);
        product.updateStatusBasedOnQuantity();

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

        verify(productRepository).findById(productId);
        verifyNoMoreInteractions(productRepository);
    }

    @Test
    void putProduct_productNotExists_throwsNoResourceFoundException() {
        Long productId = 999L;

        ProductPutRequest request = new ProductPutRequest(
                null,
                "Playstation 5 console update",
                null,
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

        Product product = new Product();
        product.setId(productId);
        product.setName("PS5");
        product.setDescription("Playstation 5 console");
        product.setPrice(BigDecimal.valueOf(499.9));
        product.setQuantity(10);
        product.updateStatusBasedOnQuantity();

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
    void deleteProduct_whenProductExists() {
        Long productId = 1L;

        Product product = new Product();
        product.setId(productId);
        product.setName("PS5");
        product.setDescription("Playstation 5 console");
        product.setPrice(BigDecimal.valueOf(499.9));
        product.setQuantity(10);
        product.updateStatusBasedOnQuantity();

        when(productRepository.findById(productId))
                .thenReturn(Optional.of(product));

        productService.deleteProduct(productId);

        verify(productRepository).findById(productId);
        verify(productRepository).delete(product);
        verifyNoMoreInteractions(productRepository);
    }

    @Test
    void deleteProduct_whenProductNotExists() {
        Long productId = 999L;

        when(productRepository.findById(productId))
                .thenReturn(Optional.empty());

        NoResourceFoundException ex = assertThrows(
                NoResourceFoundException.class,
                () -> productService.deleteProduct(productId)
        );

        assertThat(ex.getMessage()).isEqualTo(NO_RESOURCE_FOUND_EXCEPTION_MESSAGE + productId);

        verify(productRepository).findById(productId);
        verifyNoMoreInteractions(productRepository);
    }
}
