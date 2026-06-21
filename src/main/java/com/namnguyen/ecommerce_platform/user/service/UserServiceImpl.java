package com.namnguyen.ecommerce_platform.user.service;

import com.namnguyen.ecommerce_platform.common.exception.DuplicateResourceException;
import com.namnguyen.ecommerce_platform.user.dto.UserCreateRequest;
import com.namnguyen.ecommerce_platform.user.dto.UserPatchRequest;
import com.namnguyen.ecommerce_platform.user.dto.UserPutRequest;
import com.namnguyen.ecommerce_platform.user.dto.UserResponse;
import com.namnguyen.ecommerce_platform.user.entity.User;
import com.namnguyen.ecommerce_platform.user.mapper.UserMapper;
import com.namnguyen.ecommerce_platform.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    public void validateEmailDoesNotExist(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Email already exist");
        }
    }

    public void validateEmailAvailableForUpdateUser(String email, Long currentUserId) {
        userRepository.findByEmail(email)
                .filter(existingUser -> !existingUser.getId().equals(currentUserId))
                .ifPresent(existingUser -> {
                    throw new DuplicateResourceException("Email already exists");
                });
    }

    public void validatePhoneDoesNotExist(String phone) {
        if (userRepository.existsByPhoneNumber(phone)) {
            throw new DuplicateResourceException("Phone number already exist");
        }
    }

    @Override
    public UserResponse createUser(UserCreateRequest request) {
        validateEmailDoesNotExist(request.email());
        validatePhoneDoesNotExist(request.phoneNumber());

        User user = UserMapper.toEntity(request);
        user.setPasswordHash(request.password());

        User savedUser = userRepository.save(user);
        return UserMapper.toResponse(savedUser);
    }

    @Override
    public UserResponse getUserById(Long id) {
        return null;
    }

    @Override
    public List<UserResponse> getAllUsers() {
        return List.of();
    }

    @Override
    public UserResponse putUser(Long id, UserPutRequest request) {
        return null;
    }

    @Override
    public UserResponse patchUser(Long id, UserPatchRequest request) {
        return null;
    }

    @Override
    public void deleteUser(Long id) {

    }
}
