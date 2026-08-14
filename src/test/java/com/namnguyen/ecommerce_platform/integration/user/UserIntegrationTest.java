package com.namnguyen.ecommerce_platform.integration.user;

import com.namnguyen.ecommerce_platform.integration.BaseIntegrationTest;
import com.namnguyen.ecommerce_platform.user.repository.UserRepository;
import org.junit.jupiter.api.AutoClose;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class UserIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void getAllUsers_when() {

    }
}
