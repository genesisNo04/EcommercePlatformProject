package com.namnguyen.ecommerce_platform.user.controller;

import com.namnguyen.ecommerce_platform.common.exception.NoResourceFoundException;
import com.namnguyen.ecommerce_platform.common.rate_limit.RateLimitService;
import com.namnguyen.ecommerce_platform.security.jwt.JwtService;
import com.namnguyen.ecommerce_platform.security.user.CustomUserDetailsService;
import com.namnguyen.ecommerce_platform.user.dto.UserFilterRequest;
import com.namnguyen.ecommerce_platform.user.dto.UserPatchRequest;
import com.namnguyen.ecommerce_platform.user.dto.UserPutRequest;
import com.namnguyen.ecommerce_platform.user.dto.UserResponse;
import com.namnguyen.ecommerce_platform.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.*;
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
import static org.hamcrest.Matchers.containsInAnyOrder;

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

    @MockitoBean
    private RateLimitService rateLimitService;

    @Test
    void getUserById_validUserId_returnsUserResponse() throws Exception {
        Long userId = 1L;

        UserResponse response = new UserResponse(
                userId,
                VALID_EMAIL,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                VALID_PHONE_NUMBER,
                ROLE,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(userService.getUserById(userId)).thenReturn(response);

        mockMvc.perform(get(USER_URI + "/" + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.email").value(VALID_EMAIL))
                .andExpect(jsonPath("$.firstName").value(VALID_FIRST_NAME))
                .andExpect(jsonPath("$.lastName").value(VALID_LAST_NAME))
                .andExpect(jsonPath("$.phoneNumber").value(VALID_PHONE_NUMBER))
                .andExpect(jsonPath("$.role").value(ROLE.name()))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());

        verify(userService).getUserById(userId);
        verifyNoMoreInteractions(userService);
    }

    @Test
    void getUserById_userNotFound_returnsNotFound() throws Exception {
        Long userId = 1L;

        when(userService.getUserById(userId))
                .thenThrow(new NoResourceFoundException(userNotFound(userId)));

        mockMvc.perform(get(USER_URI + "/" + userId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.NOT_FOUND.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(userNotFound(userId)))
                .andExpect(jsonPath("$.uri").value(USER_URI + "/" + userId));

        verify(userService).getUserById(userId);
        verifyNoMoreInteractions(userService);
    }

    @Test
    void getUserById_invalidUserId_returnsBadRequest() throws Exception {
        String userId = "testing";

        mockMvc.perform(get(USER_URI + "/" + userId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(invalidParameter("id")))
                .andExpect(jsonPath("$.uri").value(USER_URI + "/" + userId));

        verifyNoInteractions(userService);
    }

    @Test
    void getAllUsers_whenUsersExist_returnsPageUserResponse() throws Exception {
        Long userId = 1L;
        Long userId1 = 2L;

        UserResponse response = new UserResponse(
                userId,
                VALID_EMAIL,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                VALID_PHONE_NUMBER,
                ROLE,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        UserResponse response1 = new UserResponse(
                userId1,
                "test1@gmail.com",
                VALID_FIRST_NAME + "1",
                VALID_LAST_NAME + "1",
                "1234567892",
                ROLE,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        List<UserResponse> listResponse = List.of(response, response1);
        Pageable pageable = PageRequest.of(0, 10);
        Page<UserResponse> responses = new PageImpl<>(listResponse, pageable, listResponse.size());

        when(userService.getAllUsers(any(UserFilterRequest.class), any(Pageable.class))).thenReturn(responses);

        mockMvc.perform(get(USER_URI)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].id").value(userId))
                .andExpect(jsonPath("$.content[0].email").value(VALID_EMAIL))
                .andExpect(jsonPath("$.content[0].firstName").value(VALID_FIRST_NAME))
                .andExpect(jsonPath("$.content[0].lastName").value(VALID_LAST_NAME))
                .andExpect(jsonPath("$.content[0].phoneNumber").value(VALID_PHONE_NUMBER))
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

        assertThat(pageableCapture.getSort()).contains(Sort.Order.asc("id"));
        assertThat(pageableCapture.getPageNumber()).isEqualTo(0);
        assertThat(pageableCapture.getPageSize()).isEqualTo(10);

        verifyNoMoreInteractions(userService);
    }

    @Test
    void getAllUsers_whenUsersExistWithFilter_returnsPageUserResponse() throws Exception {
        Long userId = 1L;
        Long userId1 = 2L;

        UserResponse response = new UserResponse(
                userId,
                VALID_EMAIL,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                VALID_PHONE_NUMBER,
                ROLE,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        UserResponse response1 = new UserResponse(
                userId1,
                "test1@gmail.com",
                VALID_FIRST_NAME + "1",
                VALID_LAST_NAME + "1",
                "1234567892",
                ROLE,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        List<UserResponse> listResponse = List.of(response, response1);
        Pageable pageable = PageRequest.of(0, 10);
        Page<UserResponse> responses = new PageImpl<>(listResponse, pageable, listResponse.size());

        when(userService.getAllUsers(any(UserFilterRequest.class), any(Pageable.class))).thenReturn(responses);

        mockMvc.perform(get(USER_URI)
                        .param("page", "0")
                        .param("size", "10")
                        .param("email", VALID_EMAIL)
                        .param("keyword", VALID_FIRST_NAME)
                        .param("role", ROLE.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].id").value(userId))
                .andExpect(jsonPath("$.content[0].email").value(VALID_EMAIL))
                .andExpect(jsonPath("$.content[0].firstName").value(VALID_FIRST_NAME))
                .andExpect(jsonPath("$.content[0].lastName").value(VALID_LAST_NAME))
                .andExpect(jsonPath("$.content[0].phoneNumber").value(VALID_PHONE_NUMBER))
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

        assertThat(requestCapture.email()).isEqualTo(VALID_EMAIL);
        assertThat(requestCapture.keyword()).isEqualTo(VALID_FIRST_NAME);
        assertThat(requestCapture.role()).isEqualTo(ROLE);

        assertThat(pageableCapture.getPageNumber()).isEqualTo(0);
        assertThat(pageableCapture.getPageSize()).isEqualTo(10);
        assertThat(pageableCapture.getSort()).contains(Sort.Order.asc("id"));

        verifyNoMoreInteractions(userService);
    }

    @Test
    void getAllUsers_whenFilterRoleIsInvalid_returnsBadRequest() throws Exception {
        mockMvc.perform(get(USER_URI)
                        .param("page", "0")
                        .param("size", "10")
                        .param("email", VALID_EMAIL)
                        .param("keyword", VALID_FIRST_NAME)
                        .param("role", "BAD_ROLE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.uri").value(USER_URI))
                .andExpect(jsonPath("$.fieldErrors.role", containsInAnyOrder(invalidParameter("role"))));

        verifyNoInteractions(userService);
    }

    @Test
    void getAllUsers_listOfUsersEmpty_returnsPageUserResponse() throws Exception {
        List<UserResponse> listResponse = List.of();
        Pageable pageable = PageRequest.of(0, 10);
        Page<UserResponse> responses = new PageImpl<>(listResponse, pageable, listResponse.size());

        when(userService.getAllUsers(any(UserFilterRequest.class), any(Pageable.class))).thenReturn(responses);

        mockMvc.perform(get(USER_URI)
                        .param("page", "0")
                        .param("size", "10"))
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
        assertThat(pageableCapture.getSort()).contains(Sort.Order.asc("id"));

        verifyNoMoreInteractions(userService);
    }

    @Test
    void  putUser_whenUserExists_returnsUserResponse() throws Exception {
        Long userId = 1L;

        UserPutRequest request = new UserPutRequest(
                VALID_EMAIL,
                VALID_PASSWORD,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                VALID_PHONE_NUMBER);

        UserResponse response = new UserResponse(
                userId,
                VALID_EMAIL,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                VALID_PHONE_NUMBER,
                ROLE,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(userService.putUser(userId, request)).thenReturn(response);

        mockMvc.perform(put(USER_URI + "/" + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.email").value(VALID_EMAIL))
                .andExpect(jsonPath("$.firstName").value(VALID_FIRST_NAME))
                .andExpect(jsonPath("$.lastName").value(VALID_LAST_NAME))
                .andExpect(jsonPath("$.phoneNumber").value(VALID_PHONE_NUMBER))
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
    void  putUser_whenUserNotExists_returnsNotFound() throws Exception {
        Long userId = 1L;

        UserPutRequest request = new UserPutRequest(
                VALID_EMAIL,
                VALID_PASSWORD,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                VALID_PHONE_NUMBER);

        when(userService.putUser(userId, request))
                .thenThrow(new NoResourceFoundException(userNotFound(userId)));

        mockMvc.perform(put(USER_URI + "/" + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.NOT_FOUND.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(userNotFound(userId)))
                .andExpect(jsonPath("$.uri").value(USER_URI + "/" + userId));

        verify(userService).putUser(userId, request);
        verifyNoMoreInteractions(userService);
    }

    @Test
    void  putUser_whenUserIdIsInvalid_returnsBadRequest() throws Exception {
        String userId = "test";

        UserPutRequest request = new UserPutRequest(
                VALID_EMAIL,
                VALID_PASSWORD,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                VALID_PHONE_NUMBER);

        mockMvc.perform(put(USER_URI + "/" + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(invalidParameter("id")))
                .andExpect(jsonPath("$.uri").value(USER_URI + "/" + userId));

        verifyNoInteractions(userService);
    }

    @Test
    void  putUser_whenEmailIsEmpty_returnsBadRequest() throws Exception {
        Long userId = 1L;

        UserPutRequest request = new UserPutRequest(
                "",
                VALID_PASSWORD,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                VALID_PHONE_NUMBER);

        mockMvc.perform(put(USER_URI + "/" + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.uri").value(USER_URI + "/" + userId))
                .andExpect(jsonPath("$.fieldErrors.email", containsInAnyOrder(emailIsRequired())));

        verifyNoInteractions(userService);
    }

    @Test
    void  putUser_whenEmailIsNull_returnsBadRequest() throws Exception {
        Long userId = 1L;

        UserPutRequest request = new UserPutRequest(
                null,
                VALID_PASSWORD,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                VALID_PHONE_NUMBER);

        mockMvc.perform(put(USER_URI + "/" + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.uri").value(USER_URI + "/" + userId))
                .andExpect(jsonPath("$.fieldErrors.email", containsInAnyOrder(emailIsRequired())));

        verifyNoInteractions(userService);
    }

    @Test
    void  putUser_whenEmailIsInvalid_returnsBadRequest() throws Exception {
        Long userId = 1L;

        UserPutRequest request = new UserPutRequest(
                INVALID_EMAIL,
                VALID_PASSWORD,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                VALID_PHONE_NUMBER);

        mockMvc.perform(put(USER_URI + "/" + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.uri").value(USER_URI + "/" + userId))
                .andExpect(jsonPath("$.fieldErrors.email", containsInAnyOrder(emailIsInvalid())));

        verifyNoInteractions(userService);
    }

    @Test
    void  putUser_whenPasswordIsLessThanEight_returnsBadRequest() throws Exception {
        Long userId = 1L;

        UserPutRequest request = new UserPutRequest(
                VALID_EMAIL,
                INVALID_PASSWORD_LESS_THAN_EIGHT,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                VALID_PHONE_NUMBER);

        mockMvc.perform(put(USER_URI + "/" + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.uri").value(USER_URI + "/" + userId))
                .andExpect(jsonPath("$.fieldErrors.password", containsInAnyOrder(passwordIsInvalid())));

        verifyNoInteractions(userService);
    }

    @Test
    void  putUser_whenPasswordIsMoreThanFifty_returnsBadRequest() throws Exception {
        Long userId = 1L;

        UserPutRequest request = new UserPutRequest(
                VALID_EMAIL,
                INVALID_PASSWORD_MORE_THAN_FIFTY,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                VALID_PHONE_NUMBER);

        mockMvc.perform(put(USER_URI + "/" + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.uri").value(USER_URI + "/" + userId))
                .andExpect(jsonPath("$.fieldErrors.password", containsInAnyOrder(passwordIsInvalid())));

        verifyNoInteractions(userService);
    }

    @Test
    void  putUser_whenPasswordIsEmpty_returnsBadRequest() throws Exception {
        Long userId = 1L;

        UserPutRequest request = new UserPutRequest(
                VALID_EMAIL,
                "",
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                VALID_PHONE_NUMBER);

        mockMvc.perform(put(USER_URI + "/" + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.uri").value(USER_URI + "/" + userId))
                .andExpect(jsonPath("$.fieldErrors.password", containsInAnyOrder(
                        passwordIsInvalid(),
                        passwordIsRequired())));

        verifyNoInteractions(userService);
    }

    @Test
    void  putUser_whenPasswordIsNull_returnsBadRequest() throws Exception {
        Long userId = 1L;

        UserPutRequest request = new UserPutRequest(
                VALID_EMAIL,
                null,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                VALID_PHONE_NUMBER);

        mockMvc.perform(put(USER_URI + "/" + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.uri").value(USER_URI + "/" + userId))
                .andExpect(jsonPath("$.fieldErrors.password", containsInAnyOrder(passwordIsRequired())));

        verifyNoInteractions(userService);
    }

    @Test
    void  putUser_whenFirstNameIsEmpty_returnsBadRequest() throws Exception {
        Long userId = 1L;

        UserPutRequest request = new UserPutRequest(
                VALID_EMAIL,
                VALID_PASSWORD,
                "",
                VALID_LAST_NAME,
                VALID_PHONE_NUMBER);

        mockMvc.perform(put(USER_URI + "/" + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.uri").value(USER_URI + "/" + userId))
                .andExpect(jsonPath("$.fieldErrors.firstName", containsInAnyOrder(firstNameIsRequired())));

        verifyNoInteractions(userService);
    }

    @Test
    void  putUser_whenFirstNameIsNull_returnsBadRequest() throws Exception {
        Long userId = 1L;

        UserPutRequest request = new UserPutRequest(
                VALID_EMAIL,
                VALID_PASSWORD,
                null,
                VALID_LAST_NAME,
                VALID_PHONE_NUMBER);

        mockMvc.perform(put(USER_URI + "/" + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.uri").value(USER_URI + "/" + userId))
                .andExpect(jsonPath("$.fieldErrors.firstName", containsInAnyOrder(firstNameIsRequired())));

        verifyNoInteractions(userService);
    }

    @Test
    void  putUser_whenLastNameIsEmpty_returnsBadRequest() throws Exception {
        Long userId = 1L;

        UserPutRequest request = new UserPutRequest(
                VALID_EMAIL,
                VALID_PASSWORD,
                VALID_FIRST_NAME,
                "",
                VALID_PHONE_NUMBER);

        mockMvc.perform(put(USER_URI + "/" + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.uri").value(USER_URI + "/" + userId))
                .andExpect(jsonPath("$.fieldErrors.lastName", containsInAnyOrder(lastNameIsRequired())));

        verifyNoInteractions(userService);
    }

    @Test
    void  putUser_whenLastNameIsNull_returnsBadRequest() throws Exception {
        Long userId = 1L;

        UserPutRequest request = new UserPutRequest(
                VALID_EMAIL,
                VALID_PASSWORD,
                VALID_FIRST_NAME,
                null,
                VALID_PHONE_NUMBER);

        mockMvc.perform(put(USER_URI + "/" + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.uri").value(USER_URI + "/" + userId))
                .andExpect(jsonPath("$.fieldErrors.lastName", containsInAnyOrder(lastNameIsRequired())));

        verifyNoInteractions(userService);
    }

    @Test
    void  putUser_whenPhoneNumberIsEmpty_returnsBadRequest() throws Exception {
        Long userId = 1L;

        UserPutRequest request = new UserPutRequest(
                VALID_EMAIL,
                VALID_PASSWORD,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                "");

        mockMvc.perform(put(USER_URI + "/" + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.uri").value(USER_URI + "/" + userId))
                .andExpect(jsonPath("$.fieldErrors.phoneNumber", containsInAnyOrder(
                        phoneNumberIsRequired(),
                        phoneNumberIsInvalid())));

        verifyNoInteractions(userService);
    }

    @Test
    void  putUser_whenPhoneNumberIsNull_returnsBadRequest() throws Exception {
        Long userId = 1L;

        UserPutRequest request = new UserPutRequest(
                VALID_EMAIL,
                VALID_PASSWORD,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                null);

        mockMvc.perform(put(USER_URI + "/" + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.uri").value(USER_URI + "/" + userId))
                .andExpect(jsonPath("$.fieldErrors.phoneNumber", containsInAnyOrder(phoneNumberIsRequired())));

        verifyNoInteractions(userService);
    }

    @Test
    void  putUser_whenPhoneNumberIsLessThan10Digits_returnsBadRequest() throws Exception {
        Long userId = 1L;

        UserPutRequest request = new UserPutRequest(
                VALID_EMAIL,
                VALID_PASSWORD,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                INVALID_PHONE_NUMBER_LESS_THAN_TEN);

        mockMvc.perform(put(USER_URI + "/" + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.uri").value(USER_URI + "/" + userId))
                .andExpect(jsonPath("$.fieldErrors.phoneNumber", containsInAnyOrder(phoneNumberIsInvalid())));

        verifyNoInteractions(userService);
    }

    @Test
    void  putUser_whenPhoneNumberIsMoreThan15Digits_returnsBadRequest() throws Exception {
        Long userId = 1L;

        UserPutRequest request = new UserPutRequest(
                VALID_EMAIL,
                VALID_PASSWORD,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                INVALID_PHONE_NUMBER_MORE_THAN_FIFTEEN);

        mockMvc.perform(put(USER_URI + "/" + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.uri").value(USER_URI + "/" + userId))
                .andExpect(jsonPath("$.fieldErrors.phoneNumber", containsInAnyOrder(phoneNumberIsInvalid())));

        verifyNoInteractions(userService);
    }

    @Test
    void  putUser_whenPhoneNumberHasInvalidSymbol_returnsBadRequest() throws Exception {
        Long userId = 1L;

        UserPutRequest request = new UserPutRequest(
                VALID_EMAIL,
                VALID_PASSWORD,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                INVALID_PHONE_NUMBER_WITH_MINUS);

        mockMvc.perform(put(USER_URI + "/" + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.uri").value(USER_URI + "/" + userId))
                .andExpect(jsonPath("$.fieldErrors.phoneNumber", containsInAnyOrder(phoneNumberIsInvalid())));

        verifyNoInteractions(userService);
    }

    @Test
    void  patchUser_whenUserExists_returnsUserResponse() throws Exception {
        Long userId = 1L;

        UserPatchRequest request = new UserPatchRequest(
                VALID_EMAIL,
                VALID_PASSWORD,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                VALID_PHONE_NUMBER);

        UserResponse response = new UserResponse(
                userId,
                VALID_EMAIL,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                VALID_PHONE_NUMBER,
                ROLE,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(userService.patchUser(userId, request)).thenReturn(response);

        mockMvc.perform(patch(USER_URI + "/" + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.email").value(VALID_EMAIL))
                .andExpect(jsonPath("$.firstName").value(VALID_FIRST_NAME))
                .andExpect(jsonPath("$.lastName").value(VALID_LAST_NAME))
                .andExpect(jsonPath("$.phoneNumber").value(VALID_PHONE_NUMBER))
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
    void  patchUser_whenUserNotExists_returnsNotFound() throws Exception {
        Long userId = 1L;

        UserPatchRequest request = new UserPatchRequest(
                VALID_EMAIL,
                VALID_PASSWORD,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                VALID_PHONE_NUMBER);

        when(userService.patchUser(userId, request))
                .thenThrow(new NoResourceFoundException(userNotFound(userId)));

        mockMvc.perform(patch(USER_URI + "/" + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.NOT_FOUND.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(userNotFound(userId)))
                .andExpect(jsonPath("$.uri").value(USER_URI + "/" + userId));

        verify(userService).patchUser(userId, request);
        verifyNoMoreInteractions(userService);
    }

    @Test
    void  patchUser_whenEmailIsInvalid_returnsBadRequest() throws Exception {
        Long userId = 1L;

        UserPatchRequest request = new UserPatchRequest(
                INVALID_EMAIL,
                VALID_PASSWORD,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                VALID_PHONE_NUMBER);

        mockMvc.perform(patch(USER_URI + "/" + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.uri").value(USER_URI + "/" + userId))
                .andExpect(jsonPath("$.fieldErrors.email", containsInAnyOrder(emailIsInvalid())));

        verifyNoInteractions(userService);
    }

    @Test
    void  patchUser_whenEmailIsEmpty_returnsBadRequest() throws Exception {
        Long userId = 1L;

        UserPatchRequest request = new UserPatchRequest(
                "",
                VALID_PASSWORD,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                VALID_PHONE_NUMBER);

        mockMvc.perform(patch(USER_URI + "/" + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.uri").value(USER_URI + "/" + userId))
                .andExpect(jsonPath("$.fieldErrors.email", containsInAnyOrder(emailIsEmpty())));

        verifyNoInteractions(userService);
    }

    @Test
    void  patchUser_whenPasswordLessThan8Chars_returnsBadRequest() throws Exception {
        Long userId = 1L;

        UserPatchRequest request = new UserPatchRequest(
                VALID_EMAIL,
                INVALID_PASSWORD_LESS_THAN_EIGHT,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                VALID_PHONE_NUMBER);

        mockMvc.perform(patch(USER_URI + "/" + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.uri").value(USER_URI + "/" + userId))
                .andExpect(jsonPath("$.fieldErrors.password", containsInAnyOrder(passwordIsInvalid())));

        verifyNoInteractions(userService);
    }

    @Test
    void  patchUser_whenPasswordMoreThan50Chars_returnsBadRequest() throws Exception {
        Long userId = 1L;

        UserPatchRequest request = new UserPatchRequest(
                VALID_EMAIL,
                INVALID_PASSWORD_MORE_THAN_FIFTY,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                VALID_PHONE_NUMBER);

        mockMvc.perform(patch(USER_URI + "/" + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.uri").value(USER_URI + "/" + userId))
                .andExpect(jsonPath("$.fieldErrors.password", containsInAnyOrder(passwordIsInvalid())));

        verifyNoInteractions(userService);
    }

    @Test
    void  patchUser_whenPasswordIsEmpty_returnsBadRequest() throws Exception {
        Long userId = 1L;

        UserPatchRequest request = new UserPatchRequest(
                VALID_EMAIL,
                "",
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                VALID_PHONE_NUMBER);

        mockMvc.perform(patch(USER_URI + "/" + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.uri").value(USER_URI + "/" + userId))
                .andExpect(jsonPath("$.fieldErrors.password", containsInAnyOrder(
                        passwordIsEmpty(),
                        passwordIsInvalid())));

        verifyNoInteractions(userService);
    }

    @Test
    void  patchUser_whenFirstNameIsEmpty_returnsBadRequest() throws Exception {
        Long userId = 1L;

        UserPatchRequest request = new UserPatchRequest(
                VALID_EMAIL,
                VALID_PASSWORD,
                "",
                VALID_LAST_NAME,
                VALID_PHONE_NUMBER);

        mockMvc.perform(patch(USER_URI + "/" + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.uri").value(USER_URI + "/" + userId))
                .andExpect(jsonPath("$.fieldErrors.firstName", containsInAnyOrder(firstNameIsEmpty())));

        verifyNoInteractions(userService);
    }

    @Test
    void  patchUser_whenLastNameIsEmpty_returnsBadRequest() throws Exception {
        Long userId = 1L;

        UserPatchRequest request = new UserPatchRequest(
                VALID_EMAIL,
                VALID_PASSWORD,
                VALID_FIRST_NAME,
                "",
                VALID_PHONE_NUMBER);

        mockMvc.perform(patch(USER_URI + "/" + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.uri").value(USER_URI + "/" + userId))
                .andExpect(jsonPath("$.fieldErrors.lastName", containsInAnyOrder(lastNameIsEmpty())));

        verifyNoInteractions(userService);
    }

    @Test
    void  patchUser_whenPhoneNumberIsLessThan10Digits_returnsBadRequest() throws Exception {
        Long userId = 1L;

        UserPatchRequest request = new UserPatchRequest(
                VALID_EMAIL,
                VALID_PASSWORD,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                INVALID_PHONE_NUMBER_LESS_THAN_TEN);

        mockMvc.perform(patch(USER_URI + "/" + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.uri").value(USER_URI + "/" + userId))
                .andExpect(jsonPath("$.fieldErrors.phoneNumber", containsInAnyOrder(phoneNumberIsInvalid())));

        verifyNoInteractions(userService);
    }

    @Test
    void  patchUser_whenPhoneNumberMoreThan15_returnsBadRequest() throws Exception {
        Long userId = 1L;

        UserPatchRequest request = new UserPatchRequest(
                VALID_EMAIL,
                VALID_PASSWORD,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                INVALID_PHONE_NUMBER_MORE_THAN_FIFTEEN);

        mockMvc.perform(patch(USER_URI + "/" + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.uri").value(USER_URI + "/" + userId))
                .andExpect(jsonPath("$.fieldErrors.phoneNumber", containsInAnyOrder(phoneNumberIsInvalid())));

        verifyNoInteractions(userService);
    }

    @Test
    void  patchUser_whenPhoneNumberIsEmpty_returnsBadRequest() throws Exception {
        Long userId = 1L;

        UserPatchRequest request = new UserPatchRequest(
                VALID_EMAIL,
                VALID_PASSWORD,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                "");

        mockMvc.perform(patch(USER_URI + "/" + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.uri").value(USER_URI + "/" + userId))
                .andExpect(jsonPath("$.fieldErrors.phoneNumber", containsInAnyOrder(phoneNumberIsInvalid())));

        verifyNoInteractions(userService);
    }

    @Test
    void  patchUser_whenPhoneNumberHasInvalidSymbol_returnsBadRequest() throws Exception {
        Long userId = 1L;

        UserPatchRequest request = new UserPatchRequest(
                VALID_EMAIL,
                VALID_PASSWORD,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                INVALID_PHONE_NUMBER_WITH_MINUS);

        mockMvc.perform(patch(USER_URI + "/" + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(validationFailed()))
                .andExpect(jsonPath("$.uri").value(USER_URI + "/" + userId))
                .andExpect(jsonPath("$.fieldErrors.phoneNumber", containsInAnyOrder(phoneNumberIsInvalid())));

        verifyNoInteractions(userService);
    }

    @Test
    void  patchUser_whenInvalidUserId_returnsBadRequest() throws Exception {
        String userId = "testing";

        UserPatchRequest request = new UserPatchRequest(
                VALID_EMAIL,
                VALID_PASSWORD,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                VALID_PHONE_NUMBER);

        mockMvc.perform(patch(USER_URI + "/" + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(invalidParameter("id")))
                .andExpect(jsonPath("$.uri").value(USER_URI + "/" + userId));

        verifyNoInteractions(userService);
    }

    @Test
    void  patchUser_whenUserExists_partiallyPatch_returnsUserResponse() throws Exception {
        Long userId = 1L;

        UserPatchRequest request = new UserPatchRequest(
                VALID_EMAIL,
                VALID_PASSWORD,
                null,
                VALID_LAST_NAME,
                null);

        UserResponse response = new UserResponse(
                userId,
                VALID_EMAIL,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                VALID_PHONE_NUMBER,
                ROLE,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(userService.patchUser(userId, request)).thenReturn(response);

        mockMvc.perform(patch(USER_URI + "/" + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.email").value(VALID_EMAIL))
                .andExpect(jsonPath("$.firstName").value(VALID_FIRST_NAME))
                .andExpect(jsonPath("$.lastName").value(VALID_LAST_NAME))
                .andExpect(jsonPath("$.phoneNumber").value(VALID_PHONE_NUMBER))
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
    void  patchUser_whenAllFieldsAreNull_returnsUserResponse() throws Exception {
        Long userId = 1L;

        UserPatchRequest request = new UserPatchRequest(
                null,
                null,
                null,
                null,
                null);

        UserResponse response = new UserResponse(
                userId,
                VALID_EMAIL,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                VALID_PHONE_NUMBER,
                ROLE,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(userService.patchUser(userId, request)).thenReturn(response);

        mockMvc.perform(patch(USER_URI + "/" + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.email").value(VALID_EMAIL))
                .andExpect(jsonPath("$.firstName").value(VALID_FIRST_NAME))
                .andExpect(jsonPath("$.lastName").value(VALID_LAST_NAME))
                .andExpect(jsonPath("$.phoneNumber").value(VALID_PHONE_NUMBER))
                .andExpect(jsonPath("$.role").value(ROLE.name()))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());

        ArgumentCaptor<UserPatchRequest> captor = ArgumentCaptor.forClass(UserPatchRequest.class);
        verify(userService).patchUser(eq(userId), captor.capture());

        UserPatchRequest userPatchRequest = captor.getValue();

        assertThat(userPatchRequest.email()).isNull();
        assertThat(userPatchRequest.password()).isNull();
        assertThat(userPatchRequest.firstName()).isNull();
        assertThat(userPatchRequest.lastName()).isNull();
        assertThat(userPatchRequest.phoneNumber()).isNull();

        verifyNoMoreInteractions(userService);
    }

    @Test
    void deleteUser_whenUserExists_returnsNoContent() throws Exception {
        Long userId = 1L;

        mockMvc.perform(delete(USER_URI + "/" + userId))
                .andExpect(status().isNoContent());

        verify(userService).deleteUser(userId);
        verifyNoMoreInteractions(userService);
    }

    @Test
    void deleteUser_whenUserNotExists_returnsNotFound() throws Exception {
        Long userId = 1L;

        doThrow(new NoResourceFoundException(userNotFound(userId)))
                .when(userService).deleteUser(userId);

        mockMvc.perform(delete(USER_URI + "/" + userId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.NOT_FOUND.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(userNotFound(userId)))
                .andExpect(jsonPath("$.uri").value(USER_URI + "/" + userId));

        verify(userService).deleteUser(userId);
        verifyNoMoreInteractions(userService);
    }

    @Test
    void deleteUser_whenUserIdIsInvalid_returnsBadRequest() throws Exception {
        String userId = "test";

        mockMvc.perform(delete(USER_URI + "/" + userId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(invalidParameter("id")))
                .andExpect(jsonPath("$.uri").value(USER_URI + "/" + userId));

        verifyNoInteractions(userService);
    }
}
