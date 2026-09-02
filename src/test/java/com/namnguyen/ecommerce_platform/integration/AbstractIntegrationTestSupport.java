package com.namnguyen.ecommerce_platform.integration;

import com.namnguyen.ecommerce_platform.auth.dto.LoginRequest;
import com.namnguyen.ecommerce_platform.auth.dto.RegisterRequest;
import com.namnguyen.ecommerce_platform.cart.dto.CartItemRequest;
import com.namnguyen.ecommerce_platform.cart.entity.Cart;
import com.namnguyen.ecommerce_platform.cart.entity.CartItem;
import com.namnguyen.ecommerce_platform.cart.repository.CartItemRepository;
import com.namnguyen.ecommerce_platform.cart.repository.CartRepository;
import com.namnguyen.ecommerce_platform.order.dto.CreateOrderItemRequest;
import com.namnguyen.ecommerce_platform.order.entity.Order;
import com.namnguyen.ecommerce_platform.order.entity.OrderItem;
import com.namnguyen.ecommerce_platform.order.enums.OrderStatus;
import com.namnguyen.ecommerce_platform.order.repository.OrderRepository;
import com.namnguyen.ecommerce_platform.payment.entity.Payment;
import com.namnguyen.ecommerce_platform.payment.enums.PaymentMethod;
import com.namnguyen.ecommerce_platform.payment.enums.PaymentStatus;
import com.namnguyen.ecommerce_platform.payment.repository.PaymentRepository;
import com.namnguyen.ecommerce_platform.product.dto.ProductCreateRequest;
import com.namnguyen.ecommerce_platform.product.dto.ProductPatchRequest;
import com.namnguyen.ecommerce_platform.product.dto.ProductPutRequest;
import com.namnguyen.ecommerce_platform.product.entity.Product;
import com.namnguyen.ecommerce_platform.product.enums.ProductStatus;
import com.namnguyen.ecommerce_platform.product.repository.ProductRepository;
import com.namnguyen.ecommerce_platform.user.dto.UserPatchRequest;
import com.namnguyen.ecommerce_platform.user.dto.UserPutRequest;
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

    protected ProductCreateRequest createDefaultProductCreateRequest() {
        return new ProductCreateRequest(
                "PS5",
                "Playstation",
                BigDecimal.valueOf(399.99),
                12
        );
    }

    protected ProductCreateRequest createProductCreateRequest(
            String productName,
            String productDescription,
            BigDecimal productPrice,
            int productQuantit
    ) {
        return new ProductCreateRequest(
                "PS5",
                "Playstation",
                BigDecimal.valueOf(399.99),
                12
        );
    }

    protected ProductPutRequest createDefaultPutProductRequest() {
        return new ProductPutRequest(
                "PS5 update",
                "Playstation update",
                BigDecimal.valueOf(499.99),
                20
        );
    }

    protected ProductPutRequest createPutProductRequest(
            String productName,
            String productDescription,
            BigDecimal price,
            int quantity
    ) {
        return new ProductPutRequest(
                productName,
                productDescription,
                price,
                quantity
        );
    }

    protected ProductPatchRequest createDefaultPatchProductRequest() {
        return new ProductPatchRequest(
                "PS5 update",
                "Playstation update",
                BigDecimal.valueOf(499.99),
                20
        );
    }

    protected ProductPatchRequest createPatchProductRequest(
            String productName,
            String productDescription,
            BigDecimal price,
            Integer quantity
    ) {
        return new ProductPatchRequest(
                productName,
                productDescription,
                price,
                quantity
        );
    }

    protected User createUser(String email,
                              String password,
                              String firstName,
                              String lastName,
                              String phoneNumber,
                              Role role) {
        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .firstName(firstName)
                .lastName(lastName)
                .role(role)
                .phoneNumber(phoneNumber)
                .build();

        return userRepository.save(user);
    }

    protected User createDefaultCustomer() {
        return createUser(
                "customer@gmail.com",
                VALID_PASSWORD,
                "test",
                "customer",
                "1234567891",
                Role.CUSTOMER
        );
    }

    protected User createDefaultAdmin() {
        return createUser(
                "admin@gmail.com",
                VALID_PASSWORD,
                "test",
                "admin",
                "1234567892",
                Role.ADMIN
        );
    }

    protected UserPutRequest createDefaultPutUserRequest() {
        return new UserPutRequest(
                "testupdate@gmail.com",
                VALID_PASSWORD,
                "testupdate",
                "userupdate",
                "1234567801"
        );
    }

    protected UserPutRequest createPutUserRequest(
            String email,
            String password,
            String firstName,
            String lastName,
            String phoneNumber
    ) {
        return new UserPutRequest(
                email,
                password,
                firstName,
                lastName,
                phoneNumber
        );
    }

    protected UserPatchRequest createDefaultPatchUserRequest() {
        return new UserPatchRequest(
                "testupdate@gmail.com",
                VALID_PASSWORD,
                "testupdate",
                "userupdate",
                "1234567801"
        );
    }

    protected UserPatchRequest createPatchUserRequest(
            String email,
            String password,
            String firstName,
            String lastName,
            String phoneNumber
    ) {
        return new UserPatchRequest(
                email,
                password,
                firstName,
                lastName,
                phoneNumber
        );
    }

    protected Product createProduct(String productName,
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

    protected Product createDefaultProduct() {

        Product product = Product.builder()
                .name("Keyboard")
                .description("Mechanical keyboard")
                .price(BigDecimal.valueOf(99.99))
                .quantity(50)
                .status(ProductStatus.ACTIVE)
                .build();

        return productRepository.save(product);
    }

    protected RegisterRequest createRegisterRequest(
            String email,
            String password,
            String firstName,
            String lastName,
            String phoneNumber
    ) {
        return new RegisterRequest(
                email,
                password,
                firstName,
                lastName,
                phoneNumber
        );
    }

    protected RegisterRequest createDefaultRegisterRequest() {
        return new RegisterRequest(
                VALID_EMAIL,
                VALID_PASSWORD,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                VALID_PHONE_NUMBER
        );
    }

    protected LoginRequest createLoginRequest(String email, String password) {
        return new LoginRequest(
                email,
                password
        );
    }

    protected LoginRequest createDefaultLoginRequest() {
        return new LoginRequest(
                "customer@gmail.com",
                VALID_PASSWORD
        );
    }

    protected CartItemRequest createCartItemRequest(Long productId, int quantity) {
        return new CartItemRequest(
                productId,
                quantity
        );
    }

    protected CreateOrderItemRequest createCreateOrderItemRequest(Long productId, int quantity) {
        return new CreateOrderItemRequest(
                productId,
                quantity
        );
    }

    protected Cart createCart(User user) {
        return cartRepository.save(Cart
                .builder()
                .user(user)
                .build());
    }

    protected CartItem createCartItem(Cart cart, Product product, int quantity) {
        return cartItemRepository.save(CartItem
                .builder()
                .cart(cart)
                .product(product)
                .quantity(quantity)
                .build());
    }

    protected Order createOrder(
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

    protected OrderItem createOrderItem(
            Order order,
            Product product,
            int quantity,
            BigDecimal price
    ) {
        return OrderItem
                .builder()
                .order(order)
                .quantity(quantity)
                .price(price)
                .build();
    }

    protected Payment createPayment(
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
