package com.namnguyen.ecommerce_platform.user.service;

import com.namnguyen.ecommerce_platform.common.exception.NoResourceFoundException;
import com.namnguyen.ecommerce_platform.user.entity.User;
import com.namnguyen.ecommerce_platform.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static com.namnguyen.ecommerce_platform.user.error.UserErrorMessages.userNotFoundWithEmail;
import static com.namnguyen.ecommerce_platform.user.error.UserErrorMessages.userNotFoundWithId;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserLookupService {

    private final UserRepository userRepository;

    public User getUserById(Long userId) {
        User user = userRepository.findById(userId)
                        .orElseThrow(() ->
                        new NoResourceFoundException(userNotFoundWithId(userId)));
        log.debug("Fetched user userId={}", userId);
        return user;
    }

    public User getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                        .orElseThrow(() ->
                        new NoResourceFoundException(userNotFoundWithEmail(email)));
        log.debug("Fetched user with email userId={}", user.getId());
        return user;
    }
}
