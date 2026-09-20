package com.namnguyen.ecommerce_platform.user.service;

import com.namnguyen.ecommerce_platform.common.response.PageResponse;
import com.namnguyen.ecommerce_platform.user.dto.*;
import org.springframework.data.domain.Pageable;

public interface UserService {

    UserResponse createUser(UserCreateRequest request);

    UserResponse createAdminUser(UserCreateRequest request);

    UserResponse getUserById(Long userId);

    PageResponse<UserResponse> getAllUsers(UserFilterRequest request, Pageable pageable);

    UserResponse putUser(Long userId, UserPutRequest request);

    UserResponse patchUser(Long userId, UserPatchRequest request);

    void deleteUser(Long userId);
}
