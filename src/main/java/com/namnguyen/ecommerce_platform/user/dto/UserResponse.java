package com.namnguyen.ecommerce_platform.user.dto;

import com.namnguyen.ecommerce_platform.user.enums.Role;
import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String email,
        String firstName,
        String lastName,
        String phoneNumber,
        Role role,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
