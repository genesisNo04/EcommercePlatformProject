package com.namnguyen.ecommerce_platform.testutil;

import com.namnguyen.ecommerce_platform.auth.dto.LoginRequest;
import com.namnguyen.ecommerce_platform.auth.dto.RegisterRequest;
import com.namnguyen.ecommerce_platform.cart.entity.Cart;
import com.namnguyen.ecommerce_platform.cart.entity.CartItem;
import com.namnguyen.ecommerce_platform.order.entity.Order;
import com.namnguyen.ecommerce_platform.order.entity.OrderItem;
import com.namnguyen.ecommerce_platform.order.enums.OrderStatus;
import com.namnguyen.ecommerce_platform.payment.entity.Payment;
import com.namnguyen.ecommerce_platform.payment.enums.PaymentMethod;
import com.namnguyen.ecommerce_platform.payment.enums.PaymentStatus;
import com.namnguyen.ecommerce_platform.product.entity.Product;
import com.namnguyen.ecommerce_platform.user.entity.User;
import com.namnguyen.ecommerce_platform.user.enums.Role;

import java.math.BigDecimal;
import java.util.ArrayList;

public final class TestDataFactory {

    public static final String LOGIN_URI = "/api/auth/login";
    public static final String CART_URI = "/api/cart";
    public static final String CART_ITEM_URI = "/api/cart/items";
    public static final String ORDER_URI = "/api/orders";
    public static final String REGISTER_URI = "/api/auth/register";
    public static final String PRODUCT_URI = "/api/products";
    public static final String USER_URI = "/api/users";

    public static final String VALID_EMAIL = "test@gmail.com";
    public static final String VALID_PASSWORD = "test123456789";
    public static final String WRONG_PASSWORD = "wrongpassword";
    public static final String VALID_FIRST_NAME = "test";
    public static final String VALID_LAST_NAME = "user";
    public static final String VALID_PHONE_NUMBER = "1234567891";
    public static final String VALID_PHONE_NUMBER_WITH_PLUS = "+1234567891";
    public static final String VALID_PRODUCT_NAME = "Test Product";
    public static final String VALID_PRODUCT_DESCRIPTION = "Test Product Description";
    public static final BigDecimal VALID_PRODUCT_PRICE = BigDecimal.valueOf(10.99);
    public static final Integer VALID_PRODUCT_QUANTITY = 50;

    public static final String INVALID_ID = "test";
    public static final String INVALID_EMAIL = "testgmail.com";
    public static final String INVALID_PASSWORD_LESS_THAN_EIGHT = "a".repeat(7);
    public static final String INVALID_PASSWORD_MORE_THAN_FIFTY = "a".repeat(51);
    public static final String INVALID_PHONE_NUMBER_LESS_THAN_TEN = "1".repeat(9);
    public static final String INVALID_PHONE_NUMBER_MORE_THAN_FIFTEEN = "1".repeat(16);
    public static final String INVALID_PHONE_NUMBER_WITH_MINUS = "-1234567891";
    public static final String INVALID_PRODUCT_NAME_MORE_THAN_LIMIT = "a".repeat(101);
    public static final String INVALID_PRODUCT_DESCRIPTION_MORE_THAN_LIMIT = "a".repeat(1001);
    public static final String INVALID_PRODUCT_DESCRIPTION_LESS_THAN_LIMIT = "test";
    public static final BigDecimal INVALID_PRODUCT_PRICE_ZERO = BigDecimal.ZERO;
    public static final Integer INVALID_PRODUCT_NEGATIVE_QUANTITY = -1;

    public static final String ENCODED_PASSWORD = "encodedPassword";
    public static final String MOCK_JWT_TOKEN = "fake-jwt-token";

    private TestDataFactory() {
    }

    public static String productUri(Long productId) {
        return PRODUCT_URI + "/" + productId;
    }

    public static String userUri(Long userId) {
        return USER_URI + "/" + userId;
    }

    public static String orderUri(Long orderId) {
        return ORDER_URI + "/" + orderId;
    }

    public static String paymentUri(Long orderId) {
        return orderUri(orderId) + "/payments";
    }

    public static String cartItemUri(Long productId) {
        return CART_ITEM_URI + "/" + productId;
    }

    public static String cartItemUri(String productId) {
        return CART_ITEM_URI + "/" + productId;
    }

    public static User createUser(Long userId) {
        User user = new User();
        user.setId(userId);
        user.setEmail(VALID_EMAIL);
        user.setPasswordHash(ENCODED_PASSWORD);
        user.setFirstName(VALID_FIRST_NAME);
        user.setLastName(VALID_LAST_NAME);
        user.setPhoneNumber(VALID_PHONE_NUMBER);
        user.setRole(Role.CUSTOMER);

        return user;
    }

    public static Product createProduct(
            Long productId,
            String name,
            BigDecimal price,
            Integer quantity
    ) {
        Product product = new Product();
        product.setId(productId);
        product.setName(name);
        product.setPrice(price);
        product.setQuantity(quantity);
        product.updateStatusBasedOnQuantity();
        return product;
    }

    public static Product createDefaultProduct(Long productId) {
        return createProduct(
                productId,
                VALID_PRODUCT_NAME,
                VALID_PRODUCT_PRICE,
                VALID_PRODUCT_QUANTITY
        );
    }

    public static CartItem createCartItem(
            Long cartItemId,
            Cart cart,
            Product product,
            Integer quantity
    ) {
        CartItem item = new CartItem();
        item.setId(cartItemId);
        item.setCart(cart);
        item.setProduct(product);
        item.setQuantity(quantity);
        return item;
    }

    public static Cart createCart(Long cartId, User user) {
        Cart cart = new Cart();
        cart.setId(cartId);
        cart.setUser(user);
        cart.setItems(new ArrayList<>());
        return cart;
    }

    public static Cart createCartWithItem(
            Long cartId,
            User user,
            Product product,
            Integer quantity
    ) {
        Cart cart = createCart(cartId, user);

        CartItem item = createCartItem(
                1L,
                cart,
                product,
                quantity
        );

        cart.addItem(item);

        return cart;
    }

    public static Order createOrder(
            Long orderId,
            BigDecimal total,
            OrderStatus status,
            User user
    ) {
        return Order.builder()
                .id(orderId)
                .total(total)
                .status(status)
                .user(user)
                .build();
    }

    public static OrderItem createOrderItem(
            Long orderItemId,
            Order order,
            Product product,
            Integer quantity,
            BigDecimal price
    ) {
        return OrderItem.builder()
                .id(orderItemId)
                .order(order)
                .product(product)
                .quantity(quantity)
                .price(price)
                .build();
    }

    public static Payment createPayment(
            Long paymentId,
            PaymentMethod method,
            PaymentStatus status,
            Order order,
            BigDecimal amount
    ) {
        return Payment.builder()
                .id(paymentId)
                .paymentMethod(method)
                .paymentStatus(status)
                .order(order)
                .amount(amount)
                .build();
    }

    public static RegisterRequest createDefaultRegisterRequest() {
        return new RegisterRequest(
                VALID_EMAIL,
                VALID_PASSWORD,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                VALID_PHONE_NUMBER
        );
    }

    public static LoginRequest createDefaultLoginRequest() {
        return new LoginRequest(
                VALID_EMAIL,
                VALID_PASSWORD
        );
    }
}
