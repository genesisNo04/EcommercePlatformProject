package com.namnguyen.ecommerce_platform.cart;

import com.namnguyen.ecommerce_platform.cart.entity.Cart;
import com.namnguyen.ecommerce_platform.cart.entity.CartItem;
import com.namnguyen.ecommerce_platform.cart.repository.CartRepository;
import com.namnguyen.ecommerce_platform.cart.service.CartServiceImpl;
import com.namnguyen.ecommerce_platform.user.entity.User;
import com.namnguyen.ecommerce_platform.user.enums.Role;
import com.namnguyen.ecommerce_platform.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CartServiceImplTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CartServiceImpl cartService;

    private User createUser() {
        User user = new User();
        user.setId(1L);
        user.setEmail("email@gmail.com");
        user.setPasswordHash("Test123");
        user.setFirstName("user");
        user.setLastName("test");
        user.setPhoneNumber("123456789");
        user.setRole(Role.CUSTOMER);

        return user;
    }

    private CartItem createCartItem() {
        CartItem item = new CartItem();
        item.setCart();
    }

    private Cart createCart(User user) {
        Cart cart = new Cart();

        return cart;
    }

    @Test
    void getCart_cartExists_returnCartResponse() {
        User user = createUser();
        Cart cart = new Cart();
        cart.setUser(user);

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(cartRepository.findByUserId(user.getId())).thenReturn(Optional.of(cart));
    }
}
