package com.namnguyen.ecommerce_platform.integration;

import com.namnguyen.ecommerce_platform.cart.entity.Cart;
import com.namnguyen.ecommerce_platform.cart.entity.CartItem;
import com.namnguyen.ecommerce_platform.cart.repository.CartItemRepository;
import com.namnguyen.ecommerce_platform.cart.repository.CartRepository;
import com.namnguyen.ecommerce_platform.order.entity.Order;
import com.namnguyen.ecommerce_platform.order.entity.OrderItem;
import com.namnguyen.ecommerce_platform.order.enums.OrderStatus;
import com.namnguyen.ecommerce_platform.order.repository.OrderRepository;
import com.namnguyen.ecommerce_platform.payment.entity.Payment;
import com.namnguyen.ecommerce_platform.payment.enums.PaymentMethod;
import com.namnguyen.ecommerce_platform.payment.enums.PaymentStatus;
import com.namnguyen.ecommerce_platform.payment.repository.PaymentRepository;
import com.namnguyen.ecommerce_platform.product.entity.Product;
import com.namnguyen.ecommerce_platform.product.enums.ProductStatus;
import com.namnguyen.ecommerce_platform.product.repository.ProductRepository;
import com.namnguyen.ecommerce_platform.user.entity.User;
import com.namnguyen.ecommerce_platform.user.enums.Role;
import com.namnguyen.ecommerce_platform.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.cache.CacheManager;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;

import static com.namnguyen.ecommerce_platform.testutil.TestDataFactory.*;

@SpringBootTest
@ActiveProfiles("test")
@Import(AbstractIntegrationTestSupport.IntegrationTestConfig.class)
public abstract class AbstractIntegrationTestSupport {
    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected ProductRepository productRepository;

    @Autowired
    protected CartRepository cartRepository;

    @Autowired
    protected CartItemRepository cartItemRepository;

    @Autowired
    protected OrderRepository orderRepository;

    @Autowired
    protected PaymentRepository paymentRepository;

    @BeforeEach
    protected void cleanDatabase() {
        jdbcTemplate.execute("""
                TRUNCATE TABLE
                    payments,
                    order_items,
                    orders,
                    cart_items,
                    carts,
                    products,
                    users
                RESTART IDENTITY CASCADE
                """);
    }

    @AfterEach
    protected void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @TestConfiguration(proxyBeanMethods = false)
    public static class IntegrationTestConfig {

        @Bean
        @ServiceConnection
        PostgreSQLContainer postgreSQLContainer() {
            return new PostgreSQLContainer("postgres:16-alpine");
        }

        @Bean
        @Primary
        CacheManager noOpCacheManager() {
            return new NoOpCacheManager();
        }
    }

    protected User persistUser(String email,
                               String rawPassword,
                               String firstName,
                               String lastName,
                               String phoneNumber,
                               Role role) {
        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .firstName(firstName)
                .lastName(lastName)
                .role(role)
                .phoneNumber(phoneNumber)
                .build();

        return userRepository.save(user);
    }

    protected User persistDefaultCustomer() {
        return persistUser(
                VALID_EMAIL,
                VALID_PASSWORD,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                VALID_PHONE_NUMBER,
                Role.CUSTOMER
        );
    }

    protected User persistDefaultAdmin() {
        return persistUser(
                VALID_ADMIN_EMAIL,
                VALID_PASSWORD,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                VALID_ADMIN_PHONE_NUMBER,
                Role.ADMIN
        );
    }

    protected Product persistProduct(String productName,
                                     String productDescription,
                                     BigDecimal unitPrice,
                                     int quantity,
                                     ProductStatus status) {

        Product product = Product.builder()
                .name(productName)
                .description(productDescription)
                .price(unitPrice)
                .quantity(quantity)
                .status(status)
                .build();

        return productRepository.save(product);
    }

    protected Product persistDefaultProduct() {
        return persistProduct(
                VALID_PRODUCT_NAME,
                VALID_PRODUCT_DESCRIPTION,
                VALID_PRODUCT_PRICE,
                VALID_PRODUCT_QUANTITY,
                ProductStatus.ACTIVE
        );
    }

    protected Cart persistCart(User user) {
        return cartRepository.save(Cart
                .builder()
                .user(user)
                .build());
    }

    protected CartItem persistCartItem(Cart cart, Product product, int quantity) {
        return cartItemRepository.save(CartItem
                .builder()
                .cart(cart)
                .product(product)
                .quantity(quantity)
                .build());
    }

    protected Order persistOrder(
            BigDecimal total,
            OrderStatus status,
            User user,
            List<OrderItem> orderItems,
            Payment payment
    ) {
        return orderRepository.save(Order
                .builder()
                .total(total)
                .status(status)
                .user(user)
                .orderItems(orderItems)
                .payment(payment)
                .build());
    }

    protected Payment persistPayment(
            PaymentMethod method,
            PaymentStatus status,
            Order order,
            BigDecimal amount
    ) {
        return paymentRepository.save(Payment
                .builder()
                .paymentMethod(method)
                .paymentStatus(status)
                .order(order)
                .amount(amount)
                .build());
    }
}
