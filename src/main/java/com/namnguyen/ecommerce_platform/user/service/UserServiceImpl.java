package com.namnguyen.ecommerce_platform.user.service;

import com.namnguyen.ecommerce_platform.common.config.CacheNames;
import com.namnguyen.ecommerce_platform.common.exception.*;
import com.namnguyen.ecommerce_platform.user.specifications.UserSpecification;
import com.namnguyen.ecommerce_platform.user.dto.*;
import com.namnguyen.ecommerce_platform.user.entity.User;
import com.namnguyen.ecommerce_platform.user.enums.Role;
import com.namnguyen.ecommerce_platform.user.mapper.UserMapper;
import com.namnguyen.ecommerce_platform.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    @Caching(
            evict = {
                    @CacheEvict(value = CacheNames.USERS_PAGES, allEntries = true)
            }
    )
    public UserResponse createUser(UserCreateRequest request) {
        return createUserWithRole(request, Role.CUSTOMER);
    }

    @Override
    @Transactional
    @Caching(
            evict = {
                    @CacheEvict(value = CacheNames.USERS_PAGES, allEntries = true)
            }
    )
    public UserResponse createAdminUser(UserCreateRequest request) {
        return createUserWithRole(request, Role.ADMIN);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = CacheNames.USERS, key = "#userId")
    public UserResponse getUserById(Long userId) {
        User user = getUserOrThrow(userId);
        return UserMapper.toResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(
            value = CacheNames.USERS_PAGES,
            key = "#request.email() + ':' + " +
                    "#request.keyword() + ':' + " +
                    "(#request.role() == null ? 'null' : #request.role().name()) + ':' + " +
                    "#pageable.pageNumber + ':' + " +
                    "#pageable.pageSize + ':' + " +
                    "#pageable.sort.toString().replace(' ', '')")
    public Page<UserResponse> getAllUsers(UserFilterRequest request, Pageable pageable) {
        Specification<User> spec = Specification
                .where(UserSpecification.nameContains(request.keyword()))
                .and(UserSpecification.emailContains(request.email()))
                .and(UserSpecification.hasRole(request.role()));

        return userRepository.findAll(spec, pageable).map(UserMapper::toResponse);
    }

    @Override
    @Transactional
    @Caching(
            put = {
                    @CachePut(value = CacheNames.USERS, key = "#userId")
            },
            evict = {
                    @CacheEvict(value = CacheNames.USERS_PAGES, allEntries = true)
            }
    )
    public UserResponse putUser(Long userId, UserPutRequest request) {
        User user = getUserOrThrow(userId);

        validateEmailAvailableForUpdate(request.email(), userId);
        validatePhoneAvailableForUpdate(request.phoneNumber(), userId);

        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setPhoneNumber(request.phoneNumber());

        return UserMapper.toResponse(user);
    }

    @Override
    @Transactional
    @Caching(
            put = {
                    @CachePut(value = CacheNames.USERS, key = "#userId")
            },
            evict = {
                    @CacheEvict(value = CacheNames.USERS_PAGES, allEntries = true)
            }
    )
    public UserResponse patchUser(Long userId, UserPatchRequest request) {
        User user = getUserOrThrow(userId);

        if (request.email() != null) {
            validateEmailAvailableForUpdate(request.email(), userId);
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
            validatePhoneAvailableForUpdate(request.phoneNumber(), userId);
            user.setPhoneNumber(request.phoneNumber());
        }

        return UserMapper.toResponse(user);
    }

    @Override
    @Transactional
    @Caching(
            evict = {
                    @CacheEvict(value = CacheNames.USERS, key = "#userId"),
                    @CacheEvict(value = CacheNames.USERS_PAGES, allEntries = true)
            }
    )
    public void deleteUser(Long userId) {
        User user = getUserOrThrow(userId);
        userRepository.delete(user);
    }
}
