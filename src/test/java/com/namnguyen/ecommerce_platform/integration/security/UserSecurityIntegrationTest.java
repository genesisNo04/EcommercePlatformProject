package com.namnguyen.ecommerce_platform.integration.security;

import com.namnguyen.ecommerce_platform.integration.BaseSecurityIntegrationTest;
import com.namnguyen.ecommerce_platform.user.entity.User;
import com.namnguyen.ecommerce_platform.user.enums.Role;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static com.namnguyen.ecommerce_platform.testutil.TestDataFactory.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class UserSecurityIntegrationTest extends BaseSecurityIntegrationTest {

    @Test
    void getAllUsers_withCustomerJwt_returnsForbidden() throws Exception {

        User user = persistDefaultCustomer();

        String token = loginAndGetToken(user.getEmail(), VALID_PASSWORD);

        mockMvc.perform(get(USER_URI)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAllUsers_withAdminJwt_returnsOk() throws Exception {

        User user = persistDefaultAdmin();

        String token = loginAndGetToken(user.getEmail(), VALID_PASSWORD);

        mockMvc.perform(get(USER_URI)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void getAllUsers_whenUnauthenticated_returnsUnauthorized() throws Exception {
        mockMvc.perform(get(USER_URI))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getUserById_withCustomerJwt_returnsUserResponse() throws Exception {
        User user = persistDefaultCustomer();

        String token = loginAndGetToken(user.getEmail(), VALID_PASSWORD);

        mockMvc.perform(get(userUri(user.getId()))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId()));
    }

    @Test
    void getUserById_whenUnauthenticated_returnsUnauthorized() throws Exception {
        Long userId = 999L;

        mockMvc.perform(get(userUri(userId)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getUserById_withDifferentCustomerJwt_returnsForbidden() throws Exception {
        User user = persistDefaultCustomer();

        String token = loginAndGetToken(user.getEmail(), VALID_PASSWORD);

        User otherUser = persistUser(
                "seconduser@gmail.com",
                VALID_PASSWORD,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                "9876543213",
                Role.CUSTOMER
        );

        mockMvc.perform(get(userUri(otherUser.getId()))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void getUserById_withAdminJwt_returnsUserResponse() throws Exception {
        User adminUser = persistDefaultAdmin();

        String token = loginAndGetToken(adminUser.getEmail(), VALID_PASSWORD);

        User customerUser = persistDefaultCustomer();

        mockMvc.perform(get(userUri(customerUser.getId()))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(customerUser.getId()));
    }

    @Test
    void putUser_withCustomerJwt_returnsUserResponse() throws Exception {
        User user = persistDefaultCustomer();

        String token = loginAndGetToken(user.getEmail(), VALID_PASSWORD);

        mockMvc.perform(put(userUri(user.getId()))
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDefaultUserPutRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId()));
    }

    @Test
    void putUser_withDifferentCustomerJwt_returnsForbidden() throws Exception {
        User user = persistDefaultCustomer();

        String token = loginAndGetToken(user.getEmail(), VALID_PASSWORD);

        User otherUser = persistUser(
                "seconduser@gmail.com",
                VALID_PASSWORD,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                "9876543212",
                Role.CUSTOMER
        );

        mockMvc.perform(put(userUri(otherUser.getId()))
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDefaultUserPutRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    void putUser_withAdminJwt_returnsUserResponse() throws Exception {
        User adminUser = persistDefaultAdmin();

        String token = loginAndGetToken(adminUser.getEmail(), VALID_PASSWORD);

        User customerUser = persistDefaultCustomer();

        mockMvc.perform(put(userUri(customerUser.getId()))
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDefaultUserPutRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(customerUser.getId()));
    }

    @Test
    void putUser_whenUnauthenticated_returnsUnauthorized() throws Exception {
        Long userId = 999L;

        mockMvc.perform(put(userUri(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDefaultUserPutRequest())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void patchUser_withCustomerJwt_returnsUserResponse() throws Exception {
        User user = persistDefaultCustomer();

        String token = loginAndGetToken(user.getEmail(), VALID_PASSWORD);

        mockMvc.perform(patch(userUri(user.getId()))
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDefaultUserPatchRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId()));
    }

    @Test
    void patchUser_withDifferentCustomerJwt_returnsForbidden() throws Exception {
        User user = persistDefaultCustomer();

        String token = loginAndGetToken(user.getEmail(), VALID_PASSWORD);

        User otherUser = persistUser(
                "seconduser@gmail.com",
                VALID_PASSWORD,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                "98764532123",
                Role.CUSTOMER
        );

        mockMvc.perform(patch(userUri(otherUser.getId()))
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDefaultUserPatchRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    void patchUser_withAdminJwt_returnsUserResponse() throws Exception {
        User adminUser = persistDefaultAdmin();

        String token = loginAndGetToken(adminUser.getEmail(), VALID_PASSWORD);

        User customerUser = persistDefaultCustomer();

        mockMvc.perform(patch(userUri(customerUser.getId()))
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDefaultUserPatchRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(customerUser.getId()));
    }

    @Test
    void patchUser_whenUnauthenticated_returnsUnauthorized() throws Exception {
        Long userId = 999L;

        mockMvc.perform(patch(userUri(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDefaultUserPatchRequest())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteUser_withCustomerJwt_returnsForbidden() throws Exception {
        User user = persistDefaultCustomer();

        String token = loginAndGetToken(user.getEmail(), VALID_PASSWORD);

        mockMvc.perform(delete(userUri(user.getId()))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteUser_withAdminJwt_returnsNoContent() throws Exception {
        User user = persistDefaultCustomer();

        User adminUser = persistDefaultAdmin();

        String token = loginAndGetToken(adminUser.getEmail(), VALID_PASSWORD);

        mockMvc.perform(delete(userUri(user.getId()))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteUser_whenUnauthenticated_returnsUnauthorized() throws Exception {
        Long userId = 999L;

        mockMvc.perform(delete(userUri(userId)))
                .andExpect(status().isUnauthorized());
    }
}
