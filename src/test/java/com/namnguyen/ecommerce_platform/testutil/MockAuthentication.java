package com.namnguyen.ecommerce_platform.testutil;

import com.namnguyen.ecommerce_platform.security.user.CustomUserDetails;
import com.namnguyen.ecommerce_platform.user.enums.Role;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import static org.mockito.Mockito.*;

public class MockAuthentication {

    public static void authenticateUser(Long userId) {
        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        when(userDetails.getUserId()).thenReturn(userId);

        Authentication authentication =
                new UsernamePasswordAuthenticationToken(userDetails, null, List.of());

        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    public static void authenticateUser(Long userId, Role role) {
        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        when(userDetails.getUserId()).thenReturn(userId);

        Authentication authentication =
                new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        List.of(
                                new SimpleGrantedAuthority(
                                        "ROLE_" + role.name()
                                )
                        )
                );

        SecurityContext securityContext =
                SecurityContextHolder.createEmptyContext();

        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);
    }
}
