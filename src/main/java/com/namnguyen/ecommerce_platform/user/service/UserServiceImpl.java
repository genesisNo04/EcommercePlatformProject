package com.namnguyen.ecommerce_platform.user.service;

import com.namnguyen.ecommerce_platform.common.exception.*;
import com.namnguyen.ecommerce_platform.user.dto.*;
import com.namnguyen.ecommerce_platform.user.entity.User;
import com.namnguyen.ecommerce_platform.user.enums.Role;
import com.namnguyen.ecommerce_platform.user.mapper.UserMapper;
import com.namnguyen.ecommerce_platform.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private void validateEmailDoesNotExist(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Email already exists");
        }
    }

    private void validateEmailAvailableForUpdate(String email, Long currentUserId) {
        userRepository.findByEmail(email)
                .filter(existingUser -> !existingUser.getId().equals(currentUserId))
                .ifPresent(existingUser -> {
                    throw new DuplicateResourceException("Email already exists");
                });
    }

    private void validatePhoneDoesNotExist(String phone) {
        if (userRepository.existsByPhoneNumber(phone)) {
            throw new DuplicateResourceException("Phone number already exists");
        }
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> new NoResourceFoundException("User not found with id: " + userId));
    }

    private void validatePhoneAvailableForUpdate(String phoneNumber, Long currentUserId) {
        userRepository.findByPhoneNumber(phoneNumber)
                .filter(existingUser -> !existingUser.getId().equals(currentUserId))
                .ifPresent(existingUser -> {
                    throw new DuplicateResourceException("Phone number already exists");
                });
    }

    private UserResponse createUserWithRole(UserCreateRequest request, Role role) {
        validateEmailDoesNotExist(request.email());
        validatePhoneDoesNotExist(request.phoneNumber());

        User user = UserMapper.toEntity(request);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(role);

        User savedUser = userRepository.save(user);
        return UserMapper.toResponse(savedUser);
    }


    @Override
    @Transactional
    public UserResponse createUser(UserCreateRequest request) {
        return createUserWithRole(request, Role.CUSTOMER);
    }

    @Override
    @Transactional
    public UserResponse createAdminUser(UserCreateRequest request) {
        return createUserWithRole(request, Role.ADMIN);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        User user = getUserOrThrow(id);
        return UserMapper.toResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        List<User> users = userRepository.findAll();
        return users.stream().map(UserMapper::toResponse).toList();
    }

    @Override
    @Transactional
    public UserResponse putUser(Long id, UserPutRequest request) {
        User user = getUserOrThrow(id);

        validateEmailAvailableForUpdate(request.email(), id);
        validatePhoneAvailableForUpdate(request.phoneNumber(), id);

        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setPhoneNumber(request.phoneNumber());

        return UserMapper.toResponse(user);
    }

    @Override
    @Transactional
    public UserResponse patchUser(Long id, UserPatchRequest request) {
        User user = getUserOrThrow(id);

        if (request.email() != null) {
            validateEmailAvailableForUpdate(request.email(), id);
            user.setEmail(request.email());
        }

        if (request.password() != null) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
        }

        if (request.firstName() != null) {
            user.setFirstName(request.firstName());
        }

        if (request.lastName() != null) {
            user.setLastName(request.lastName());
        }

        if (request.phoneNumber() != null) {
            validatePhoneAvailableForUpdate(request.phoneNumber(), id);
            user.setPhoneNumber(request.phoneNumber());
        }

        return UserMapper.toResponse(user);
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        User user = getUserOrThrow(id);
        userRepository.delete(user);
    }
}
