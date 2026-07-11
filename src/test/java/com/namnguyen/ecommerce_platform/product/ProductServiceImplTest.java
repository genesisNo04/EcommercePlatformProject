package com.namnguyen.ecommerce_platform.product;

import com.namnguyen.ecommerce_platform.product.dto.ProductResponse;
import com.namnguyen.ecommerce_platform.product.entity.Product;
import com.namnguyen.ecommerce_platform.product.repository.ProductRepository;
import com.namnguyen.ecommerce_platform.product.service.ProductServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductServiceImpl productService;

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
}
