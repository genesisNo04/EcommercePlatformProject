package com.namnguyen.ecommerce_platform.integration.security;

import com.namnguyen.ecommerce_platform.integration.BaseSecurityIntegrationTest;
import com.namnguyen.ecommerce_platform.user.dto.UserPatchRequest;
import com.namnguyen.ecommerce_platform.user.dto.UserPutRequest;
import com.namnguyen.ecommerce_platform.user.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static com.namnguyen.ecommerce_platform.testutil.TestDataFactory.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class UserSecurityIntegrationTest extends BaseSecurityIntegrationTest {

    @Test
    void getAllUsers_withCustomerJwt_returnsForbidden() throws Exception {

        User user = createDefaultCustomer();

        String token = loginAndGetToken(user.getEmail(), VALID_PASSWORD);

        mockMvc.perform(get(USER_URI)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAllUsers_withAdminJwt_returnsOk() throws Exception {

        User user = createDefaultAdmin();

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
        User user = createDefaultCustomer();

        String token = loginAndGetToken(user.getEmail(), VALID_PASSWORD);

        mockMvc.perform(get(USER_URI + "/" + user.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId()));
    }

    @Test
    void getUserById_whenUnauthenticated_returnsUnauthorized() throws Exception {
        Long userId = 999L;

        mockMvc.perform(get(USER_URI + "/" + userId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getUserById_withDifferentCustomerJwt_returnsForbidden() throws Exception {
        User user = createDefaultCustomer();

        String token = loginAndGetToken(user.getEmail(), VALID_PASSWORD);

        User otherUser = createUser(
                "seconduser@gmail.com",
                "test123456789",
                "firstName",
                "lastName",
                "12345678971",
                ROLE_CUSTOMER
        );

        mockMvc.perform(get(USER_URI + "/" + otherUser.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void getUserById_withAdminJwt_returnsUserResponse() throws Exception {
        User adminUser = createDefaultAdmin();

        String token = loginAndGetToken(adminUser.getEmail(), VALID_PASSWORD);

        User customerUser = createDefaultCustomer();

        mockMvc.perform(get(USER_URI + "/" + customerUser.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(customerUser.getId()));
    }

    @Test
    void putUser_withCustomerJwt_returnsUserResponse() throws Exception {
        User user = createDefaultCustomer();

        UserPutRequest request = createDefaultPutUserRequest();

        String token = loginAndGetToken(user.getEmail(), VALID_PASSWORD);

        mockMvc.perform(put(USER_URI + "/" + user.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId()));
    }

    @Test
    void putUser_withDifferentCustomerJwt_returnsForbidden() throws Exception {
        User user = createDefaultCustomer();

        UserPutRequest request = createDefaultPutUserRequest();

        String token = loginAndGetToken(user.getEmail(), VALID_PASSWORD);

        User otherUser = createUser(
                "seconduser@gmail.com",
                "test123456789",
                "firstName",
                "lastName",
                "12345678971",
                ROLE_CUSTOMER
        );

        mockMvc.perform(put(USER_URI + "/" + otherUser.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void putUser_withAdminJwt_returnsUserResponse() throws Exception {
        User adminUser = createDefaultAdmin();

        UserPutRequest request = createDefaultPutUserRequest();

        String token = loginAndGetToken(adminUser.getEmail(), VALID_PASSWORD);

        User customerUser = createDefaultCustomer();

        mockMvc.perform(put(USER_URI + "/" + customerUser.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(customerUser.getId()));
    }

    @Test
    void putUser_whenUnauthenticated_returnsUnauthorized() throws Exception {
        Long userId = 999L;

        mockMvc.perform(put(USER_URI + "/" + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDefaultPutUserRequest())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void patchUser_withCustomerJwt_returnsUserResponse() throws Exception {
        User user = createDefaultCustomer();

        UserPatchRequest request = createDefaultPatchUserRequest();

        String token = loginAndGetToken(user.getEmail(), VALID_PASSWORD);

        mockMvc.perform(patch(USER_URI + "/" + user.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId()));
    }

    @Test
    void patchUser_withCustomerJwt_patchOtherUser_returnsForbidden() throws Exception {
        User user = createDefaultCustomer();

        String token = loginAndGetToken(user.getEmail(), VALID_PASSWORD);

        UserPatchRequest request = createDefaultPatchUserRequest();

        User otherUser = createUser(
                "seconduser@gmail.com",
                "test123456789",
                "firstName",
                "lastName",
                "12345678971",
                ROLE_CUSTOMER
        );

        mockMvc.perform(patch(USER_URI + "/" + otherUser.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void patchUser_withAdminJwt_returnsUserResponse() throws Exception {
        User adminUser = createDefaultAdmin();

        UserPatchRequest request = createDefaultPatchUserRequest();

        String token = loginAndGetToken(adminUser.getEmail(), VALID_PASSWORD);

        User customerUser = createDefaultCustomer();

        mockMvc.perform(patch(USER_URI + "/" + customerUser.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(customerUser.getId()));
    }

    @Test
    void patchUser_whenUnauthenticated_returnsUnauthorized() throws Exception {
        Long userId = 999L;

        mockMvc.perform(patch(USER_URI + "/" + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDefaultPatchUserRequest())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteUser_withCustomerJwt_returnsForbidden() throws Exception {
        User user = createDefaultCustomer();

        String token = loginAndGetToken(user.getEmail(), VALID_PASSWORD);

        mockMvc.perform(delete(USER_URI + "/" + user.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteUser_withAdminJwt_returnsNoContent() throws Exception {
        User user = createDefaultCustomer();

        User adminUser = createDefaultAdmin();

        String token = loginAndGetToken(adminUser.getEmail(), VALID_PASSWORD);

        mockMvc.perform(delete(USER_URI + "/" + user.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteUser_whenUnauthenticated_returnsUnauthorized() throws Exception {
        Long userId = 999L;

        mockMvc.perform(delete(USER_URI + "/" + userId))
                .andExpect(status().isUnauthorized());
    }
}
