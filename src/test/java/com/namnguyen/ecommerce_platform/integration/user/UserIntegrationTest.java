package com.namnguyen.ecommerce_platform.integration.user;

import com.namnguyen.ecommerce_platform.integration.BaseIntegrationTest;
import com.namnguyen.ecommerce_platform.user.dto.UserPatchRequest;
import com.namnguyen.ecommerce_platform.user.dto.UserPutRequest;
import com.namnguyen.ecommerce_platform.user.entity.User;
import com.namnguyen.ecommerce_platform.user.enums.Role;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static com.namnguyen.ecommerce_platform.testutil.MockAuthentication.authenticateUser;
import static com.namnguyen.ecommerce_platform.testutil.TestDataFactory.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class UserIntegrationTest extends BaseIntegrationTest {

    @Test
    void getAllUsers_withAdminRole_returnsPageOfUsers() throws Exception {
        User adminUser = persistDefaultAdmin();
        
        User firstUser = persistUser(
                "firstuser@gmail.com",
                VALID_PASSWORD,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                "4561237891",
                Role.CUSTOMER
        );

        User secondUser = persistUser(
                "seconduser@gmail.com",
                VALID_PASSWORD,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                "9876543212",
                Role.ADMIN
        );

        authenticateUser(adminUser.getId());

        mockMvc.perform(get(USER_URI))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.content[0].id").value(adminUser.getId()))
                .andExpect(jsonPath("$.content[0].email").value(adminUser.getEmail()))
                .andExpect(jsonPath("$.content[0].firstName").value(adminUser.getFirstName()))
                .andExpect(jsonPath("$.content[0].lastName").value(adminUser.getLastName()))
                .andExpect(jsonPath("$.content[0].phoneNumber").value(adminUser.getPhoneNumber()))
                .andExpect(jsonPath("$.content[0].role").value(Role.ADMIN.name()))
                .andExpect(jsonPath("$.content[0].createdAt").exists())
                .andExpect(jsonPath("$.content[0].updatedAt").exists())

                .andExpect(jsonPath("$.content[1].id").value(firstUser.getId()))
                .andExpect(jsonPath("$.content[1].email").value(firstUser.getEmail()))
                .andExpect(jsonPath("$.content[1].firstName").value(firstUser.getFirstName()))
                .andExpect(jsonPath("$.content[1].lastName").value(firstUser.getLastName()))
                .andExpect(jsonPath("$.content[1].phoneNumber").value(firstUser.getPhoneNumber()))
                .andExpect(jsonPath("$.content[1].role").value(Role.CUSTOMER.name()))
                .andExpect(jsonPath("$.content[1].createdAt").exists())
                .andExpect(jsonPath("$.content[1].updatedAt").exists())

                .andExpect(jsonPath("$.content[2].id").value(secondUser.getId()))
                .andExpect(jsonPath("$.content[2].email").value(secondUser.getEmail()))
                .andExpect(jsonPath("$.content[2].firstName").value(secondUser.getFirstName()))
                .andExpect(jsonPath("$.content[2].lastName").value(secondUser.getLastName()))
                .andExpect(jsonPath("$.content[2].phoneNumber").value(secondUser.getPhoneNumber()))
                .andExpect(jsonPath("$.content[2].role").value(Role.ADMIN.name()))
                .andExpect(jsonPath("$.content[2].createdAt").exists())
                .andExpect(jsonPath("$.content[2].updatedAt").exists())

                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.numberOfElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void getAllUsers_withFilters_returnsPageOfUsers() throws Exception {
        User adminUser = persistDefaultAdmin();

        persistUser(
                "firstuser@gmail.com",
                VALID_PASSWORD,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                "4561237891",
                Role.CUSTOMER
        );

        User secondUser = persistUser(
                "seconduser@gmail.com",
                VALID_PASSWORD,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                "9876543212",
                Role.ADMIN
        );

        authenticateUser(adminUser.getId());

        mockMvc.perform(get(USER_URI)
                        .param("role", Role.ADMIN.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].id").value(adminUser.getId()))
                .andExpect(jsonPath("$.content[0].email").value(adminUser.getEmail()))
                .andExpect(jsonPath("$.content[0].firstName").value(adminUser.getFirstName()))
                .andExpect(jsonPath("$.content[0].lastName").value(adminUser.getLastName()))
                .andExpect(jsonPath("$.content[0].phoneNumber").value(adminUser.getPhoneNumber()))
                .andExpect(jsonPath("$.content[0].role").value(Role.ADMIN.name()))
                .andExpect(jsonPath("$.content[0].createdAt").exists())
                .andExpect(jsonPath("$.content[0].updatedAt").exists())

                .andExpect(jsonPath("$.content[1].id").value(secondUser.getId()))
                .andExpect(jsonPath("$.content[1].email").value(secondUser.getEmail()))
                .andExpect(jsonPath("$.content[1].firstName").value(secondUser.getFirstName()))
                .andExpect(jsonPath("$.content[1].lastName").value(secondUser.getLastName()))
                .andExpect(jsonPath("$.content[1].phoneNumber").value(secondUser.getPhoneNumber()))
                .andExpect(jsonPath("$.content[1].role").value(Role.ADMIN.name()))
                .andExpect(jsonPath("$.content[1].createdAt").exists())
                .andExpect(jsonPath("$.content[1].updatedAt").exists())

                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.numberOfElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void getUserById_withAdminRole_returnsUser() throws Exception {
        User adminUser = persistDefaultAdmin();

        User user = persistUser(
                VALID_EMAIL,
                VALID_PASSWORD,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                VALID_UPDATE_PHONE_NUMBER,
                Role.CUSTOMER
        );

        authenticateUser(adminUser.getId());

        mockMvc.perform(get(userUri(user.getId())))
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
    void getUserById_whenUserNotFound_returnsNotFound() throws Exception {
        User adminUser = persistDefaultAdmin();

        Long userId = 999L;

        authenticateUser(adminUser.getId());

        mockMvc.perform(get(userUri(userId)))
                .andExpect(status().isNotFound());
    }

    @Test
    void getUserById_whenSameCustomer_returnsUser() throws Exception {
        User user = persistDefaultCustomer();

        authenticateUser(user.getId());

        mockMvc.perform(get(userUri(user.getId())))
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
        User user = persistDefaultCustomer();

        UserPutRequest userPutRequest = createDefaultUserPutRequest();

        authenticateUser(user.getId());

        mockMvc.perform(put(userUri(user.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userPutRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId()))
                .andExpect(jsonPath("$.email").value(userPutRequest.email()))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.firstName").value(userPutRequest.firstName()))
                .andExpect(jsonPath("$.lastName").value(userPutRequest.lastName()))
                .andExpect(jsonPath("$.phoneNumber").value(userPutRequest.phoneNumber()))
                .andExpect(jsonPath("$.role").value(user.getRole().name()));

        User updatedUser = userRepository.findById(user.getId()).orElseThrow();

        assertThat(updatedUser.getEmail()).isEqualTo(userPutRequest.email());
        assertThat(updatedUser.getFirstName()).isEqualTo(userPutRequest.firstName());
        assertThat(updatedUser.getLastName()).isEqualTo(userPutRequest.lastName());
        assertThat(updatedUser.getPhoneNumber()).isEqualTo(userPutRequest.phoneNumber());
        assertThat(updatedUser.getRole()).isEqualTo(Role.CUSTOMER);
        assertThat(passwordEncoder.matches(userPutRequest.password(), updatedUser.getPasswordHash())).isTrue();
    }

    @Test
    void putUser_whenEmailAlreadyExists_returnsConflictAndKeepsUserUnchanged() throws Exception {
        User user = persistDefaultCustomer();

        User otherUser = persistUser(
                "other@gmail.com",
                VALID_PASSWORD,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                "9876543212",
                Role.CUSTOMER
        );

        String originalEmail = user.getEmail();
        String originalPasswordHash = user.getPasswordHash();
        String originalFirstName = user.getFirstName();
        String originalLastName = user.getLastName();
        String originalPhoneNumber = user.getPhoneNumber();

        UserPutRequest userPutRequest = createUserPutRequest(
                otherUser.getEmail(),
                VALID_UPDATE_PASSWORD,
                VALID_UPDATE_FIRST_NAME,
                VALID_UPDATE_LAST_NAME,
                VALID_UPDATE_PHONE_NUMBER
        );

        authenticateUser(user.getId());

        mockMvc.perform(put(userUri(user.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userPutRequest)))
                .andExpect(status().isConflict());

        User savedUser = userRepository.findById(user.getId()).orElseThrow();

        assertThat(savedUser.getEmail()).isEqualTo(originalEmail);
        assertThat(savedUser.getPasswordHash()).isEqualTo(originalPasswordHash);
        assertThat(savedUser.getFirstName()).isEqualTo(originalFirstName);
        assertThat(savedUser.getLastName()).isEqualTo(originalLastName);
        assertThat(savedUser.getPhoneNumber()).isEqualTo(originalPhoneNumber);
    }

    @Test
    void putUser_whenUserNotFound_returnsNotFound() throws Exception {
        User adminUser = persistDefaultAdmin();
        Long userId = 999_999L;

        UserPutRequest userPutRequest = createDefaultUserPutRequest();

        authenticateUser(adminUser.getId());

        mockMvc.perform(put(userUri(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userPutRequest)))
                .andExpect(status().isNotFound());
    }

    @Test
    void patchUser_whenSameCustomer_patchesUser() throws Exception {
        User user = persistDefaultCustomer();

        UserPatchRequest userPatchRequest = createUserPatchRequest(
                null,
                null,
                VALID_UPDATE_FIRST_NAME,
                null,
                VALID_UPDATE_PHONE_NUMBER
        );

        String originalEmail = user.getEmail();
        String originalLastName = user.getLastName();
        String originalPasswordHash = user.getPasswordHash();

        authenticateUser(user.getId());

        mockMvc.perform(patch(userUri(user.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userPatchRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId()))
                .andExpect(jsonPath("$.email").value(originalEmail))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.firstName").value(userPatchRequest.firstName()))
                .andExpect(jsonPath("$.lastName").value(originalLastName))
                .andExpect(jsonPath("$.phoneNumber").value(userPatchRequest.phoneNumber()))
                .andExpect(jsonPath("$.role").value(user.getRole().name()));

        User updatedUser = userRepository.findById(user.getId()).orElseThrow();

        assertThat(updatedUser.getEmail()).isEqualTo(originalEmail);
        assertThat(updatedUser.getFirstName()).isEqualTo(userPatchRequest.firstName());
        assertThat(updatedUser.getLastName()).isEqualTo(originalLastName);
        assertThat(updatedUser.getPhoneNumber()).isEqualTo(userPatchRequest.phoneNumber());
        assertThat(updatedUser.getRole()).isEqualTo(Role.CUSTOMER);
        assertThat(updatedUser.getPasswordHash()).isEqualTo(originalPasswordHash);
    }

    @Test
    void patchUser_whenPhoneNumberAlreadyExists_returnsConflictAndKeepsUserUnchanged()
            throws Exception {

        persistDefaultCustomer();

        User otherUser = persistUser(
                "other@gmail.com",
                VALID_PASSWORD,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                "9876543212",
                Role.CUSTOMER
        );

        String originalPhoneNumber = otherUser.getPhoneNumber();

        UserPatchRequest userPatchRequest = createUserPatchRequest(
                null,
                null,
                null,
                null,
                VALID_PHONE_NUMBER
        );

        authenticateUser(otherUser.getId());

        mockMvc.perform(patch(userUri(otherUser.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userPatchRequest)))
                .andExpect(status().isConflict());

        User savedUser = userRepository.findById(otherUser.getId()).orElseThrow();

        assertThat(savedUser.getPhoneNumber())
                .isEqualTo(originalPhoneNumber);
    }

    @Test
    void patchUser_whenUserNotFound_returnsNotFound() throws Exception {
        User adminUser = persistDefaultAdmin();
        Long userId = 999_999L;

        UserPatchRequest userPatchRequest = createDefaultUserPatchRequest();

        authenticateUser(adminUser.getId());

        mockMvc.perform(patch(userUri(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userPatchRequest)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteUser_withAdminRole_deletesUser() throws Exception {
        User adminUser = persistDefaultAdmin();
        User user = persistDefaultCustomer();

        authenticateUser(adminUser.getId());

        mockMvc.perform(delete(userUri(user.getId())))
                .andExpect(status().isNoContent());

        assertThat(userRepository.existsById(user.getId())).isFalse();
    }

    @Test
    void deleteUser_whenUserNotFound_returnsNotFound() throws Exception {
        User adminUser = persistDefaultAdmin();
        Long userId = 999_999L;
        
        authenticateUser(adminUser.getId());

        mockMvc.perform(delete(userUri(userId)))
                .andExpect(status().isNotFound());
    }
}
