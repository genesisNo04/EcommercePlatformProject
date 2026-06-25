package com.namnguyen.ecommerce_platform.user.dto;

import com.namnguyen.ecommerce_platform.user.enums.Role;

public record UserFilterRequest(
        String email,
        String keyword,
        Role role
) {
}
