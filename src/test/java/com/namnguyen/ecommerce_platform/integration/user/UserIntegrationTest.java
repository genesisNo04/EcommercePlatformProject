package com.namnguyen.ecommerce_platform.integration.user;

import com.namnguyen.ecommerce_platform.integration.BaseIntegrationTest;
import com.namnguyen.ecommerce_platform.testutil.MockAuthentication;
import com.namnguyen.ecommerce_platform.user.dto.UserPatchRequest;
import com.namnguyen.ecommerce_platform.user.dto.UserPutRequest;
import com.namnguyen.ecommerce_platform.user.entity.User;
import com.namnguyen.ecommerce_platform.user.enums.Role;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import static com.namnguyen.ecommerce_platform.testutil.TestDataFactory.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class UserIntegrationTest extends BaseIntegrationTest {

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllUsers_withAdminRole_returnsPageOfUsers() throws Exception {

        User user = createUser(
                "test@gmail.com",
                "test123456789",
                "test",
                "user",
                "123456789",
                Role.CUSTOMER
        );

        User user1 = createUser(
                "test1@gmail.com",
                "test123456789",
                "test1",
                "user1",
                "123456780",
                Role.ADMIN
        );

        mockMvc.perform(get(USER_URI))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].id").value(user.getId()))
                .andExpect(jsonPath("$.content[0].email").value("test@gmail.com"))
                .andExpect(jsonPath("$.content[0].firstName").value("test"))
                .andExpect(jsonPath("$.content[0].lastName").value("user"))
                .andExpect(jsonPath("$.content[0].phoneNumber").value("123456789"))
                .andExpect(jsonPath("$.content[0].role").value(Role.CUSTOMER.name()))
                .andExpect(jsonPath("$.content[0].createdAt").exists())
                .andExpect(jsonPath("$.content[0].updatedAt").exists())

                .andExpect(jsonPath("$.content[1].id").value(user1.getId()))
                .andExpect(jsonPath("$.content[1].email").value("test1@gmail.com"))
                .andExpect(jsonPath("$.content[1].firstName").value("test1"))
                .andExpect(jsonPath("$.content[1].lastName").value("user1"))
                .andExpect(jsonPath("$.content[1].phoneNumber").value("123456780"))
                .andExpect(jsonPath("$.content[1].role").value(Role.ADMIN.name()))
                .andExpect(jsonPath("$.content[1].createdAt").exists())
                .andExpect(jsonPath("$.content[1].updatedAt").exists())

                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.numberOfElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllUsers_withFilters_returnsPageOfUsers() throws Exception {

        createUser(
                "test@gmail.com",
                "test123456789",
                "test",
                "user",
                "123456789",
                Role.CUSTOMER
        );

        User user1 = createUser(
                "test1@gmail.com",
                "test123456789",
                "test1",
                "user1",
                "123456780",
                Role.ADMIN
        );

        mockMvc.perform(get(USER_URI)
                        .param("role", Role.ADMIN.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(user1.getId()))
                .andExpect(jsonPath("$.content[0].email").value("test1@gmail.com"))
                .andExpect(jsonPath("$.content[0].firstName").value("test1"))
                .andExpect(jsonPath("$.content[0].lastName").value("user1"))
                .andExpect(jsonPath("$.content[0].phoneNumber").value("123456780"))
                .andExpect(jsonPath("$.content[0].role").value(Role.ADMIN.name()))
                .andExpect(jsonPath("$.content[0].createdAt").exists())
                .andExpect(jsonPath("$.content[0].updatedAt").exists())

                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.numberOfElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getUserById_withAdminRole_returnsUser() throws Exception {
        User user = createUser(
                "test@gmail.com",
                "test123456789",
                "test",
                "user",
                "123456789",
                Role.CUSTOMER
        );

        mockMvc.perform(get(USER_URI + "/" + user.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId()))
                .andExpect(jsonPath("$.email").value(user.getEmail()))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.firstName").value(user.getFirstName()))
                .andExpect(jsonPath("$.lastName").value(user.getLastName()))
                .andExpect(jsonPath("$.phoneNumber").value(user.getPhoneNumber()))
                .andExpect(jsonPath("$.role").value(user.getRole().name()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getUserById_whenUserNotFound_returnsNotFoundResponse() throws Exception {
        Long userId = 999L;

        mockMvc.perform(get(USER_URI + "/" + userId))
                .andExpect(status().isNotFound());
    }

    @Test
    void getUserById_whenSameCustomer_returnsUser() throws Exception {
        User user = createDefaultCustomer();

        MockAuthentication.authenticateUser(user.getId());

        mockMvc.perform(get(USER_URI + "/" + user.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(user.getEmail()))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.firstName").value(user.getFirstName()))
                .andExpect(jsonPath("$.lastName").value(user.getLastName()))
                .andExpect(jsonPath("$.phoneNumber").value(user.getPhoneNumber()))
                .andExpect(jsonPath("$.role").value(user.getRole().name()))
                .andExpect(jsonPath("$.id").value(user.getId()));
    }

    @Test
    void putUser_whenSameCustomer_updatesUser() throws Exception {
        User user = createDefaultCustomer();

        UserPutRequest request = createDefaultPutUserRequest();

        MockAuthentication.authenticateUser(user.getId());

        mockMvc.perform(put(USER_URI + "/" + user.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId()))
                .andExpect(jsonPath("$.email").value(request.email()))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.firstName").value(request.firstName()))
                .andExpect(jsonPath("$.lastName").value(request.lastName()))
                .andExpect(jsonPath("$.phoneNumber").value(request.phoneNumber()))
                .andExpect(jsonPath("$.role").value(user.getRole().name()));

        User updatedUser = userRepository.findById(user.getId()).orElseThrow();

        assertThat(updatedUser.getEmail()).isEqualTo(request.email());
        assertThat(updatedUser.getFirstName()).isEqualTo(request.firstName());
        assertThat(updatedUser.getLastName()).isEqualTo(request.lastName());
        assertThat(updatedUser.getPhoneNumber()).isEqualTo(request.phoneNumber());
        assertThat(updatedUser.getRole()).isEqualTo(Role.CUSTOMER);
        assertThat(passwordEncoder.matches(request.password(), updatedUser.getPasswordHash())).isTrue();
    }

    @Test
    void putUser_whenEmailAlreadyExists_returnsConflictAndKeepsUserUnchanged() throws Exception {
        User user = createDefaultCustomer();

        User otherUser = createUser(
                "other@gmail.com",
                "test123456789",
                "Other",
                "User",
                "1234567891",
                Role.CUSTOMER
        );

        String originalEmail = user.getEmail();

        UserPutRequest request = new UserPutRequest(
                otherUser.getEmail(),
                "test123456789",
                "Updated",
                "User",
                "1234567892"
        );

        MockAuthentication.authenticateUser(user.getId());

        mockMvc.perform(put(USER_URI + "/" + user.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());

        User savedUser = userRepository.findById(user.getId()).orElseThrow();

        assertThat(savedUser.getEmail()).isEqualTo(originalEmail);
    }

    @Test
    void putUser_whenUserNotFound_returnsNotFound() throws Exception {
        Long userId = 999_999L;

        UserPutRequest request = new UserPutRequest(
                "test@gmail.com",
                "test123456789",
                "Updated",
                "User",
                "1234567892"
        );

        MockAuthentication.authenticateUser(userId);

        mockMvc.perform(put(USER_URI + "/" + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void patchUser_whenSameCustomer_patchesUser() throws Exception {
        User user = createDefaultCustomer();

        UserPatchRequest request = createPatchUserRequest(
                null,
                null,
                "updatedFirstName",
                null,
                "1234567801"
        );

        MockAuthentication.authenticateUser(user.getId());

        mockMvc.perform(patch(USER_URI + "/" + user.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId()))
                .andExpect(jsonPath("$.email").value(user.getEmail()))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.firstName").value(request.firstName()))
                .andExpect(jsonPath("$.lastName").value(user.getLastName()))
                .andExpect(jsonPath("$.phoneNumber").value(request.phoneNumber()))
                .andExpect(jsonPath("$.role").value(user.getRole().name()));

        User updatedUser = userRepository.findById(user.getId()).orElseThrow();

        assertThat(updatedUser.getEmail()).isEqualTo(user.getEmail());
        assertThat(updatedUser.getFirstName()).isEqualTo(request.firstName());
        assertThat(updatedUser.getLastName()).isEqualTo(user.getLastName());
        assertThat(updatedUser.getPhoneNumber()).isEqualTo(request.phoneNumber());
        assertThat(updatedUser.getRole()).isEqualTo(Role.CUSTOMER);
        assertThat(updatedUser.getPasswordHash()).isEqualTo(user.getPasswordHash());
    }

    @Test
    void patchUser_whenPhoneNumberAlreadyExists_returnsConflictAndKeepsUserUnchanged()
            throws Exception {

        User user = createDefaultCustomer();

        User otherUser = createUser(
                "other@gmail.com",
                "test123456789",
                "Other",
                "User",
                "1234567891",
                Role.CUSTOMER
        );

        String originalPhoneNumber = user.getPhoneNumber();

        UserPatchRequest request = new UserPatchRequest(
                null,
                null,
                null,
                null,
                otherUser.getPhoneNumber()
        );

        MockAuthentication.authenticateUser(user.getId());

        mockMvc.perform(patch(USER_URI + "/" + user.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());

        User savedUser = userRepository.findById(user.getId()).orElseThrow();

        assertThat(savedUser.getPhoneNumber())
                .isEqualTo(originalPhoneNumber);
    }

    @Test
    void patchUser_whenUserNotFound_returnsNotFound() throws Exception {
        Long userId = 999_999L;

        UserPatchRequest request = new UserPatchRequest(
                "test@gmail.com",
                "test123456789",
                "Updated",
                "User",
                "1234567892"
        );

        MockAuthentication.authenticateUser(userId);

        mockMvc.perform(patch(USER_URI + "/" + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteUser_withAdminRole_deletesUser() throws Exception {
        User user = createDefaultCustomer();

        mockMvc.perform(delete(USER_URI + "/" + user.getId()))
                .andExpect(status().isNoContent());

        assertThat(userRepository.existsById(user.getId())).isFalse();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteUser_whenUserNotFound_returnsNotFound() throws Exception {
        Long userId = 999_999L;

        mockMvc.perform(delete(USER_URI + "/" + userId))
                .andExpect(status().isNotFound());
    }
}
