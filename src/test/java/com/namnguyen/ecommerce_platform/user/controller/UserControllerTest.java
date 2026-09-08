package com.namnguyen.ecommerce_platform.user.controller;

import com.namnguyen.ecommerce_platform.common.exception.DuplicateResourceException;
import com.namnguyen.ecommerce_platform.common.exception.NoResourceFoundException;
import com.namnguyen.ecommerce_platform.common.rate_limit.RateLimitService;
import com.namnguyen.ecommerce_platform.security.jwt.JwtService;
import com.namnguyen.ecommerce_platform.security.user.CustomUserDetailsService;
import com.namnguyen.ecommerce_platform.user.dto.UserFilterRequest;
import com.namnguyen.ecommerce_platform.user.dto.UserPatchRequest;
import com.namnguyen.ecommerce_platform.user.dto.UserPutRequest;
import com.namnguyen.ecommerce_platform.user.dto.UserResponse;
import com.namnguyen.ecommerce_platform.user.enums.Role;
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
import static com.namnguyen.ecommerce_platform.testutil.messages.CommonTestMessages.VALIDATION_FAILED;
import static com.namnguyen.ecommerce_platform.testutil.messages.CommonTestMessages.invalidParameter;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static com.namnguyen.ecommerce_platform.testutil.messages.UserTestMessages.*;
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
    void getUserById_whenUserExists_returnsUserResponse() throws Exception {
        Long userId = 1L;

        UserResponse userResponse = new UserResponse(
                userId,
                VALID_EMAIL,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                VALID_PHONE_NUMBER,
                Role.CUSTOMER,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(userService.getUserById(userId)).thenReturn(userResponse);

        mockMvc.perform(get(userUri(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.email").value(VALID_EMAIL))
                .andExpect(jsonPath("$.firstName").value(VALID_FIRST_NAME))
                .andExpect(jsonPath("$.lastName").value(VALID_LAST_NAME))
                .andExpect(jsonPath("$.phoneNumber").value(VALID_PHONE_NUMBER))
                .andExpect(jsonPath("$.role").value(Role.CUSTOMER.name()))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());

        verify(userService).getUserById(userId);
        verifyNoMoreInteractions(userService);
    }

    @Test
    void getUserById_whenUserDoesNotExist_returnsNotFound() throws Exception {
        Long userId = 1L;

        when(userService.getUserById(userId))
                .thenThrow(new NoResourceFoundException(userNotFoundWithId(userId)));

        mockMvc.perform(get(userUri(userId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.NOT_FOUND.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(userNotFoundWithId(userId)))
                .andExpect(jsonPath("$.uri").value(userUri(userId)));

        verify(userService).getUserById(userId);
        verifyNoMoreInteractions(userService);
    }

    @Test
    void getUserById_whenUserIdIsInvalid_returnsBadRequest() throws Exception {
        String userId = INVALID_ID;

        mockMvc.perform(get(userUri(userId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(invalidParameter("id")))
                .andExpect(jsonPath("$.uri").value(userUri(userId)));

        verifyNoInteractions(userService);
    }

    @Test
    void getAllUsers_whenUsersExist_returnsPageUserResponse() throws Exception {
        Long firstUserId = 1L;
        Long secondUserId = 2L;

        UserResponse firstUserResponse = new UserResponse(
                firstUserId,
                VALID_EMAIL,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                VALID_PHONE_NUMBER,
                Role.CUSTOMER,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        UserResponse secondUserResponse = new UserResponse(
                secondUserId,
                "secondemail@gmail.com",
                "secondName",
                "secondLast",
                "1234567892",
                Role.CUSTOMER,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        List<UserResponse> listUserResponse = List.of(firstUserResponse, secondUserResponse);
        Pageable pageable = PageRequest.of(0, 10);
        Page<UserResponse> userResponsePage = new PageImpl<>(listUserResponse, pageable, listUserResponse.size());

        when(userService.getAllUsers(any(UserFilterRequest.class), any(Pageable.class))).thenReturn(userResponsePage);

        mockMvc.perform(get(USER_URI)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].id").value(firstUserId))
                .andExpect(jsonPath("$.content[0].email").value(VALID_EMAIL))
                .andExpect(jsonPath("$.content[0].firstName").value(VALID_FIRST_NAME))
                .andExpect(jsonPath("$.content[0].lastName").value(VALID_LAST_NAME))
                .andExpect(jsonPath("$.content[0].phoneNumber").value(VALID_PHONE_NUMBER))
                .andExpect(jsonPath("$.content[0].role").value(Role.CUSTOMER.name()))
                .andExpect(jsonPath("$.content[0].createdAt").exists())
                .andExpect(jsonPath("$.content[0].updatedAt").exists())
                .andExpect(jsonPath("$.content[1].id").value(secondUserId))
                .andExpect(jsonPath("$.content[1].email").value(secondUserResponse.email()))
                .andExpect(jsonPath("$.content[1].firstName").value(secondUserResponse.firstName()))
                .andExpect(jsonPath("$.content[1].lastName").value(secondUserResponse.lastName()))
                .andExpect(jsonPath("$.content[1].phoneNumber").value(secondUserResponse.phoneNumber()))
                .andExpect(jsonPath("$.content[1].role").value(secondUserResponse.role().name()))
                .andExpect(jsonPath("$.content[1].createdAt").exists())
                .andExpect(jsonPath("$.content[1].updatedAt").exists())
                .andExpect(jsonPath("$.numberOfElements").value(2))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1));

        ArgumentCaptor<UserFilterRequest> userFilterCaptor = ArgumentCaptor.forClass(UserFilterRequest.class);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(userService).getAllUsers(userFilterCaptor.capture(), pageableCaptor.capture());


        UserFilterRequest capturedUserFilterRequest = userFilterCaptor.getValue();
        Pageable capturedPageable = pageableCaptor.getValue();

        assertThat(capturedUserFilterRequest.email()).isNull();
        assertThat(capturedUserFilterRequest.keyword()).isNull();
        assertThat(capturedUserFilterRequest.role()).isNull();

        assertThat(capturedPageable.getSort()).contains(Sort.Order.asc("id"));
        assertThat(capturedPageable.getPageNumber()).isEqualTo(0);
        assertThat(capturedPageable.getPageSize()).isEqualTo(10);

        verifyNoMoreInteractions(userService);
    }

    @Test
    void getAllUsers_whenUsersExistWithFilter_returnsPageUserResponse() throws Exception {
        Long firstUserId = 1L;
        Long secondUserId = 2L;

        UserResponse firstUserResponse = new UserResponse(
                firstUserId,
                VALID_EMAIL,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                VALID_PHONE_NUMBER,
                Role.CUSTOMER,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        UserResponse secondUserResponse = new UserResponse(
                secondUserId,
                "secondemail@gmail.com",
                "secondUser",
                "secondLast",
                "1234567892",
                Role.CUSTOMER,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        List<UserResponse> listUserResponses = List.of(firstUserResponse, secondUserResponse);
        Pageable pageable = PageRequest.of(0, 10);
        Page<UserResponse> userResponsePage = new PageImpl<>(listUserResponses, pageable, listUserResponses.size());

        when(userService.getAllUsers(any(UserFilterRequest.class), any(Pageable.class))).thenReturn(userResponsePage);

        mockMvc.perform(get(USER_URI)
                        .param("page", "0")
                        .param("size", "10")
                        .param("email", VALID_EMAIL)
                        .param("keyword", VALID_FIRST_NAME)
                        .param("role", Role.CUSTOMER.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].id").value(firstUserId))
                .andExpect(jsonPath("$.content[0].email").value(VALID_EMAIL))
                .andExpect(jsonPath("$.content[0].firstName").value(VALID_FIRST_NAME))
                .andExpect(jsonPath("$.content[0].lastName").value(VALID_LAST_NAME))
                .andExpect(jsonPath("$.content[0].phoneNumber").value(VALID_PHONE_NUMBER))
                .andExpect(jsonPath("$.content[0].role").value(Role.CUSTOMER.name()))
                .andExpect(jsonPath("$.content[0].createdAt").exists())
                .andExpect(jsonPath("$.content[0].updatedAt").exists())
                .andExpect(jsonPath("$.content[1].id").value(secondUserId))
                .andExpect(jsonPath("$.content[1].email").value(secondUserResponse.email()))
                .andExpect(jsonPath("$.content[1].firstName").value(secondUserResponse.firstName()))
                .andExpect(jsonPath("$.content[1].lastName").value(secondUserResponse.lastName()))
                .andExpect(jsonPath("$.content[1].phoneNumber").value(secondUserResponse.phoneNumber()))
                .andExpect(jsonPath("$.content[1].role").value(secondUserResponse.role().name()))
                .andExpect(jsonPath("$.content[1].createdAt").exists())
                .andExpect(jsonPath("$.content[1].updatedAt").exists())
                .andExpect(jsonPath("$.numberOfElements").value(2))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1));

        ArgumentCaptor<UserFilterRequest> userFilterRequestCaptor = ArgumentCaptor.forClass(UserFilterRequest.class);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(userService).getAllUsers(userFilterRequestCaptor.capture(), pageableCaptor.capture());


        UserFilterRequest capturedUserFilter = userFilterRequestCaptor.getValue();
        Pageable capturedPageable = pageableCaptor.getValue();

        assertThat(capturedUserFilter.email()).isEqualTo(VALID_EMAIL);
        assertThat(capturedUserFilter.keyword()).isEqualTo(VALID_FIRST_NAME);
        assertThat(capturedUserFilter.role()).isEqualTo(Role.CUSTOMER);

        assertThat(capturedPageable.getPageNumber()).isEqualTo(0);
        assertThat(capturedPageable.getPageSize()).isEqualTo(10);
        assertThat(capturedPageable.getSort()).contains(Sort.Order.asc("id"));

        verifyNoMoreInteractions(userService);
    }

    @Test
    void getAllUsers_whenFilterRoleIsInvalid_returnsBadRequest() throws Exception {
        mockMvc.perform(get(USER_URI)
                        .param("page", "0")
                        .param("size", "10")
                        .param("email", VALID_EMAIL)
                        .param("keyword", VALID_FIRST_NAME)
                        .param("role", INVALID_ENUM_VALUE))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                .andExpect(jsonPath("$.uri").value(USER_URI))
                .andExpect(jsonPath("$.fieldErrors.role", containsInAnyOrder(invalidParameter("role"))));

        verifyNoInteractions(userService);
    }

    @Test
    void getAllUsers_whenNoUsersExist_returnsEmptyPage() throws Exception {
        List<UserResponse> listUserResponses = List.of();
        Pageable pageable = PageRequest.of(0, 10);
        Page<UserResponse> userResponsesPage = new PageImpl<>(listUserResponses, pageable, listUserResponses.size());

        when(userService.getAllUsers(any(UserFilterRequest.class), any(Pageable.class))).thenReturn(userResponsesPage);

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

        ArgumentCaptor<UserFilterRequest> userFilterCaptor = ArgumentCaptor.forClass(UserFilterRequest.class);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(userService).getAllUsers(userFilterCaptor.capture(), pageableCaptor.capture());

        UserFilterRequest capturedUserFilterRequest = userFilterCaptor.getValue();
        Pageable capturedPageable = pageableCaptor.getValue();

        assertThat(capturedUserFilterRequest.email()).isNull();
        assertThat(capturedUserFilterRequest.keyword()).isNull();
        assertThat(capturedUserFilterRequest.role()).isNull();

        assertThat(capturedPageable.getPageNumber()).isEqualTo(0);
        assertThat(capturedPageable.getPageSize()).isEqualTo(10);
        assertThat(capturedPageable.getSort()).contains(Sort.Order.asc("id"));

        verifyNoMoreInteractions(userService);
    }

    @Test
    void putUser_whenUserExists_returnsUserResponse() throws Exception {
        Long userId = 1L;

        UserPutRequest userPutRequest = new UserPutRequest(
                VALID_EMAIL,
                VALID_PASSWORD,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                VALID_PHONE_NUMBER);

        UserResponse userResponse = new UserResponse(
                userId,
                VALID_EMAIL,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                VALID_PHONE_NUMBER,
                Role.CUSTOMER,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(userService.putUser(userId, userPutRequest)).thenReturn(userResponse);

        mockMvc.perform(put(userUri(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userPutRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.email").value(VALID_EMAIL))
                .andExpect(jsonPath("$.firstName").value(VALID_FIRST_NAME))
                .andExpect(jsonPath("$.lastName").value(VALID_LAST_NAME))
                .andExpect(jsonPath("$.phoneNumber").value(VALID_PHONE_NUMBER))
                .andExpect(jsonPath("$.role").value(Role.CUSTOMER.name()))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());

        ArgumentCaptor<UserPutRequest> userPutRequestCaptor = ArgumentCaptor.forClass(UserPutRequest.class);
        verify(userService).putUser(eq(userId), userPutRequestCaptor.capture());

        UserPutRequest capturedUserPutRequest = userPutRequestCaptor.getValue();

        assertThat(capturedUserPutRequest.email()).isEqualTo(userPutRequest.email());
        assertThat(capturedUserPutRequest.password()).isEqualTo(userPutRequest.password());
        assertThat(capturedUserPutRequest.firstName()).isEqualTo(userPutRequest.firstName());
        assertThat(capturedUserPutRequest.lastName()).isEqualTo(userPutRequest.lastName());
        assertThat(capturedUserPutRequest.phoneNumber()).isEqualTo(userPutRequest.phoneNumber());

        verifyNoMoreInteractions(userService);
    }

    @Test
    void putUser_whenUserDoesNotExist_returnsNotFound() throws Exception {
        Long userId = 1L;

        UserPutRequest userPutRequest = new UserPutRequest(
                VALID_EMAIL,
                VALID_PASSWORD,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                VALID_PHONE_NUMBER);

        when(userService.putUser(userId, userPutRequest))
                .thenThrow(new NoResourceFoundException(userNotFoundWithId(userId)));

        mockMvc.perform(put(userUri(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userPutRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.NOT_FOUND.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(userNotFoundWithId(userId)))
                .andExpect(jsonPath("$.uri").value(userUri(userId)));

        verify(userService).putUser(userId, userPutRequest);
        verifyNoMoreInteractions(userService);
    }

    @Test
    void putUser_whenEmailAlreadyExists_returnsConflict() throws Exception {
        Long userId = 1L;

        UserPutRequest userPutRequest = new UserPutRequest(
                VALID_EMAIL,
                VALID_PASSWORD,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                VALID_PHONE_NUMBER);

        when(userService.putUser(userId, userPutRequest))
                .thenThrow(new DuplicateResourceException(EMAIL_ALREADY_EXISTS));

        mockMvc.perform(put(userUri(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userPutRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.CONFLICT.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.CONFLICT.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(EMAIL_ALREADY_EXISTS))
                .andExpect(jsonPath("$.uri").value(userUri(userId)));

        verify(userService).putUser(userId, userPutRequest);
        verifyNoMoreInteractions(userService);
    }

    @Test
    void putUser_whenPhoneNumberAlreadyExists_returnsConflict() throws Exception {
        Long userId = 1L;

        UserPutRequest userPutRequest = new UserPutRequest(
                VALID_EMAIL,
                VALID_PASSWORD,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                VALID_PHONE_NUMBER);

        when(userService.putUser(userId, userPutRequest))
                .thenThrow(new DuplicateResourceException(PHONE_NUMBER_ALREADY_EXISTS));

        mockMvc.perform(put(userUri(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userPutRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.CONFLICT.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.CONFLICT.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(PHONE_NUMBER_ALREADY_EXISTS))
                .andExpect(jsonPath("$.uri").value(userUri(userId)));

        verify(userService).putUser(userId, userPutRequest);
        verifyNoMoreInteractions(userService);
    }

    @Test
    void putUser_whenUserIdIsInvalid_returnsBadRequest() throws Exception {
        String userId = INVALID_ID;

        UserPutRequest userPutRequest = new UserPutRequest(
                VALID_EMAIL,
                VALID_PASSWORD,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                VALID_PHONE_NUMBER);

        mockMvc.perform(put(userUri(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userPutRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(invalidParameter("id")))
                .andExpect(jsonPath("$.uri").value(userUri(userId)));

        verifyNoInteractions(userService);
    }

    @Test
    void putUser_whenEmailIsEmpty_returnsBadRequest() throws Exception {
        Long userId = 1L;

        UserPutRequest userPutRequest = new UserPutRequest(
                "",
                VALID_PASSWORD,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                VALID_PHONE_NUMBER);

        mockMvc.perform(put(userUri(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userPutRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                .andExpect(jsonPath("$.uri").value(userUri(userId)))
                .andExpect(jsonPath("$.fieldErrors.email", containsInAnyOrder(EMAIL_IS_REQUIRED)));

        verifyNoInteractions(userService);
    }

    @Test
    void putUser_whenEmailIsNull_returnsBadRequest() throws Exception {
        Long userId = 1L;

        UserPutRequest userPutRequest = new UserPutRequest(
                null,
                VALID_PASSWORD,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                VALID_PHONE_NUMBER);

        mockMvc.perform(put(userUri(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userPutRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                .andExpect(jsonPath("$.uri").value(userUri(userId)))
                .andExpect(jsonPath("$.fieldErrors.email", containsInAnyOrder(EMAIL_IS_REQUIRED)));

        verifyNoInteractions(userService);
    }

    @Test
    void putUser_whenEmailIsInvalid_returnsBadRequest() throws Exception {
        Long userId = 1L;

        UserPutRequest userPutRequest = new UserPutRequest(
                INVALID_EMAIL,
                VALID_PASSWORD,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                VALID_PHONE_NUMBER);

        mockMvc.perform(put(userUri(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userPutRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                .andExpect(jsonPath("$.uri").value(userUri(userId)))
                .andExpect(jsonPath("$.fieldErrors.email", containsInAnyOrder(EMAIL_IS_INVALID)));

        verifyNoInteractions(userService);
    }

    @Test
    void putUser_whenPasswordIsLessThanEight_returnsBadRequest() throws Exception {
        Long userId = 1L;

        UserPutRequest userPutRequest = new UserPutRequest(
                VALID_EMAIL,
                INVALID_PASSWORD_LESS_THAN_EIGHT,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                VALID_PHONE_NUMBER);

        mockMvc.perform(put(userUri(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userPutRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                .andExpect(jsonPath("$.uri").value(userUri(userId)))
                .andExpect(jsonPath("$.fieldErrors.password", containsInAnyOrder(PASSWORD_IS_INVALID)));

        verifyNoInteractions(userService);
    }

    @Test
    void putUser_whenPasswordIsMoreThanFifty_returnsBadRequest() throws Exception {
        Long userId = 1L;

        UserPutRequest userPutRequest = new UserPutRequest(
                VALID_EMAIL,
                INVALID_PASSWORD_MORE_THAN_FIFTY,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                VALID_PHONE_NUMBER);

        mockMvc.perform(put(userUri(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userPutRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                .andExpect(jsonPath("$.uri").value(userUri(userId)))
                .andExpect(jsonPath("$.fieldErrors.password", containsInAnyOrder(PASSWORD_IS_INVALID)));

        verifyNoInteractions(userService);
    }

    @Test
    void putUser_whenPasswordIsEmpty_returnsBadRequest() throws Exception {
        Long userId = 1L;

        UserPutRequest userPutRequest = new UserPutRequest(
                VALID_EMAIL,
                "",
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                VALID_PHONE_NUMBER);

        mockMvc.perform(put(userUri(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userPutRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                .andExpect(jsonPath("$.uri").value(userUri(userId)))
                .andExpect(jsonPath("$.fieldErrors.password", containsInAnyOrder(
                        PASSWORD_IS_INVALID,
                        PASSWORD_IS_REQUIRED)));

        verifyNoInteractions(userService);
    }

    @Test
    void putUser_whenPasswordIsNull_returnsBadRequest() throws Exception {
        Long userId = 1L;

        UserPutRequest userPutRequest = new UserPutRequest(
                VALID_EMAIL,
                null,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                VALID_PHONE_NUMBER);

        mockMvc.perform(put(userUri(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userPutRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                .andExpect(jsonPath("$.uri").value(userUri(userId)))
                .andExpect(jsonPath("$.fieldErrors.password", containsInAnyOrder(PASSWORD_IS_REQUIRED)));

        verifyNoInteractions(userService);
    }

    @Test
    void putUser_whenFirstNameIsEmpty_returnsBadRequest() throws Exception {
        Long userId = 1L;

        UserPutRequest userPutRequest = new UserPutRequest(
                VALID_EMAIL,
                VALID_PASSWORD,
                "",
                VALID_LAST_NAME,
                VALID_PHONE_NUMBER);

        mockMvc.perform(put(userUri(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userPutRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                .andExpect(jsonPath("$.uri").value(userUri(userId)))
                .andExpect(jsonPath("$.fieldErrors.firstName", containsInAnyOrder(FIRST_NAME_IS_REQUIRED)));

        verifyNoInteractions(userService);
    }

    @Test
    void putUser_whenFirstNameIsNull_returnsBadRequest() throws Exception {
        Long userId = 1L;

        UserPutRequest userPutRequest = new UserPutRequest(
                VALID_EMAIL,
                VALID_PASSWORD,
                null,
                VALID_LAST_NAME,
                VALID_PHONE_NUMBER);

        mockMvc.perform(put(userUri(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userPutRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                .andExpect(jsonPath("$.uri").value(userUri(userId)))
                .andExpect(jsonPath("$.fieldErrors.firstName", containsInAnyOrder(FIRST_NAME_IS_REQUIRED)));

        verifyNoInteractions(userService);
    }

    @Test
    void putUser_whenLastNameIsEmpty_returnsBadRequest() throws Exception {
        Long userId = 1L;

        UserPutRequest userPutRequest = new UserPutRequest(
                VALID_EMAIL,
                VALID_PASSWORD,
                VALID_FIRST_NAME,
                "",
                VALID_PHONE_NUMBER);

        mockMvc.perform(put(userUri(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userPutRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                .andExpect(jsonPath("$.uri").value(userUri(userId)))
                .andExpect(jsonPath("$.fieldErrors.lastName", containsInAnyOrder(LAST_NAME_IS_REQUIRED)));

        verifyNoInteractions(userService);
    }

    @Test
    void putUser_whenLastNameIsNull_returnsBadRequest() throws Exception {
        Long userId = 1L;

        UserPutRequest userPutRequest = new UserPutRequest(
                VALID_EMAIL,
                VALID_PASSWORD,
                VALID_FIRST_NAME,
                null,
                VALID_PHONE_NUMBER);

        mockMvc.perform(put(userUri(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userPutRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                .andExpect(jsonPath("$.uri").value(userUri(userId)))
                .andExpect(jsonPath("$.fieldErrors.lastName", containsInAnyOrder(LAST_NAME_IS_REQUIRED)));

        verifyNoInteractions(userService);
    }

    @Test
    void putUser_whenPhoneNumberIsEmpty_returnsBadRequest() throws Exception {
        Long userId = 1L;

        UserPutRequest userPutRequest = new UserPutRequest(
                VALID_EMAIL,
                VALID_PASSWORD,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                "");

        mockMvc.perform(put(userUri(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userPutRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                .andExpect(jsonPath("$.uri").value(userUri(userId)))
                .andExpect(jsonPath("$.fieldErrors.phoneNumber", containsInAnyOrder(
                        PHONE_NUMBER_IS_REQUIRED,
                        PHONE_NUMBER_IS_INVALID)));

        verifyNoInteractions(userService);
    }

    @Test
    void putUser_whenPhoneNumberIsNull_returnsBadRequest() throws Exception {
        Long userId = 1L;

        UserPutRequest userPutRequest = new UserPutRequest(
                VALID_EMAIL,
                VALID_PASSWORD,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                null);

        mockMvc.perform(put(userUri(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userPutRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                .andExpect(jsonPath("$.uri").value(userUri(userId)))
                .andExpect(jsonPath("$.fieldErrors.phoneNumber", containsInAnyOrder(PHONE_NUMBER_IS_REQUIRED)));

        verifyNoInteractions(userService);
    }

    @Test
    void putUser_whenPhoneNumberIsLessThan10Digits_returnsBadRequest() throws Exception {
        Long userId = 1L;

        UserPutRequest userPutRequest = new UserPutRequest(
                VALID_EMAIL,
                VALID_PASSWORD,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                INVALID_PHONE_NUMBER_LESS_THAN_TEN);

        mockMvc.perform(put(userUri(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userPutRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                .andExpect(jsonPath("$.uri").value(userUri(userId)))
                .andExpect(jsonPath("$.fieldErrors.phoneNumber", containsInAnyOrder(PHONE_NUMBER_IS_INVALID)));

        verifyNoInteractions(userService);
    }

    @Test
    void putUser_whenPhoneNumberIsMoreThan15Digits_returnsBadRequest() throws Exception {
        Long userId = 1L;

        UserPutRequest userPutRequest = new UserPutRequest(
                VALID_EMAIL,
                VALID_PASSWORD,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                INVALID_PHONE_NUMBER_MORE_THAN_FIFTEEN);

        mockMvc.perform(put(userUri(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userPutRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                .andExpect(jsonPath("$.uri").value(userUri(userId)))
                .andExpect(jsonPath("$.fieldErrors.phoneNumber", containsInAnyOrder(PHONE_NUMBER_IS_INVALID)));

        verifyNoInteractions(userService);
    }

    @Test
    void putUser_whenPhoneNumberHasInvalidSymbol_returnsBadRequest() throws Exception {
        Long userId = 1L;

        UserPutRequest userPutRequest = new UserPutRequest(
                VALID_EMAIL,
                VALID_PASSWORD,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                INVALID_PHONE_NUMBER_WITH_MINUS);

        mockMvc.perform(put(userUri(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userPutRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                .andExpect(jsonPath("$.uri").value(userUri(userId)))
                .andExpect(jsonPath("$.fieldErrors.phoneNumber", containsInAnyOrder(PHONE_NUMBER_IS_INVALID)));

        verifyNoInteractions(userService);
    }

    @Test
    void patchUser_whenUserExists_returnsUserResponse() throws Exception {
        Long userId = 1L;

        UserPatchRequest userPatchRequest = new UserPatchRequest(
                VALID_EMAIL,
                VALID_PASSWORD,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                VALID_PHONE_NUMBER);

        UserResponse userResponse = new UserResponse(
                userId,
                VALID_EMAIL,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                VALID_PHONE_NUMBER,
                Role.CUSTOMER,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(userService.patchUser(userId, userPatchRequest)).thenReturn(userResponse);

        mockMvc.perform(patch(userUri(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userPatchRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.email").value(VALID_EMAIL))
                .andExpect(jsonPath("$.firstName").value(VALID_FIRST_NAME))
                .andExpect(jsonPath("$.lastName").value(VALID_LAST_NAME))
                .andExpect(jsonPath("$.phoneNumber").value(VALID_PHONE_NUMBER))
                .andExpect(jsonPath("$.role").value(Role.CUSTOMER.name()))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());

        ArgumentCaptor<UserPatchRequest> userPatchRequestCaptor = ArgumentCaptor.forClass(UserPatchRequest.class);
        verify(userService).patchUser(eq(userId), userPatchRequestCaptor.capture());

        UserPatchRequest capturedUserPatchRequest = userPatchRequestCaptor.getValue();

        assertThat(capturedUserPatchRequest.email()).isEqualTo(userPatchRequest.email());
        assertThat(capturedUserPatchRequest.password()).isEqualTo(userPatchRequest.password());
        assertThat(capturedUserPatchRequest.firstName()).isEqualTo(userPatchRequest.firstName());
        assertThat(capturedUserPatchRequest.lastName()).isEqualTo(userPatchRequest.lastName());
        assertThat(capturedUserPatchRequest.phoneNumber()).isEqualTo(userPatchRequest.phoneNumber());

        verifyNoMoreInteractions(userService);
    }

    @Test
    void patchUser_whenUserDoesNotExist_returnsNotFound() throws Exception {
        Long userId = 1L;

        UserPatchRequest userPatchRequest = new UserPatchRequest(
                VALID_EMAIL,
                VALID_PASSWORD,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                VALID_PHONE_NUMBER);

        when(userService.patchUser(userId, userPatchRequest))
                .thenThrow(new NoResourceFoundException(userNotFoundWithId(userId)));

        mockMvc.perform(patch(userUri(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userPatchRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.NOT_FOUND.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(userNotFoundWithId(userId)))
                .andExpect(jsonPath("$.uri").value(userUri(userId)));

        verify(userService).patchUser(userId, userPatchRequest);
        verifyNoMoreInteractions(userService);
    }

    @Test
    void patchUser_whenEmailIsInvalid_returnsBadRequest() throws Exception {
        Long userId = 1L;

        UserPatchRequest userPatchRequest = new UserPatchRequest(
                INVALID_EMAIL,
                VALID_PASSWORD,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                VALID_PHONE_NUMBER);

        mockMvc.perform(patch(userUri(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userPatchRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                .andExpect(jsonPath("$.uri").value(userUri(userId)))
                .andExpect(jsonPath("$.fieldErrors.email", containsInAnyOrder(EMAIL_IS_INVALID)));

        verifyNoInteractions(userService);
    }

    @Test
    void patchUser_whenEmailIsEmpty_returnsBadRequest() throws Exception {
        Long userId = 1L;

        UserPatchRequest userPatchRequest = new UserPatchRequest(
                "",
                VALID_PASSWORD,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                VALID_PHONE_NUMBER);

        mockMvc.perform(patch(userUri(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userPatchRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                .andExpect(jsonPath("$.uri").value(userUri(userId)))
                .andExpect(jsonPath("$.fieldErrors.email", containsInAnyOrder(EMAIL_IS_EMPTY)));

        verifyNoInteractions(userService);
    }

    @Test
    void patchUser_whenPasswordLessThan8Chars_returnsBadRequest() throws Exception {
        Long userId = 1L;

        UserPatchRequest userPatchRequest = new UserPatchRequest(
                VALID_EMAIL,
                INVALID_PASSWORD_LESS_THAN_EIGHT,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                VALID_PHONE_NUMBER);

        mockMvc.perform(patch(userUri(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userPatchRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                .andExpect(jsonPath("$.uri").value(userUri(userId)))
                .andExpect(jsonPath("$.fieldErrors.password", containsInAnyOrder(PASSWORD_IS_INVALID)));

        verifyNoInteractions(userService);
    }

    @Test
    void patchUser_whenPasswordMoreThan50Chars_returnsBadRequest() throws Exception {
        Long userId = 1L;

        UserPatchRequest userPatchRequest = new UserPatchRequest(
                VALID_EMAIL,
                INVALID_PASSWORD_MORE_THAN_FIFTY,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                VALID_PHONE_NUMBER);

        mockMvc.perform(patch(userUri(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userPatchRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                .andExpect(jsonPath("$.uri").value(userUri(userId)))
                .andExpect(jsonPath("$.fieldErrors.password", containsInAnyOrder(PASSWORD_IS_INVALID)));

        verifyNoInteractions(userService);
    }

    @Test
    void patchUser_whenPasswordIsEmpty_returnsBadRequest() throws Exception {
        Long userId = 1L;

        UserPatchRequest userPatchRequest = new UserPatchRequest(
                VALID_EMAIL,
                "",
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                VALID_PHONE_NUMBER);

        mockMvc.perform(patch(userUri(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userPatchRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                .andExpect(jsonPath("$.uri").value(userUri(userId)))
                .andExpect(jsonPath("$.fieldErrors.password", containsInAnyOrder(
                        PASSWORD_IS_EMPTY,
                        PASSWORD_IS_INVALID)));

        verifyNoInteractions(userService);
    }

    @Test
    void patchUser_whenFirstNameIsEmpty_returnsBadRequest() throws Exception {
        Long userId = 1L;

        UserPatchRequest userPatchRequest = new UserPatchRequest(
                VALID_EMAIL,
                VALID_PASSWORD,
                "",
                VALID_LAST_NAME,
                VALID_PHONE_NUMBER);

        mockMvc.perform(patch(userUri(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userPatchRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                .andExpect(jsonPath("$.uri").value(userUri(userId)))
                .andExpect(jsonPath("$.fieldErrors.firstName", containsInAnyOrder(FIRST_NAME_IS_EMPTY)));

        verifyNoInteractions(userService);
    }

    @Test
    void patchUser_whenLastNameIsEmpty_returnsBadRequest() throws Exception {
        Long userId = 1L;

        UserPatchRequest userPatchRequest = new UserPatchRequest(
                VALID_EMAIL,
                VALID_PASSWORD,
                VALID_FIRST_NAME,
                "",
                VALID_PHONE_NUMBER);

        mockMvc.perform(patch(userUri(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userPatchRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                .andExpect(jsonPath("$.uri").value(userUri(userId)))
                .andExpect(jsonPath("$.fieldErrors.lastName", containsInAnyOrder(LAST_NAME_IS_EMPTY)));

        verifyNoInteractions(userService);
    }

    @Test
    void patchUser_whenPhoneNumberIsLessThan10Digits_returnsBadRequest() throws Exception {
        Long userId = 1L;

        UserPatchRequest userPatchRequest = new UserPatchRequest(
                VALID_EMAIL,
                VALID_PASSWORD,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                INVALID_PHONE_NUMBER_LESS_THAN_TEN);

        mockMvc.perform(patch(userUri(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userPatchRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                .andExpect(jsonPath("$.uri").value(userUri(userId)))
                .andExpect(jsonPath("$.fieldErrors.phoneNumber", containsInAnyOrder(PHONE_NUMBER_IS_INVALID)));

        verifyNoInteractions(userService);
    }

    @Test
    void patchUser_whenPhoneNumberMoreThan15_returnsBadRequest() throws Exception {
        Long userId = 1L;

        UserPatchRequest userPatchRequest = new UserPatchRequest(
                VALID_EMAIL,
                VALID_PASSWORD,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                INVALID_PHONE_NUMBER_MORE_THAN_FIFTEEN);

        mockMvc.perform(patch(userUri(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userPatchRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                .andExpect(jsonPath("$.uri").value(userUri(userId)))
                .andExpect(jsonPath("$.fieldErrors.phoneNumber", containsInAnyOrder(PHONE_NUMBER_IS_INVALID)));

        verifyNoInteractions(userService);
    }

    @Test
    void patchUser_whenPhoneNumberIsEmpty_returnsBadRequest() throws Exception {
        Long userId = 1L;

        UserPatchRequest userPatchRequest = new UserPatchRequest(
                VALID_EMAIL,
                VALID_PASSWORD,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                "");

        mockMvc.perform(patch(userUri(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userPatchRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                .andExpect(jsonPath("$.uri").value(userUri(userId)))
                .andExpect(jsonPath("$.fieldErrors.phoneNumber", containsInAnyOrder(PHONE_NUMBER_IS_INVALID)));

        verifyNoInteractions(userService);
    }

    @Test
    void patchUser_whenPhoneNumberHasInvalidSymbol_returnsBadRequest() throws Exception {
        Long userId = 1L;

        UserPatchRequest userPatchRequest = new UserPatchRequest(
                VALID_EMAIL,
                VALID_PASSWORD,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                INVALID_PHONE_NUMBER_WITH_MINUS);

        mockMvc.perform(patch(userUri(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userPatchRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(VALIDATION_FAILED))
                .andExpect(jsonPath("$.uri").value(userUri(userId)))
                .andExpect(jsonPath("$.fieldErrors.phoneNumber", containsInAnyOrder(PHONE_NUMBER_IS_INVALID)));

        verifyNoInteractions(userService);
    }

    @Test
    void patchUser_whenInvalidUserId_returnsBadRequest() throws Exception {
        String userId = INVALID_ID;

        UserPatchRequest userPatchRequest = new UserPatchRequest(
                VALID_EMAIL,
                VALID_PASSWORD,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                VALID_PHONE_NUMBER);

        mockMvc.perform(patch(userUri(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userPatchRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(invalidParameter("id")))
                .andExpect(jsonPath("$.uri").value(userUri(userId)));

        verifyNoInteractions(userService);
    }

    @Test
    void patchUser_whenRequestHasPartialFields_returnsUserResponse() throws Exception {
        Long userId = 1L;

        UserPatchRequest userPatchRequest = new UserPatchRequest(
                VALID_EMAIL,
                VALID_PASSWORD,
                null,
                VALID_LAST_NAME,
                null);

        UserResponse userResponse = new UserResponse(
                userId,
                VALID_EMAIL,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                VALID_PHONE_NUMBER,
                Role.CUSTOMER,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(userService.patchUser(userId, userPatchRequest)).thenReturn(userResponse);

        mockMvc.perform(patch(userUri(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userPatchRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.email").value(VALID_EMAIL))
                .andExpect(jsonPath("$.firstName").value(VALID_FIRST_NAME))
                .andExpect(jsonPath("$.lastName").value(VALID_LAST_NAME))
                .andExpect(jsonPath("$.phoneNumber").value(VALID_PHONE_NUMBER))
                .andExpect(jsonPath("$.role").value(Role.CUSTOMER.name()))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());

        ArgumentCaptor<UserPatchRequest> userPatchRequestCaptor = ArgumentCaptor.forClass(UserPatchRequest.class);
        verify(userService).patchUser(eq(userId), userPatchRequestCaptor.capture());

        UserPatchRequest capturedUserPatchRequest = userPatchRequestCaptor.getValue();

        assertThat(capturedUserPatchRequest.email()).isEqualTo(userPatchRequest.email());
        assertThat(capturedUserPatchRequest.password()).isEqualTo(userPatchRequest.password());
        assertThat(capturedUserPatchRequest.firstName()).isEqualTo(userPatchRequest.firstName());
        assertThat(capturedUserPatchRequest.lastName()).isEqualTo(userPatchRequest.lastName());
        assertThat(capturedUserPatchRequest.phoneNumber()).isEqualTo(userPatchRequest.phoneNumber());

        verifyNoMoreInteractions(userService);
    }

    @Test
    void patchUser_whenAllFieldsAreNull_returnsUserResponse() throws Exception {
        Long userId = 1L;

        UserPatchRequest userPatchRequest = new UserPatchRequest(
                null,
                null,
                null,
                null,
                null);

        UserResponse userResponse = new UserResponse(
                userId,
                VALID_EMAIL,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                VALID_PHONE_NUMBER,
                Role.CUSTOMER,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(userService.patchUser(userId, userPatchRequest)).thenReturn(userResponse);

        mockMvc.perform(patch(userUri(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userPatchRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.email").value(VALID_EMAIL))
                .andExpect(jsonPath("$.firstName").value(VALID_FIRST_NAME))
                .andExpect(jsonPath("$.lastName").value(VALID_LAST_NAME))
                .andExpect(jsonPath("$.phoneNumber").value(VALID_PHONE_NUMBER))
                .andExpect(jsonPath("$.role").value(Role.CUSTOMER.name()))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());

        ArgumentCaptor<UserPatchRequest> userPatchRequestCaptor = ArgumentCaptor.forClass(UserPatchRequest.class);
        verify(userService).patchUser(eq(userId), userPatchRequestCaptor.capture());

        UserPatchRequest capturedUserPatchRequest = userPatchRequestCaptor.getValue();

        assertThat(capturedUserPatchRequest.email()).isNull();
        assertThat(capturedUserPatchRequest.password()).isNull();
        assertThat(capturedUserPatchRequest.firstName()).isNull();
        assertThat(capturedUserPatchRequest.lastName()).isNull();
        assertThat(capturedUserPatchRequest.phoneNumber()).isNull();

        verifyNoMoreInteractions(userService);
    }

    @Test
    void deleteUser_whenUserExists_returnsNoContent() throws Exception {
        Long userId = 1L;

        mockMvc.perform(delete(userUri(userId)))
                .andExpect(status().isNoContent());

        verify(userService).deleteUser(userId);
        verifyNoMoreInteractions(userService);
    }

    @Test
    void deleteUser_whenUserDoesNotExist_returnsNotFound() throws Exception {
        Long userId = 1L;

        doThrow(new NoResourceFoundException(userNotFoundWithId(userId)))
                .when(userService).deleteUser(userId);

        mockMvc.perform(delete(userUri(userId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.NOT_FOUND.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(userNotFoundWithId(userId)))
                .andExpect(jsonPath("$.uri").value(userUri(userId)));

        verify(userService).deleteUser(userId);
        verifyNoMoreInteractions(userService);
    }

    @Test
    void deleteUser_whenUserIdIsInvalid_returnsBadRequest() throws Exception {
        String userId = INVALID_ID;

        mockMvc.perform(delete(userUri(userId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(invalidParameter("id")))
                .andExpect(jsonPath("$.uri").value(userUri(userId)));

        verifyNoInteractions(userService);
    }
}
