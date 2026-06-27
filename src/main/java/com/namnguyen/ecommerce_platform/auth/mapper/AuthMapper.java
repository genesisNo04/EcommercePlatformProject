package com.namnguyen.ecommerce_platform.auth.mapper;

import com.namnguyen.ecommerce_platform.auth.dto.RegisterRequest;
import com.namnguyen.ecommerce_platform.user.dto.UserCreateRequest;

public class AuthMapper {

    public static UserCreateRequest toUserCreateRequest(RegisterRequest request) {
        return new UserCreateRequest(
                request.email(),
                request.password(),
                request.firstName(),
                request.lastName(),
                request.phoneNumber());
    }
}
