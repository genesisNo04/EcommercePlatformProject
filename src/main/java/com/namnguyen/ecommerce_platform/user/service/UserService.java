package com.namnguyen.ecommerce_platform.user.service;

import com.namnguyen.ecommerce_platform.user.dto.UserCreateRequest;
import com.namnguyen.ecommerce_platform.user.dto.UserPatchRequest;
import com.namnguyen.ecommerce_platform.user.dto.UserPutRequest;
import com.namnguyen.ecommerce_platform.user.dto.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface UserService {

    UserResponse createUser(UserCreateRequest request);

    UserResponse createAdminUser(UserCreateRequest request);

    UserResponse getUserById(Long userId);

    Page<UserResponse> getAllUsers(Pageable pageable);

    UserResponse putUser(Long userId, UserPutRequest request);

    UserResponse patchUser(Long userId, UserPatchRequest request);

    void deleteUser(Long userId);
}
