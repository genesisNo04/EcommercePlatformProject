package com.namnguyen.ecommerce_platform.integration.user;

import com.namnguyen.ecommerce_platform.integration.BaseIntegrationTest;
import com.namnguyen.ecommerce_platform.testutil.MockAuthentication;
import com.namnguyen.ecommerce_platform.user.dto.UserPutRequest;
import com.namnguyen.ecommerce_platform.user.entity.User;
import com.namnguyen.ecommerce_platform.user.enums.Role;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;


import static com.namnguyen.ecommerce_platform.testutil.TestDataFactory.*;
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
                .andExpect(jsonPath("$.content[1].updatedAt").exists());
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
                .andExpect(jsonPath("$.email").value(user.getEmail()))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.firstName").value(user.getFirstName()))
                .andExpect(jsonPath("$.lastName").value(user.getLastName()))
                .andExpect(jsonPath("$.phoneNumber").value(user.getPhoneNumber()))
                .andExpect(jsonPath("$.role").value(user.getRole().name()));
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
                .andExpect(jsonPath("$.role").value(user.getRole().name()));
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
                .andExpect(jsonPath("$.email").value(request.email()))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.firstName").value(request.firstName()))
                .andExpect(jsonPath("$.lastName").value(request.lastName()))
                .andExpect(jsonPath("$.phoneNumber").value(request.phoneNumber()))
                .andExpect(jsonPath("$.role").value(user.getRole().name()));
    }

    @Test
    void patchUser_whenSameCustomer_patchUser() throws Exception {
        User user = createDefaultCustomer();

        UserPutRequest request = createDefaultPutUserRequest();

        MockAuthentication.authenticateUser(user.getId());

        mockMvc.perform(put(USER_URI + "/" + user.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(request.email()))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.firstName").value(request.firstName()))
                .andExpect(jsonPath("$.lastName").value(request.lastName()))
                .andExpect(jsonPath("$.phoneNumber").value(request.phoneNumber()))
                .andExpect(jsonPath("$.role").value(user.getRole().name()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteUser_withAdminRole_deletesUser() throws Exception {
        User user = createDefaultCustomer();

        mockMvc.perform(delete(USER_URI + "/" + user.getId()))
                .andExpect(status().isNoContent());
    }
}
