package com.namnguyen.ecommerce_platform.user.service;

import com.namnguyen.ecommerce_platform.common.caching.CacheNames;
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

import com.namnguyen.ecommerce_platform.common.exception.DuplicateResourceException;

import static com.namnguyen.ecommerce_platform.user.error.UserErrorMessages.EMAIL_ALREADY_EXISTS;
import static com.namnguyen.ecommerce_platform.user.error.UserErrorMessages.PHONE_NUMBER_ALREADY_EXISTS;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserLookupService userLookupService;

    private void validateEmailDoesNotExist(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException(EMAIL_ALREADY_EXISTS);
        }
    }

    private void validateEmailAvailableForUpdate(String email, Long currentUserId) {
        userRepository.findByEmail(email)
                .filter(existingUser -> !existingUser.getId().equals(currentUserId))
                .ifPresent(existingUser -> {
                    throw new DuplicateResourceException(EMAIL_ALREADY_EXISTS);
                });
    }

    private void validatePhoneDoesNotExist(String phone) {
        if (userRepository.existsByPhoneNumber(phone)) {
            throw new DuplicateResourceException(PHONE_NUMBER_ALREADY_EXISTS);
        }
    }

    private void validatePhoneAvailableForUpdate(String phoneNumber, Long currentUserId) {
        userRepository.findByPhoneNumber(phoneNumber)
                .filter(existingUser -> !existingUser.getId().equals(currentUserId))
                .ifPresent(existingUser -> {
                    throw new DuplicateResourceException(PHONE_NUMBER_ALREADY_EXISTS);
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
        User user = userLookupService.getUserById(userId);
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
        User user = userLookupService.getUserById(userId);

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
        User user = userLookupService.getUserById(userId);

        if (request.email() != null) {
            validateEmailAvailableForUpdate(request.email(), userId);
        }

        if (request.phoneNumber() != null) {
            validatePhoneAvailableForUpdate(request.phoneNumber(), userId);
        }

        if (request.email() != null) {
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
        User user = userLookupService.getUserById(userId);
        userRepository.delete(user);
    }
}
