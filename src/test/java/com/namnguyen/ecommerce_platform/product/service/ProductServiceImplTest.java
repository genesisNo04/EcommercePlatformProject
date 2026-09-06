package com.namnguyen.ecommerce_platform.product.service;

import com.namnguyen.ecommerce_platform.common.exception.NoResourceFoundException;
import com.namnguyen.ecommerce_platform.product.dto.*;
import com.namnguyen.ecommerce_platform.product.entity.Product;
import com.namnguyen.ecommerce_platform.product.enums.ProductStatus;
import com.namnguyen.ecommerce_platform.product.repository.ProductRepository;
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

import static com.namnguyen.ecommerce_platform.testutil.TestDataFactory.*;
import static com.namnguyen.ecommerce_platform.testutil.messages.ProductTestMessages.*;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    @Test
    void createProduct_returnsProductResponse() {
        Long productId = 1L;

        ProductCreateRequest productCreateRequest = createDefaultProductCreateRequest();

        when(productRepository.save(any(Product.class)))
                .thenAnswer(inv -> {
                    Product productToSave = inv.getArgument(0);
                    productToSave.setId(productId);
                    return productToSave;
                });

        ProductResponse productResponse = productService.createProduct(productCreateRequest);

        assertThat(productResponse).isNotNull();
        assertThat(productResponse.id()).isEqualTo(productId);
        assertThat(productResponse.name()).isEqualTo(productCreateRequest.name());
        assertThat(productResponse.description()).isEqualTo(productCreateRequest.description());
        assertThat(productResponse.price()).isEqualByComparingTo(productCreateRequest.price());
        assertThat(productResponse.quantity()).isEqualTo(productCreateRequest.quantity());
        assertThat(productResponse.status()).isEqualTo(ProductStatus.ACTIVE);

        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(productCaptor.capture());

        Product savedProduct = productCaptor.getValue();

        assertThat(savedProduct.getName()).isEqualTo(productCreateRequest.name());
        assertThat(savedProduct.getDescription()).isEqualTo(productCreateRequest.description());
        assertThat(savedProduct.getPrice()).isEqualByComparingTo(productCreateRequest.price());
        assertThat(savedProduct.getQuantity()).isEqualTo(productCreateRequest.quantity());
        assertThat(savedProduct.getStatus()).isEqualTo(ProductStatus.ACTIVE);

        verifyNoMoreInteractions(productRepository);
    }

    @Test
    void createProduct_whenQuantityIsZero_returnsOutOfStockProductResponse() {
        Long productId = 1L;

        ProductCreateRequest productCreateRequest = createProductCreateRequest(
                VALID_PRODUCT_NAME,
                VALID_PRODUCT_DESCRIPTION,
                VALID_PRODUCT_PRICE,
                0
        );

        when(productRepository.save(any(Product.class)))
                .thenAnswer(inv -> {
                    Product productToSave = inv.getArgument(0);
                    productToSave.setId(productId);
                    return productToSave;
                });

        ProductResponse productResponse = productService.createProduct(productCreateRequest);

        assertThat(productResponse).isNotNull();
        assertThat(productResponse.id()).isEqualTo(productId);
        assertThat(productResponse.name()).isEqualTo(productCreateRequest.name());
        assertThat(productResponse.quantity()).isEqualTo(0);
        assertThat(productResponse.status()).isEqualTo(ProductStatus.OUT_OF_STOCK);

        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(productCaptor.capture());

        Product capturedProduct = productCaptor.getValue();

        assertThat(capturedProduct.getQuantity()).isEqualTo(0);
        assertThat(capturedProduct.getStatus()).isEqualTo(ProductStatus.OUT_OF_STOCK);

        verifyNoMoreInteractions(productRepository);
    }

    @Test
    void getProductById_whenProductExists_returnsProductResponse() {
        Long productId = 1L;

        Product product = createDefaultProduct(productId);

        when(productRepository.findById(productId))
                .thenReturn(Optional.of(product));

        ProductResponse productResponse = productService.getProductById(productId);

        assertThat(productResponse).isNotNull();
        assertThat(productResponse.id()).isEqualTo(productId);
        assertThat(productResponse.name()).isEqualTo(product.getName());
        assertThat(productResponse.description()).isEqualTo(product.getDescription());
        assertThat(productResponse.price()).isEqualByComparingTo(product.getPrice());
        assertThat(productResponse.quantity()).isEqualTo(product.getQuantity());
        assertThat(productResponse.status()).isEqualTo(ProductStatus.ACTIVE);

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
        assertThat(ex.getMessage()).isEqualTo(productNotFoundWithId(productId));

        verify(productRepository).findById(productId);
        verifyNoMoreInteractions(productRepository);
    }

    @Test
    void getAllProducts_whenProductsExist_returnsPagedProductResponses() {
        Long firstProductId = 1L;
        Long secondProductId = 2L;
        Product firstProduct = createDefaultProduct(firstProductId);
        Product secondProduct = createDefaultProduct(secondProductId);

        List<Product> products = List.of(
                firstProduct, secondProduct
        );
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> productPage = new PageImpl<>(products, pageable, products.size());

        when(productRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(productPage);

        ProductFilterRequest productFilterRequest = new ProductFilterRequest(null, null, null, null);

        Page<ProductResponse> productResponses = productService.getAllProducts(productFilterRequest, pageable);

        assertThat(productResponses).isNotNull();
        assertThat(productResponses.getTotalElements()).isEqualTo(2);
        assertThat(productResponses.getNumberOfElements()).isEqualTo(2);
        assertThat(productResponses.getTotalPages()).isEqualTo(1);
        assertThat(productResponses.getSize()).isEqualTo(10);
        assertThat(productResponses.getNumber()).isEqualTo(0);

        assertThat(productResponses.getContent()).hasSize(2);

        ProductResponse firstProductResponse = productResponses.getContent().getFirst();

        assertThat(firstProductResponse.id()).isEqualTo(firstProductId);
        assertThat(firstProductResponse.name()).isEqualTo(firstProduct.getName());
        assertThat(firstProductResponse.description()).isEqualTo(firstProduct.getDescription());
        assertThat(firstProductResponse.price()).isEqualByComparingTo(firstProduct.getPrice());
        assertThat(firstProductResponse.quantity()).isEqualTo(firstProduct.getQuantity());
        assertThat(firstProductResponse.status()).isEqualTo(ProductStatus.ACTIVE);

        ProductResponse secondProductResponse = productResponses.getContent().get(1);

        assertThat(secondProductResponse.id()).isEqualTo(secondProductId);
        assertThat(secondProductResponse.name()).isEqualTo(secondProduct.getName());
        assertThat(secondProductResponse.description()).isEqualTo(secondProduct.getDescription());
        assertThat(secondProductResponse.price()).isEqualByComparingTo(secondProduct.getPrice());
        assertThat(secondProductResponse.quantity()).isEqualTo(secondProduct.getQuantity());
        assertThat(secondProductResponse.status()).isEqualTo(ProductStatus.ACTIVE);

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

        ProductFilterRequest productFilterRequest = new ProductFilterRequest(null, null, null, null);

        Page<ProductResponse> productResponses = productService.getAllProducts(productFilterRequest, pageable);

        assertThat(productResponses).isNotNull();
        assertThat(productResponses.getTotalElements()).isEqualTo(0);
        assertThat(productResponses.getNumberOfElements()).isEqualTo(0);
        assertThat(productResponses.getTotalPages()).isEqualTo(0);
        assertThat(productResponses.getSize()).isEqualTo(10);
        assertThat(productResponses.getNumber()).isEqualTo(0);

        assertThat(productResponses.getContent()).hasSize(0);

        verify(productRepository).findAll(any(Specification.class), eq(pageable));
        verifyNoMoreInteractions(productRepository);
    }

    @Test
    void putProduct_whenProductExists_returnsProductResponse() {
        Long productId = 1L;

        Product product = createDefaultProduct(productId);

        ProductPutRequest productPutRequest = createDefaultProductPutRequest();

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        ProductResponse productResponse = productService.putProduct(productId, productPutRequest);

        assertThat(productResponse).isNotNull();
        assertThat(productResponse.id()).isEqualTo(productId);
        assertThat(productResponse.name()).isEqualTo(productPutRequest.name());
        assertThat(productResponse.description()).isEqualTo(productPutRequest.description());
        assertThat(productResponse.price()).isEqualByComparingTo(productPutRequest.price());
        assertThat(productResponse.quantity()).isEqualTo(productPutRequest.quantity());
        assertThat(productResponse.status()).isEqualTo(ProductStatus.ACTIVE);

        verify(productRepository).findById(productId);
        verifyNoMoreInteractions(productRepository);

        assertThat(product.getName())
                .isEqualTo(productPutRequest.name());
        assertThat(product.getDescription())
                .isEqualTo(productPutRequest.description());
        assertThat(product.getPrice())
                .isEqualByComparingTo(productPutRequest.price());
        assertThat(product.getQuantity())
                .isEqualTo(productPutRequest.quantity());
        assertThat(product.getStatus())
                .isEqualTo(ProductStatus.ACTIVE);
    }

    @Test
    void putProduct_whenQuantityIsZero_returnsOutOfStockProductResponse() {
        Long productId = 1L;

        Product product = createDefaultProduct(productId);

        ProductPutRequest productPutRequest = createProductPutRequest(
                VALID_UPDATE_PRODUCT_NAME,
                VALID_UPDATE_PRODUCT_DESCRIPTION,
                VALID_UPDATE_PRODUCT_PRICE,
                0
        );

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        ProductResponse productResponse = productService.putProduct(productId, productPutRequest);

        assertThat(productResponse).isNotNull();
        assertThat(productResponse.id()).isEqualTo(productId);
        assertThat(productResponse.name()).isEqualTo(productPutRequest.name());
        assertThat(productResponse.description()).isEqualTo(productPutRequest.description());
        assertThat(productResponse.price()).isEqualByComparingTo(productPutRequest.price());
        assertThat(productResponse.quantity()).isEqualTo(productPutRequest.quantity());
        assertThat(productResponse.status()).isEqualTo(ProductStatus.OUT_OF_STOCK);

        verify(productRepository).findById(productId);
        verifyNoMoreInteractions(productRepository);

        assertThat(product.getName())
                .isEqualTo(productPutRequest.name());
        assertThat(product.getDescription())
                .isEqualTo(productPutRequest.description());
        assertThat(product.getPrice())
                .isEqualByComparingTo(productPutRequest.price());
        assertThat(product.getQuantity())
                .isEqualTo(0);
        assertThat(product.getStatus())
                .isEqualTo(ProductStatus.OUT_OF_STOCK);
    }

    @Test
    void putProduct_whenProductDoesNotExist_throwsNoResourceFoundException() {
        Long productId = 999L;

        ProductPutRequest productPutRequest = createDefaultProductPutRequest();

        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        NoResourceFoundException ex = assertThrows(
                NoResourceFoundException.class,
                () -> productService.putProduct(productId, productPutRequest));

        assertThat(ex.getMessage()).isEqualTo(productNotFoundWithId(productId));

        verify(productRepository).findById(productId);
        verifyNoMoreInteractions(productRepository);
    }

    @Test
    void patchProduct_whenProductExists_returnsProductResponse() {
        Long productId = 1L;

        Product product = createDefaultProduct(productId);

        ProductPatchRequest productPatchRequest = createProductPatchRequest(
                null,
                VALID_UPDATE_PRODUCT_DESCRIPTION,
                null,
                VALID_UPDATE_PRODUCT_QUANTITY
        );

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        String originalName = product.getName();
        BigDecimal originalPrice = product.getPrice();

        ProductResponse productResponse = productService.patchProduct(productId, productPatchRequest);

        assertThat(productResponse).isNotNull();
        assertThat(productResponse.id()).isEqualTo(productId);
        assertThat(productResponse.name()).isEqualTo(originalName);
        assertThat(productResponse.description()).isEqualTo(productPatchRequest.description());
        assertThat(productResponse.price()).isEqualByComparingTo(originalPrice);
        assertThat(productResponse.quantity()).isEqualTo(productPatchRequest.quantity());
        assertThat(productResponse.status()).isEqualTo(ProductStatus.ACTIVE);

        verify(productRepository).findById(productId);
        verifyNoMoreInteractions(productRepository);

        assertThat(product.getName())
                .isEqualTo(originalName);
        assertThat(product.getDescription())
                .isEqualTo(VALID_UPDATE_PRODUCT_DESCRIPTION);
        assertThat(product.getPrice())
                .isEqualByComparingTo(originalPrice);
        assertThat(product.getQuantity())
                .isEqualTo(VALID_UPDATE_PRODUCT_QUANTITY);
        assertThat(product.getStatus())
                .isEqualTo(ProductStatus.ACTIVE);
    }

    @Test
    void patchProduct_whenAllFieldsAreNull_returnsProductResponse() {
        Long productId = 1L;

        Product product = createDefaultProduct(productId);

        ProductPatchRequest productPatchRequest = createProductPatchRequest(
                null,
                null,
                null,
                null
        );

        String originalName = product.getName();
        String originalDescription = product.getDescription();
        BigDecimal originalPrice = product.getPrice();
        Integer originalQuantity = product.getQuantity();

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        ProductResponse productResponse = productService.patchProduct(productId, productPatchRequest);

        assertThat(productResponse).isNotNull();
        assertThat(productResponse.id()).isEqualTo(productId);
        assertThat(productResponse.name()).isEqualTo(originalName);
        assertThat(productResponse.description()).isEqualTo(originalDescription);
        assertThat(productResponse.price()).isEqualByComparingTo(originalPrice);
        assertThat(productResponse.quantity()).isEqualTo(originalQuantity);
        assertThat(productResponse.status()).isEqualTo(ProductStatus.ACTIVE);

        verify(productRepository).findById(productId);
        verifyNoMoreInteractions(productRepository);

        assertThat(product.getName())
                .isEqualTo(originalName);
        assertThat(product.getDescription())
                .isEqualTo(originalDescription);
        assertThat(product.getPrice())
                .isEqualByComparingTo(originalPrice);
        assertThat(product.getQuantity())
                .isEqualTo(originalQuantity);
        assertThat(product.getStatus())
                .isEqualTo(ProductStatus.ACTIVE);
    }

    @Test
    void patchProduct_whenQuantityIsZero_returnsOutOfStockProductResponse() {
        Long productId = 1L;

        Product product = createDefaultProduct(productId);

        ProductPatchRequest productPatchRequest = createProductPatchRequest(
                null,
                VALID_UPDATE_PRODUCT_DESCRIPTION,
                null,
                0
        );

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        String originalName = product.getName();
        BigDecimal originalPrice = product.getPrice();

        ProductResponse productResponse = productService.patchProduct(productId, productPatchRequest);

        assertThat(productResponse).isNotNull();
        assertThat(productResponse.id()).isEqualTo(productId);
        assertThat(productResponse.name()).isEqualTo(originalName);
        assertThat(productResponse.description()).isEqualTo(productPatchRequest.description());
        assertThat(productResponse.price()).isEqualByComparingTo(originalPrice);
        assertThat(productResponse.quantity()).isEqualTo(0);
        assertThat(productResponse.status()).isEqualTo(ProductStatus.OUT_OF_STOCK);

        verify(productRepository).findById(productId);
        verifyNoMoreInteractions(productRepository);

        assertThat(product.getName())
                .isEqualTo(originalName);
        assertThat(product.getDescription())
                .isEqualTo(VALID_UPDATE_PRODUCT_DESCRIPTION);
        assertThat(product.getPrice())
                .isEqualByComparingTo(originalPrice);
        assertThat(product.getQuantity())
                .isEqualTo(0);
        assertThat(product.getStatus())
                .isEqualTo(ProductStatus.OUT_OF_STOCK);
    }

    @Test
    void patchProduct_whenProductDoesNotExist_throwsNoResourceFoundException() {
        Long productId = 999L;

        ProductPatchRequest productPatchRequest = createDefaultProductPatchRequest();

        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        NoResourceFoundException ex = assertThrows(
                NoResourceFoundException.class,
                () -> productService.patchProduct(productId, productPatchRequest));

        assertThat(ex.getMessage()).isEqualTo(productNotFoundWithId(productId));

        verify(productRepository).findById(productId);
        verifyNoMoreInteractions(productRepository);
    }

    @Test
    void deleteProduct_whenProductExists_deletesProduct() {
        Long productId = 1L;

        Product product = createDefaultProduct(productId);

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

        assertThat(ex.getMessage()).isEqualTo(productNotFoundWithId(productId));

        verify(productRepository).findById(productId);
        verifyNoMoreInteractions(productRepository);
    }
}
