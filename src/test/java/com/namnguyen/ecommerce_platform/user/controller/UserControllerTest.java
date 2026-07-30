package com.namnguyen.ecommerce_platform.user.controller;

import com.namnguyen.ecommerce_platform.common.exception.NoResourceFoundException;
import com.namnguyen.ecommerce_platform.security.jwt.JwtService;
import com.namnguyen.ecommerce_platform.security.user.CustomUserDetailsService;
import com.namnguyen.ecommerce_platform.user.dto.UserFilterRequest;
import com.namnguyen.ecommerce_platform.user.dto.UserPatchRequest;
import com.namnguyen.ecommerce_platform.user.dto.UserPutRequest;
import com.namnguyen.ecommerce_platform.user.dto.UserResponse;
import com.namnguyen.ecommerce_platform.user.entity.User;
import com.namnguyen.ecommerce_platform.user.enums.Role;
import com.namnguyen.ecommerce_platform.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;

import static com.namnguyen.ecommerce_platform.testutil.TestDataFactory.*;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static com.namnguyen.ecommerce_platform.testutil.TestMessages.*;
import static org.assertj.core.api.Assertions.*;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private final static String USERS_URI = "/api/users";
    private final static String EMAIL = "test@gmail.com";
    private final static String PASSWORD = "12345678";
    private final static String FIRST_NAME = "test";
    private final static String LAST_NAME = "user";
    private final static String PHONE_NUMBER = "1234567891";
    private final static Role ROLE = Role.CUSTOMER;

    @Test
    void getUserById_validUserId_returnUserResponse() throws Exception {
        Long userId = 1L;

        UserResponse response = new UserResponse(
                userId,
                EMAIL,
                FIRST_NAME,
                LAST_NAME,
                PHONE_NUMBER,
                ROLE,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(userService.getUserById(userId)).thenReturn(response);

        mockMvc.perform(get(USERS_URI + "/" + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.firstName").value(FIRST_NAME))
                .andExpect(jsonPath("$.lastName").value(LAST_NAME))
                .andExpect(jsonPath("$.phoneNumber").value(PHONE_NUMBER))
                .andExpect(jsonPath("$.role").value(ROLE.name()))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());

        verify(userService).getUserById(userId);
        verifyNoMoreInteractions(userService);
    }

    @Test
    void getUserById_invalidUserId_returnUserResponse() throws Exception {
        Long userId = 1L;

        when(userService.getUserById(userId))
                .thenThrow(new NoResourceFoundException(userNotFound(userId)));

        mockMvc.perform(get(USERS_URI + "/" + userId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.NOT_FOUND.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(userNotFound(userId)))
                .andExpect(jsonPath("$.uri").value(USERS_URI + "/" + userId));

        verify(userService).getUserById(userId);
        verifyNoMoreInteractions(userService);
    }

    @Test
    void getAllUsers_listOfUsersExists_returnPageUserResponse() throws Exception {
        Long userId = 1L;
        Long userId1 = 2L;

        UserResponse response = new UserResponse(
                userId,
                EMAIL,
                FIRST_NAME,
                LAST_NAME,
                PHONE_NUMBER,
                ROLE,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        UserResponse response1 = new UserResponse(
                userId1,
                "test1@gmail.com",
                FIRST_NAME + "1",
                LAST_NAME + "1",
                "1234567892",
                ROLE,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        List<UserResponse> listResponse = List.of(response, response1);
        Pageable pageable = PageRequest.of(0, 10);
        Page<UserResponse> responses = new PageImpl<>(listResponse, pageable, listResponse.size());

        when(userService.getAllUsers(any(UserFilterRequest.class), any(Pageable.class))).thenReturn(responses);

        mockMvc.perform(get(USERS_URI)
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].id").value(userId))
                .andExpect(jsonPath("$.content[0].email").value(EMAIL))
                .andExpect(jsonPath("$.content[0].firstName").value(FIRST_NAME))
                .andExpect(jsonPath("$.content[0].lastName").value(LAST_NAME))
                .andExpect(jsonPath("$.content[0].phoneNumber").value(PHONE_NUMBER))
                .andExpect(jsonPath("$.content[0].role").value(ROLE.name()))
                .andExpect(jsonPath("$.content[0].createdAt").exists())
                .andExpect(jsonPath("$.content[0].updatedAt").exists())
                .andExpect(jsonPath("$.content[1].id").value(userId1))
                .andExpect(jsonPath("$.content[1].email").value(response1.email()))
                .andExpect(jsonPath("$.content[1].firstName").value(response1.firstName()))
                .andExpect(jsonPath("$.content[1].lastName").value(response1.lastName()))
                .andExpect(jsonPath("$.content[1].phoneNumber").value(response1.phoneNumber()))
                .andExpect(jsonPath("$.content[1].role").value(response1.role().name()))
                .andExpect(jsonPath("$.content[1].createdAt").exists())
                .andExpect(jsonPath("$.content[1].updatedAt").exists())
                .andExpect(jsonPath("$.numberOfElements").value(2))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1));

        ArgumentCaptor<UserFilterRequest> captorFilter = ArgumentCaptor.forClass(UserFilterRequest.class);
        ArgumentCaptor<Pageable> captorPageable = ArgumentCaptor.forClass(Pageable.class);
        verify(userService).getAllUsers(captorFilter.capture(), captorPageable.capture());


        UserFilterRequest requestCapture = captorFilter.getValue();
        Pageable pageableCapture = captorPageable.getValue();

        assertThat(requestCapture.email()).isNull();
        assertThat(requestCapture.keyword()).isNull();
        assertThat(requestCapture.role()).isNull();

        assertThat(pageableCapture.getPageNumber()).isEqualTo(0);
        assertThat(pageableCapture.getPageSize()).isEqualTo(10);

        verifyNoMoreInteractions(userService);
    }

    @Test
    void getAllUsers_listOfUsersEmpty_returnPageUserResponse() throws Exception {
        List<UserResponse> listResponse = List.of();
        Pageable pageable = PageRequest.of(0, 10);
        Page<UserResponse> responses = new PageImpl<>(listResponse, pageable, listResponse.size());

        when(userService.getAllUsers(any(UserFilterRequest.class), any(Pageable.class))).thenReturn(responses);

        mockMvc.perform(get(USERS_URI)
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.numberOfElements").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0));

        ArgumentCaptor<UserFilterRequest> captorFilter = ArgumentCaptor.forClass(UserFilterRequest.class);
        ArgumentCaptor<Pageable> captorPageable = ArgumentCaptor.forClass(Pageable.class);
        verify(userService).getAllUsers(captorFilter.capture(), captorPageable.capture());

        UserFilterRequest requestCapture = captorFilter.getValue();
        Pageable pageableCapture = captorPageable.getValue();

        assertThat(requestCapture.email()).isNull();
        assertThat(requestCapture.keyword()).isNull();
        assertThat(requestCapture.role()).isNull();

        assertThat(pageableCapture.getPageNumber()).isEqualTo(0);
        assertThat(pageableCapture.getPageSize()).isEqualTo(10);

        verifyNoMoreInteractions(userService);
    }

    @Test
    void  putUser_whenUserExists_returnUserResponse() throws Exception {
        Long userId = 1L;

        UserPutRequest request = new UserPutRequest(
                EMAIL,
                PASSWORD,
                FIRST_NAME,
                LAST_NAME,
                PHONE_NUMBER);

        UserResponse response = new UserResponse(
                userId,
                EMAIL,
                FIRST_NAME,
                LAST_NAME,
                PHONE_NUMBER,
                ROLE,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(userService.putUser(userId, request)).thenReturn(response);

        mockMvc.perform(put(USERS_URI + "/" + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.firstName").value(FIRST_NAME))
                .andExpect(jsonPath("$.lastName").value(LAST_NAME))
                .andExpect(jsonPath("$.phoneNumber").value(PHONE_NUMBER))
                .andExpect(jsonPath("$.role").value(ROLE.name()))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());

        ArgumentCaptor<UserPutRequest> captor = ArgumentCaptor.forClass(UserPutRequest.class);
        verify(userService).putUser(eq(userId), captor.capture());

        UserPutRequest userPutRequest = captor.getValue();

        assertThat(userPutRequest.email()).isEqualTo(request.email());
        assertThat(userPutRequest.password()).isEqualTo(request.password());
        assertThat(userPutRequest.firstName()).isEqualTo(request.firstName());
        assertThat(userPutRequest.lastName()).isEqualTo(request.lastName());
        assertThat(userPutRequest.phoneNumber()).isEqualTo(request.phoneNumber());

        verifyNoMoreInteractions(userService);
    }

    @Test
    void  putUser_whenUserNotExists_returnUserResponse() throws Exception {
        Long userId = 1L;

        UserPutRequest request = new UserPutRequest(
                EMAIL,
                PASSWORD,
                FIRST_NAME,
                LAST_NAME,
                PHONE_NUMBER);

        when(userService.putUser(userId, request))
                .thenThrow(new NoResourceFoundException(userNotFound(userId)));

        mockMvc.perform(put(USERS_URI + "/" + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.NOT_FOUND.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(userNotFound(userId)))
                .andExpect(jsonPath("$.uri").value(USERS_URI + "/" + userId));

        verify(userService).putUser(userId, request);
        verifyNoMoreInteractions(userService);
    }

    @Test
    void  patchUser_whenUserExists_returnUserResponse() throws Exception {
        Long userId = 1L;

        UserPatchRequest request = new UserPatchRequest(
                EMAIL,
                PASSWORD,
                FIRST_NAME,
                LAST_NAME,
                PHONE_NUMBER);

        UserResponse response = new UserResponse(
                userId,
                EMAIL,
                FIRST_NAME,
                LAST_NAME,
                PHONE_NUMBER,
                ROLE,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(userService.patchUser(userId, request)).thenReturn(response);

        mockMvc.perform(patch(USERS_URI + "/" + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.firstName").value(FIRST_NAME))
                .andExpect(jsonPath("$.lastName").value(LAST_NAME))
                .andExpect(jsonPath("$.phoneNumber").value(PHONE_NUMBER))
                .andExpect(jsonPath("$.role").value(ROLE.name()))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());

        ArgumentCaptor<UserPatchRequest> captor = ArgumentCaptor.forClass(UserPatchRequest.class);
        verify(userService).patchUser(eq(userId), captor.capture());

        UserPatchRequest userPutRequest = captor.getValue();

        assertThat(userPutRequest.email()).isEqualTo(request.email());
        assertThat(userPutRequest.password()).isEqualTo(request.password());
        assertThat(userPutRequest.firstName()).isEqualTo(request.firstName());
        assertThat(userPutRequest.lastName()).isEqualTo(request.lastName());
        assertThat(userPutRequest.phoneNumber()).isEqualTo(request.phoneNumber());

        verifyNoMoreInteractions(userService);
    }

    @Test
    void  patchUser_whenUserNotExists_returnUserResponse() throws Exception {
        Long userId = 1L;

        UserPatchRequest request = new UserPatchRequest(
                EMAIL,
                PASSWORD,
                FIRST_NAME,
                LAST_NAME,
                PHONE_NUMBER);

        when(userService.patchUser(userId, request))
                .thenThrow(new NoResourceFoundException(userNotFound(userId)));

        mockMvc.perform(patch(USERS_URI + "/" + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.NOT_FOUND.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(userNotFound(userId)))
                .andExpect(jsonPath("$.uri").value(USERS_URI + "/" + userId));

        verify(userService).patchUser(userId, request);
        verifyNoMoreInteractions(userService);
    }

    @Test
    void  patchUser_whenUserExists_partiallyPatch_returnUserResponse() throws Exception {
        Long userId = 1L;

        UserPatchRequest request = new UserPatchRequest(
                EMAIL,
                PASSWORD,
                null,
                LAST_NAME,
                null);

        UserResponse response = new UserResponse(
                userId,
                EMAIL,
                FIRST_NAME,
                LAST_NAME,
                PHONE_NUMBER,
                ROLE,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(userService.patchUser(userId, request)).thenReturn(response);

        mockMvc.perform(patch(USERS_URI + "/" + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.firstName").value(FIRST_NAME))
                .andExpect(jsonPath("$.lastName").value(LAST_NAME))
                .andExpect(jsonPath("$.phoneNumber").value(PHONE_NUMBER))
                .andExpect(jsonPath("$.role").value(ROLE.name()))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());

        ArgumentCaptor<UserPatchRequest> captor = ArgumentCaptor.forClass(UserPatchRequest.class);
        verify(userService).patchUser(eq(userId), captor.capture());

        UserPatchRequest userPutRequest = captor.getValue();

        assertThat(userPutRequest.email()).isEqualTo(request.email());
        assertThat(userPutRequest.password()).isEqualTo(request.password());
        assertThat(userPutRequest.firstName()).isEqualTo(request.firstName());
        assertThat(userPutRequest.lastName()).isEqualTo(request.lastName());
        assertThat(userPutRequest.phoneNumber()).isEqualTo(request.phoneNumber());

        verifyNoMoreInteractions(userService);
    }

    @Test
    void deleteUser_whenUserExists_returnNoContent() throws Exception {
        Long userId = 1L;

        mockMvc.perform(delete(USERS_URI + "/" + userId))
                .andExpect(status().isNoContent());

        verify(userService).deleteUser(userId);
        verifyNoMoreInteractions(userService);
    }

    @Test
    void deleteUser_whenUserNotExists_returnNoContent() throws Exception {
        Long userId = 1L;

        doThrow(new NoResourceFoundException(userNotFound(userId)))
                .when(userService).deleteUser(userId);

        mockMvc.perform(delete(USERS_URI + "/" + userId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.NOT_FOUND.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(userNotFound(userId)))
                .andExpect(jsonPath("$.uri").value(USERS_URI + "/" + userId));

        verify(userService).deleteUser(userId);
        verifyNoMoreInteractions(userService);
    }
}
