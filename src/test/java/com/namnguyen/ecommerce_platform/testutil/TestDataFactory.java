package com.namnguyen.ecommerce_platform.testutil;

import com.namnguyen.ecommerce_platform.cart.entity.Cart;
import com.namnguyen.ecommerce_platform.cart.entity.CartItem;
import com.namnguyen.ecommerce_platform.product.entity.Product;
import com.namnguyen.ecommerce_platform.user.entity.User;
import com.namnguyen.ecommerce_platform.user.enums.Role;

import java.math.BigDecimal;
import java.util.ArrayList;

public class TestDataFactory {

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
}
