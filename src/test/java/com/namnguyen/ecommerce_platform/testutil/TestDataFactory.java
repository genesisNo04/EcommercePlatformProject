package com.namnguyen.ecommerce_platform.testutil;

import com.namnguyen.ecommerce_platform.cart.entity.Cart;
import com.namnguyen.ecommerce_platform.cart.entity.CartItem;
import com.namnguyen.ecommerce_platform.order.entity.Order;
import com.namnguyen.ecommerce_platform.order.entity.OrderItem;
import com.namnguyen.ecommerce_platform.order.enums.OrderStatus;
import com.namnguyen.ecommerce_platform.payment.entity.Payment;
import com.namnguyen.ecommerce_platform.payment.enums.PaymentMethod;
import com.namnguyen.ecommerce_platform.payment.enums.PaymentStatus;
import com.namnguyen.ecommerce_platform.product.entity.Product;
import com.namnguyen.ecommerce_platform.product.enums.ProductStatus;
import com.namnguyen.ecommerce_platform.user.entity.User;
import com.namnguyen.ecommerce_platform.user.enums.Role;

import java.math.BigDecimal;
import java.util.ArrayList;

public class TestDataFactory {

    public static final String INVALID_ID = "test";
    public final static String LOGIN_URI = "/api/auth/login";
    public final static String CART_URI = "/api/cart";
    public final static String CART_ITEM_URI = "/api/cart/items";
    public final static String ORDER_URI = "/api/orders";
    public final static String REGISTER_URI = "/api/auth/register";
    public final static String PRODUCT_URI = "/api/products";
    public final static String USER_URI = "/api/users";
    public final static String VALID_EMAIL = "test@gmail.com";
    public final static String INVALID_EMAIL = "testgmail.com";
    public final static String VALID_PASSWORD = "test1237";
    public final static String INVALID_PASSWORD_LESS_THAN_EIGHT = "test123";
    public final static String INVALID_PASSWORD_MORE_THAN_FIFTY = "test1235645646467879461313131313456464as1d313a1sd31";
    public final static String VALID_FIRST_NAME = "test";
    public final static String VALID_LAST_NAME = "user";
    public final static String VALID_PHONE_NUMBER = "1234567891";
    public final static String VALID_PHONE_NUMBER_WITH_PLUS = "+1234567891";
    public final static String INVALID_PHONE_NUMBER_LESS_THAN_TEN = "123456789";
    public final static String INVALID_PHONE_NUMBER_MORE_THAN_FIFTEEN = "123456789";
    public final static String INVALID_PHONE_NUMBER_WITH_MINUS = "-1234567891";
    public final static Role ROLE = Role.CUSTOMER;
    public final static String VALID_PRODUCT_NAME = "Test Product";
    public final static String VALID_PRODUCT_DESCRIPTION = "Test Product Description";
    public final static BigDecimal VALID_PRODUCT_PRICE = BigDecimal.valueOf(10.99);
    public final static Integer VALID_PRODUCT_QUANTITY = 50;
    public final static ProductStatus VALID_PRODUCT_STATUS = ProductStatus.ACTIVE;
    public final static String INVALID_PRODUCT_NAME_MORE_THAN_LIMIT = "ibrwaeapeeezjuueygyqakycukbvdpjydtyyipmwuairzqndtgwraymiccxzpvhazaechphrjmfmbxedzhiiuznbtdmiyqktadhzi";
    public final static String INVALID_PRODUCT_DESCRIPTION_MORE_THAN_LIMIT = "ndneyybzzbpctyxfhgpzcfifjqkgzezwjvrwedkydbvqjhyxejwnjtinndtqvvmmutvknjgpynjaupuqevnnyjzctjuxkukipdmrpupwquddrphzuzeqvgcbbhbchhbkkfkzktnxhgvgcmwbiivppinhwpjaczqdtmtyapfrfttdqkmdjtzdfqypbwwhgmthwchkjqybetkkkcpcktcgkpydqqakpqytpxwdaqfrdnnidkywdftjkzcwujpjwgxxyjgtgnvwamapvgbpadjjtfkuzmtfqkdmmiciuvcgjfqgwkkirtzgnkqzqijtcibajdyugwibnuwnwzxmhuknzyxfvfuudfprgghgbzejnvizkbgtbemqtzpkqkddaupcuynirhezjtwaizkcxtitvtwrfnpnzqdbipiradmjfweggwddymeydhpippdpcrgtdfxryhqnacpjuvfjdjxxnqnttjrqrinkdviquwvjtyvhaxqpapuakdwfhtdcredxhrnahigfnyqcdjzvvcjvxcukierkgmxfchzeetrwbxrivkydcjdtptybgjgfryafyncztzdnbzpxupffvxudtmtjuqbdnxacxuudhkqitnaumkttcqehdxwxtqwkaihbywqxttmcgpnrejkhuckhkfbaxpnnfrazathcauzbbprtweggdqhtjhgnwachzdvbbdafjquqiqnifkzzwzfhzmnkqxncxdgheuyrpzbudfjdtruzwkptuxqifncppavenkmurwadhmdhkiehbmwngrdmuqpicrkqiwfueimwhbhumgewmazdfjpfgctbcmbxfdbxvxhhpnbuqywnnxkmiknzykrwjfayghtqkxbxpcbunwfeifehzgthgyrxyjwtavvzzpcnzqdhgpkgjrkdfjyfggjqcbcgtunzzhtgfxukydjtabdarvtwwuyvwrfnmuafmwhbhxfupjbewmvjraqhjqrzngquvvwbwadic";
    public final static String INVALID_PRODUCT_DESCRIPTION_LESS_THAN_LIMIT = "test";
    public final static BigDecimal INVALID_PRODUCT_PRICE_ZERO = BigDecimal.valueOf(0.0);
    public final static Integer INVALID_PRODUCT_NEGATIVE_QUANTITY = -1;



    private TestDataFactory(){
    }

    public static User createUser(Long userId) {
        User user = new User();
        user.setId(userId);
        user.setEmail("email@gmail.com");
        user.setPasswordHash("Test123");
        user.setFirstName("user");
        user.setLastName("test");
        user.setPhoneNumber("123456789");
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
}
