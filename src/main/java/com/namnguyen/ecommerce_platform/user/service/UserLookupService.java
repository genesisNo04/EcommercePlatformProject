package com.namnguyen.ecommerce_platform.user.service;

import com.namnguyen.ecommerce_platform.common.exception.NoResourceFoundException;
import com.namnguyen.ecommerce_platform.user.entity.User;
import com.namnguyen.ecommerce_platform.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import static com.namnguyen.ecommerce_platform.user.error.UserErrorMessages.userNotFoundWithEmail;
import static com.namnguyen.ecommerce_platform.user.error.UserErrorMessages.userNotFoundWithId;

@Service
@RequiredArgsConstructor
public class UserLookupService {

    private final UserRepository userRepository;

    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() ->
                        new NoResourceFoundException(userNotFoundWithId(userId)));
    }

    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new NoResourceFoundException(userNotFoundWithEmail(email)));
    }
}
