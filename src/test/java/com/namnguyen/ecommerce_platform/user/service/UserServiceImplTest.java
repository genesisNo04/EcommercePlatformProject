package com.namnguyen.ecommerce_platform.user.service;

import com.namnguyen.ecommerce_platform.common.exception.*;
import com.namnguyen.ecommerce_platform.user.dto.*;
import com.namnguyen.ecommerce_platform.user.entity.User;
import com.namnguyen.ecommerce_platform.user.enums.Role;
import com.namnguyen.ecommerce_platform.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.data.domain.*;
import java.util.List;
import java.util.Optional;

import static com.namnguyen.ecommerce_platform.testutil.TestDataFactory.*;
import static com.namnguyen.ecommerce_platform.testutil.messages.UserTestMessages.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void createCustomerUser_whenRequestIsValid_savesCustomerAndReturnsUserResponse() {
        Long userId = 1L;

        UserCreateRequest userCreateRequest = createDefaultUserCreateRequest();

        when(userRepository.existsByEmail(userCreateRequest.email())).thenReturn(false);
        when(userRepository.existsByPhoneNumber(userCreateRequest.phoneNumber())).thenReturn(false);
        when(passwordEncoder.encode(userCreateRequest.password())).thenReturn(ENCODED_PASSWORD);

        when(userRepository.save(any(User.class))).thenAnswer(
                inv -> {
                    User savedUser = inv.getArgument(0);
                    savedUser.setId(userId);
                    return savedUser;
                }
        );

        UserResponse userResponse = userService.createUser(userCreateRequest);

        assertThat(userResponse).isNotNull();
        assertThat(userResponse.id()).isEqualTo(userId);
        assertThat(userResponse.email()).isEqualTo(userCreateRequest.email());
        assertThat(userResponse.firstName()).isEqualTo(userCreateRequest.firstName());
        assertThat(userResponse.lastName()).isEqualTo(userCreateRequest.lastName());
        assertThat(userResponse.phoneNumber()).isEqualTo(userCreateRequest.phoneNumber());
        assertThat(userResponse.role()).isEqualTo(Role.CUSTOMER);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();

        assertThat(savedUser.getId()).isEqualTo(userId);
        assertThat(savedUser.getEmail()).isEqualTo(userCreateRequest.email());
        assertThat(savedUser.getFirstName()).isEqualTo(userCreateRequest.firstName());
        assertThat(savedUser.getLastName()).isEqualTo(userCreateRequest.lastName());
        assertThat(savedUser.getPhoneNumber()).isEqualTo(userCreateRequest.phoneNumber());
        assertThat(savedUser.getRole()).isEqualTo(Role.CUSTOMER);
        assertThat(savedUser.getPasswordHash()).isEqualTo(ENCODED_PASSWORD);
        assertThat(savedUser.getPasswordHash()).isNotEqualTo(userCreateRequest.password());

        verify(userRepository).existsByEmail(userCreateRequest.email());
        verify(userRepository).existsByPhoneNumber(userCreateRequest.phoneNumber());
        verifyNoMoreInteractions(userRepository);

        verify(passwordEncoder).encode(userCreateRequest.password());
        verifyNoMoreInteractions(passwordEncoder);
    }

    @Test
    void createAdmin_whenRequestIsValid_savesAdminAndReturnsUserResponse() {
        Long userId = 1L;

        UserCreateRequest userCreateRequest = createDefaultUserCreateRequest();

        when(userRepository.existsByEmail(userCreateRequest.email())).thenReturn(false);
        when(userRepository.existsByPhoneNumber(userCreateRequest.phoneNumber())).thenReturn(false);
        when(passwordEncoder.encode(userCreateRequest.password())).thenReturn(ENCODED_PASSWORD);

        when(userRepository.save(any(User.class))).thenAnswer(
                inv -> {
                    User savedUser = inv.getArgument(0);
                    savedUser.setId(userId);
                    return savedUser;
                }
        );

        UserResponse userResponse = userService.createAdminUser(userCreateRequest);

        assertThat(userResponse).isNotNull();
        assertThat(userResponse.id()).isEqualTo(userId);
        assertThat(userResponse.email()).isEqualTo(userCreateRequest.email());
        assertThat(userResponse.firstName()).isEqualTo(userCreateRequest.firstName());
        assertThat(userResponse.lastName()).isEqualTo(userCreateRequest.lastName());
        assertThat(userResponse.phoneNumber()).isEqualTo(userCreateRequest.phoneNumber());
        assertThat(userResponse.role()).isEqualTo(Role.ADMIN);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();

        assertThat(savedUser.getId()).isEqualTo(userId);
        assertThat(savedUser.getEmail()).isEqualTo(userCreateRequest.email());
        assertThat(savedUser.getFirstName()).isEqualTo(userCreateRequest.firstName());
        assertThat(savedUser.getLastName()).isEqualTo(userCreateRequest.lastName());
        assertThat(savedUser.getPhoneNumber()).isEqualTo(userCreateRequest.phoneNumber());
        assertThat(savedUser.getRole()).isEqualTo(Role.ADMIN);
        assertThat(savedUser.getPasswordHash()).isEqualTo(ENCODED_PASSWORD);
        assertThat(savedUser.getPasswordHash()).isNotEqualTo(userCreateRequest.password());

        verify(userRepository).existsByEmail(userCreateRequest.email());
        verify(userRepository).existsByPhoneNumber(userCreateRequest.phoneNumber());
        verifyNoMoreInteractions(userRepository);

        verify(passwordEncoder).encode(userCreateRequest.password());
        verifyNoMoreInteractions(passwordEncoder);
    }

    @Test
    void createUser_whenEmailAlreadyExists_throwsDuplicateResourceException() {
        UserCreateRequest userCreateRequest = createDefaultUserCreateRequest();

        when(userRepository.existsByEmail(userCreateRequest.email())).thenReturn(true);

        DuplicateResourceException ex = assertThrows(
                DuplicateResourceException.class,
                () -> userService.createUser(userCreateRequest)
        );

        assertThat(ex).isNotNull();
        assertThat(ex.getMessage()).isEqualTo(EMAIL_ALREADY_EXISTS);

        verify(userRepository).existsByEmail(userCreateRequest.email());
        verify(userRepository, never()).existsByPhoneNumber(userCreateRequest.phoneNumber());
        verify(userRepository, never()).save(any(User.class));
        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void createUser_whenPhoneNumberAlreadyExists_throwsDuplicateResourceException() {
        UserCreateRequest userCreateRequest = createDefaultUserCreateRequest();

        when(userRepository.existsByEmail(userCreateRequest.email())).thenReturn(false);
        when(userRepository.existsByPhoneNumber(userCreateRequest.phoneNumber())).thenReturn(true);

        DuplicateResourceException ex = assertThrows(
                DuplicateResourceException.class,
                () -> userService.createUser(userCreateRequest)
        );

        assertThat(ex).isNotNull();
        assertThat(ex.getMessage()).isEqualTo(PHONE_NUMBER_ALREADY_EXISTS);

        verify(userRepository).existsByEmail(userCreateRequest.email());
        verify(userRepository).existsByPhoneNumber(userCreateRequest.phoneNumber());
        verify(userRepository, never()).save(any(User.class));
        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void getUserById_whenUserExists_returnsUserResponse() {
        Long userId = 1L;
        User user = createDefaultUser(userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        UserResponse userResponse = userService.getUserById(userId);

        assertThat(userResponse).isNotNull();
        assertThat(userResponse.id()).isEqualTo(userId);
        assertThat(userResponse.email()).isEqualTo(user.getEmail());
        assertThat(userResponse.phoneNumber()).isEqualTo(user.getPhoneNumber());
        assertThat(userResponse.firstName()).isEqualTo(user.getFirstName());
        assertThat(userResponse.lastName()).isEqualTo(user.getLastName());
        assertThat(userResponse.role()).isEqualTo(user.getRole());

        verify(userRepository).findById(userId);
        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void getUserById_whenUserDoesNotExist_throwsNoResourceFoundException() {
        Long userId = 999L;

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        NoResourceFoundException ex = assertThrows(
                NoResourceFoundException.class,
                () -> userService.getUserById(userId)
        );

        assertThat(ex).isNotNull();
        assertThat(ex.getMessage()).isEqualTo(userNotFoundWithId(userId));

        verify(userRepository).findById(userId);
        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void getAllUsers_whenUsersExist_returnsPagedUserResponses() {
        Long firstUserId = 1L;
        User firstUser = createDefaultUser(firstUserId);

        Long secondUserId = 2L;
        User secondUser = createUser(
                secondUserId,
                "secondemail@gmail.com",
                ENCODED_PASSWORD,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                "98765432123",
                Role.CUSTOMER
        );

        UserFilterRequest userFilterRequest = new UserFilterRequest(null, null, null);

        List<User> users = List.of(firstUser, secondUser);
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> usersPage = new PageImpl<>(users, pageable, users.size());

        when(userRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(usersPage);

        Page<UserResponse> userPageResponse = userService.getAllUsers(userFilterRequest, pageable);

        assertThat(userPageResponse).isNotNull();
        assertThat(userPageResponse.getTotalElements()).isEqualTo(2);
        assertThat(userPageResponse.getNumberOfElements()).isEqualTo(2);
        assertThat(userPageResponse.getTotalPages()).isEqualTo(1);
        assertThat(userPageResponse.getSize()).isEqualTo(10);
        assertThat(userPageResponse.getNumber()).isEqualTo(0);

        assertThat(userPageResponse.getContent()).hasSize(2);

        UserResponse firstUserResponse = userPageResponse.getContent().getFirst();
        assertThat(firstUserResponse.id()).isEqualTo(firstUserId);
        assertThat(firstUserResponse.email()).isEqualTo(firstUser.getEmail());
        assertThat(firstUserResponse.role()).isEqualTo(firstUser.getRole());

        UserResponse secondUserResponse = userPageResponse.getContent().get(1);
        assertThat(secondUserResponse.id()).isEqualTo(secondUserId);
        assertThat(secondUserResponse.email()).isEqualTo(secondUser.getEmail());
        assertThat(secondUserResponse.role()).isEqualTo(secondUser.getRole());

        verify(userRepository).findAll(any(Specification.class), eq(pageable));
        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void getAllUsers_whenNoUsersExist_returnsPagedUserResponses() {
        UserFilterRequest userFilterRequest = new UserFilterRequest(null, null, null);

        List<User> users = List.of();
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> pageUsers = new PageImpl<>(users, pageable, 0);

        when(userRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(pageUsers);

        Page<UserResponse> userResponsesPage = userService.getAllUsers(userFilterRequest, pageable);

        assertThat(userResponsesPage).isNotNull();
        assertThat(userResponsesPage.getTotalElements()).isEqualTo(0);
        assertThat(userResponsesPage.getNumberOfElements()).isEqualTo(0);
        assertThat(userResponsesPage.getTotalPages()).isEqualTo(0);
        assertThat(userResponsesPage.getSize()).isEqualTo(10);
        assertThat(userResponsesPage.getNumber()).isEqualTo(0);

        verify(userRepository).findAll(any(Specification.class), eq(pageable));
        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void putUser_whenRequestIsValid_savesCustomerAndReturnsUserResponse() {
        Long userId = 1L;
        User user = createDefaultUser(userId);

        UserPutRequest userPutRequest = createDefaultUserPutRequest();

        when(userRepository.findByEmail(userPutRequest.email())).thenReturn(Optional.empty());
        when(userRepository.findByPhoneNumber(userPutRequest.phoneNumber())).thenReturn(Optional.empty());
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode(userPutRequest.password())).thenReturn(ENCODED_UPDATE_PASSWORD);

        UserResponse userResponse = userService.putUser(userId, userPutRequest);

        assertThat(userResponse).isNotNull();
        assertThat(userResponse.id()).isEqualTo(userId);
        assertThat(userResponse.email()).isEqualTo(userPutRequest.email());
        assertThat(userResponse.firstName()).isEqualTo(userPutRequest.firstName());
        assertThat(userResponse.lastName()).isEqualTo(userPutRequest.lastName());
        assertThat(userResponse.phoneNumber()).isEqualTo(userPutRequest.phoneNumber());
        assertThat(userResponse.role()).isEqualTo(Role.CUSTOMER);
        assertThat(user.getPasswordHash()).isNotEqualTo(userPutRequest.password());

        assertThat(user.getEmail())
                .isEqualTo(userPutRequest.email());
        assertThat(user.getFirstName())
                .isEqualTo(userPutRequest.firstName());
        assertThat(user.getLastName())
                .isEqualTo(userPutRequest.lastName());
        assertThat(user.getPhoneNumber())
                .isEqualTo(userPutRequest.phoneNumber());
        assertThat(user.getRole())
                .isEqualTo(Role.CUSTOMER);
        assertThat(user.getPasswordHash())
                .isEqualTo(ENCODED_UPDATE_PASSWORD);
        assertThat(user.getPasswordHash())
                .isNotEqualTo(userPutRequest.password());

        verify(userRepository).findByEmail(userPutRequest.email());
        verify(userRepository).findByPhoneNumber(userPutRequest.phoneNumber());
        verify(userRepository).findById(userId);
        verifyNoMoreInteractions(userRepository);

        verify(passwordEncoder).encode(userPutRequest.password());
        verifyNoMoreInteractions(passwordEncoder);
    }

    @Test
    void putUser_whenUserDoesNotExist_throwsNoResourceFoundException() {
        Long userId = 999L;

        UserPutRequest userPutRequest = createDefaultUserPutRequest();

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        NoResourceFoundException ex = assertThrows(
                NoResourceFoundException.class,
                () -> userService.putUser(userId, userPutRequest)
        );

        assertThat(ex.getMessage()).isEqualTo(userNotFoundWithId(userId));

        verify(userRepository).findById(userId);
        verify(userRepository, never()).findByEmail(anyString());
        verify(userRepository, never()).findByPhoneNumber(anyString());
        verifyNoMoreInteractions(userRepository);
        verifyNoMoreInteractions(passwordEncoder);
    }

    @Test
    void putUser_whenEmailIsDuplicated_throwsDuplicateResourceException() {
        Long firstUserId = 1L;
        User firstUser = createUser(
                firstUserId,
                VALID_EMAIL,
                ENCODED_PASSWORD,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                VALID_PHONE_NUMBER,
                Role.CUSTOMER
        );

        Long secondUserId = 2L;
        User secondUser = createUser(
                secondUserId,
                VALID_UPDATE_EMAIL,
                ENCODED_PASSWORD,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                "1234567892",
                Role.CUSTOMER
        );

        UserPutRequest userPutRequest = new UserPutRequest(
                VALID_UPDATE_EMAIL,
                VALID_UPDATE_PASSWORD,
                VALID_UPDATE_FIRST_NAME,
                VALID_UPDATE_LAST_NAME,
                "1234567893"
        );

        when(userRepository.findByEmail(userPutRequest.email())).thenReturn(Optional.of(secondUser));
        when(userRepository.findById(firstUserId)).thenReturn(Optional.of(firstUser));

        DuplicateResourceException ex = assertThrows(
                DuplicateResourceException.class,
                () -> userService.putUser(firstUserId, userPutRequest)
        );

        assertThat(ex).isNotNull();
        assertThat(ex.getMessage()).isEqualTo(EMAIL_ALREADY_EXISTS);

        verify(userRepository).findById(firstUserId);
        verify(userRepository).findByEmail(userPutRequest.email());
        verifyNoMoreInteractions(userRepository);
        verifyNoMoreInteractions(passwordEncoder);
    }

    @Test
    void putUser_whenPhoneNumberIsDuplicated_throwsDuplicateResourceException() {
        Long firstUserId = 1L;
        User firstUser = createUser(
                firstUserId,
                VALID_EMAIL,
                ENCODED_PASSWORD,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                VALID_PHONE_NUMBER,
                Role.CUSTOMER
        );

        Long secondUserId = 2L;
        User secondUser = createUser(
                secondUserId,
                "secondemail@gmail.com",
                ENCODED_PASSWORD,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                VALID_UPDATE_PHONE_NUMBER,
                Role.CUSTOMER
        );

        UserPutRequest userPutRequest = new UserPutRequest(
                "thirdemail@gmail.com",
                VALID_PASSWORD,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                VALID_UPDATE_PHONE_NUMBER
        );

        when(userRepository.findByEmail(userPutRequest.email())).thenReturn(Optional.empty());
        when(userRepository.findByPhoneNumber(userPutRequest.phoneNumber())).thenReturn(Optional.of(secondUser));
        when(userRepository.findById(firstUserId)).thenReturn(Optional.of(firstUser));

        DuplicateResourceException ex = assertThrows(
                DuplicateResourceException.class,
                () -> userService.putUser(firstUserId, userPutRequest)
        );

        assertThat(ex).isNotNull();
        assertThat(ex.getMessage()).isEqualTo(PHONE_NUMBER_ALREADY_EXISTS);

        verify(userRepository).findById(firstUserId);
        verify(userRepository).findByEmail(userPutRequest.email());
        verify(userRepository).findByPhoneNumber(userPutRequest.phoneNumber());
        verifyNoMoreInteractions(userRepository);
        verifyNoMoreInteractions(passwordEncoder);
    }

    @Test
    void putUser_whenUserKeepSameEmail_savesCustomerAndReturnsUserResponse() {
        Long userId = 1L;
        User user = createUser(
                userId,
                VALID_EMAIL,
                ENCODED_PASSWORD,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                VALID_PHONE_NUMBER,
                Role.CUSTOMER
        );

        UserPutRequest userPutRequest = new UserPutRequest(
                VALID_EMAIL,
                VALID_UPDATE_PASSWORD,
                VALID_UPDATE_FIRST_NAME,
                VALID_UPDATE_LAST_NAME,
                VALID_UPDATE_PHONE_NUMBER
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.findByEmail(userPutRequest.email())).thenReturn(Optional.of(user));
        when(userRepository.findByPhoneNumber(userPutRequest.phoneNumber())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(userPutRequest.password())).thenReturn(ENCODED_UPDATE_PASSWORD);

        UserResponse userResponse = userService.putUser(userId, userPutRequest);

        assertThat(userResponse).isNotNull();
        assertThat(userResponse.id()).isEqualTo(userId);
        assertThat(userResponse.email()).isEqualTo(userPutRequest.email());
        assertThat(userResponse.firstName()).isEqualTo(userPutRequest.firstName());
        assertThat(userResponse.lastName()).isEqualTo(userPutRequest.lastName());
        assertThat(userResponse.phoneNumber()).isEqualTo(userPutRequest.phoneNumber());
        assertThat(userResponse.role()).isEqualTo(Role.CUSTOMER);
        assertThat(user.getPasswordHash()).isNotEqualTo(userPutRequest.password());

        assertThat(user.getEmail())
                .isEqualTo(userPutRequest.email());
        assertThat(user.getFirstName())
                .isEqualTo(userPutRequest.firstName());
        assertThat(user.getLastName())
                .isEqualTo(userPutRequest.lastName());
        assertThat(user.getPhoneNumber())
                .isEqualTo(userPutRequest.phoneNumber());
        assertThat(user.getRole())
                .isEqualTo(Role.CUSTOMER);
        assertThat(user.getPasswordHash())
                .isEqualTo(ENCODED_UPDATE_PASSWORD);
        assertThat(user.getPasswordHash())
                .isNotEqualTo(userPutRequest.password());

        verify(userRepository).findById(userId);
        verify(userRepository).findByEmail(userPutRequest.email());
        verify(userRepository).findByPhoneNumber(userPutRequest.phoneNumber());
        verify(passwordEncoder).encode(userPutRequest.password());
        verifyNoMoreInteractions(userRepository);
        verifyNoMoreInteractions(passwordEncoder);
    }

    @Test
    void putUser_whenUserKeepSamePhoneNumber_savesCustomerAndReturnsUserResponse() {
        Long userId = 1L;
        User user = createUser(
                userId,
                VALID_EMAIL,
                ENCODED_PASSWORD,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                VALID_PHONE_NUMBER,
                Role.CUSTOMER
        );

        UserPutRequest userPutRequest = new UserPutRequest(
                VALID_UPDATE_EMAIL,
                VALID_UPDATE_PASSWORD,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                VALID_PHONE_NUMBER
        );

        when(userRepository.findByEmail(userPutRequest.email())).thenReturn(Optional.empty());
        when(userRepository.findByPhoneNumber(userPutRequest.phoneNumber())).thenReturn(Optional.of(user));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode(userPutRequest.password())).thenReturn(ENCODED_UPDATE_PASSWORD);

        UserResponse response = userService.putUser(userId, userPutRequest);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(userId);
        assertThat(response.email()).isEqualTo(userPutRequest.email());
        assertThat(response.firstName()).isEqualTo(userPutRequest.firstName());
        assertThat(response.lastName()).isEqualTo(userPutRequest.lastName());
        assertThat(response.phoneNumber()).isEqualTo(userPutRequest.phoneNumber());
        assertThat(response.role()).isEqualTo(Role.CUSTOMER);
        assertThat(user.getPasswordHash()).isNotEqualTo(userPutRequest.password());

        assertThat(user.getEmail())
                .isEqualTo(userPutRequest.email());
        assertThat(user.getFirstName())
                .isEqualTo(userPutRequest.firstName());
        assertThat(user.getLastName())
                .isEqualTo(userPutRequest.lastName());
        assertThat(user.getPhoneNumber())
                .isEqualTo(userPutRequest.phoneNumber());
        assertThat(user.getRole())
                .isEqualTo(Role.CUSTOMER);
        assertThat(user.getPasswordHash())
                .isEqualTo(ENCODED_UPDATE_PASSWORD);
        assertThat(user.getPasswordHash())
                .isNotEqualTo(userPutRequest.password());

        verify(userRepository).findById(userId);
        verify(userRepository).findByEmail(userPutRequest.email());
        verify(userRepository).findByPhoneNumber(userPutRequest.phoneNumber());
        verify(passwordEncoder).encode(userPutRequest.password());
        verifyNoMoreInteractions(userRepository);
        verifyNoMoreInteractions(passwordEncoder);
    }

    @Test
    void patchUser_whenRequestIsValid_savesCustomerAndReturnsUserResponse() {
        Long userId = 1L;
        User user = createDefaultUser(userId);
        UserPatchRequest userPatchRequest = createDefaultUserPatchRequest();

        when(userRepository.findByEmail(userPatchRequest.email())).thenReturn(Optional.empty());
        when(userRepository.findByPhoneNumber(userPatchRequest.phoneNumber())).thenReturn(Optional.empty());
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode(userPatchRequest.password())).thenReturn(ENCODED_UPDATE_PASSWORD);

        UserResponse response = userService.patchUser(userId, userPatchRequest);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(userId);
        assertThat(response.email()).isEqualTo(userPatchRequest.email());
        assertThat(response.firstName()).isEqualTo(userPatchRequest.firstName());
        assertThat(response.lastName()).isEqualTo(userPatchRequest.lastName());
        assertThat(response.phoneNumber()).isEqualTo(userPatchRequest.phoneNumber());
        assertThat(response.role()).isEqualTo(Role.CUSTOMER);

        assertThat(user.getEmail())
                .isEqualTo(userPatchRequest.email());
        assertThat(user.getFirstName())
                .isEqualTo(userPatchRequest.firstName());
        assertThat(user.getLastName())
                .isEqualTo(userPatchRequest.lastName());
        assertThat(user.getPhoneNumber())
                .isEqualTo(userPatchRequest.phoneNumber());
        assertThat(user.getRole())
                .isEqualTo(Role.CUSTOMER);
        assertThat(user.getPasswordHash())
                .isEqualTo(ENCODED_UPDATE_PASSWORD);
        assertThat(user.getPasswordHash())
                .isNotEqualTo(userPatchRequest.password());

        verify(userRepository).findByEmail(userPatchRequest.email());
        verify(userRepository).findByPhoneNumber(userPatchRequest.phoneNumber());
        verify(userRepository).findById(userId);
        verifyNoMoreInteractions(userRepository);

        verify(passwordEncoder).encode(userPatchRequest.password());
        verifyNoMoreInteractions(passwordEncoder);
    }

    @Test
    void patchUser_whenUserDoesNotExist_throwsNoResourceFoundException() {
        Long userId = 999L;

        UserPatchRequest request = createDefaultUserPatchRequest();

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        NoResourceFoundException ex = assertThrows(
                NoResourceFoundException.class,
                () -> userService.patchUser(userId, request)
        );

        assertThat(ex.getMessage()).isEqualTo(userNotFoundWithId(userId));

        verify(userRepository).findById(userId);
        verify(userRepository, never()).findByEmail(anyString());
        verify(userRepository, never()).findByPhoneNumber(anyString());
        verifyNoMoreInteractions(userRepository);
        verifyNoMoreInteractions(passwordEncoder);
    }

    @Test
    void patchUser_whenRequestIsValidPartiallyUpdate_savesCustomerAndReturnsUserResponse() {
        Long userId = 1L;
        User user = createDefaultUser(userId);

        UserPatchRequest userPatchRequest = createUserPatchRequest(
                VALID_UPDATE_EMAIL,
                null,
                null,
                null,
                VALID_UPDATE_PHONE_NUMBER
        );
        
        String originalFirstName = user.getFirstName();
        String originalLastName = user.getLastName();
        String originalPassword = user.getPasswordHash();

        when(userRepository.findByEmail(userPatchRequest.email())).thenReturn(Optional.empty());
        when(userRepository.findByPhoneNumber(userPatchRequest.phoneNumber())).thenReturn(Optional.empty());
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        UserResponse userResponse = userService.patchUser(userId, userPatchRequest);

        assertThat(userResponse).isNotNull();
        assertThat(userResponse.id()).isEqualTo(userId);
        assertThat(userResponse.email()).isEqualTo(userPatchRequest.email());
        assertThat(userResponse.firstName()).isEqualTo(originalFirstName);
        assertThat(userResponse.lastName()).isEqualTo(originalLastName);
        assertThat(userResponse.phoneNumber()).isEqualTo(userPatchRequest.phoneNumber());
        assertThat(userResponse.role()).isEqualTo(Role.CUSTOMER);
        assertThat(user.getPasswordHash()).isEqualTo(originalPassword);
        assertThat(user.getEmail())
                .isEqualTo(userPatchRequest.email());
        assertThat(user.getFirstName())
                .isEqualTo(originalFirstName);
        assertThat(user.getLastName())
                .isEqualTo(originalLastName);
        assertThat(user.getPhoneNumber())
                .isEqualTo(userPatchRequest.phoneNumber());
        assertThat(user.getRole())
                .isEqualTo(Role.CUSTOMER);
        assertThat(user.getPasswordHash())
                .isEqualTo(originalPassword);

        verifyNoInteractions(passwordEncoder);
        verify(userRepository).findByEmail(userPatchRequest.email());
        verify(userRepository).findByPhoneNumber(userPatchRequest.phoneNumber());
        verify(userRepository).findById(userId);
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    void patchUser_whenAllFieldsAreNull_keepsExistingUserUnchanged() {
        Long userId = 1L;
        User user = createDefaultUser(userId);

        UserPatchRequest userPatchRequest = new UserPatchRequest(
                null,
                null,
                null,
                null,
                null
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        
        String originalEmail = user.getEmail();
        String originalPassword = user.getPasswordHash();
        String originalFirstName = user.getFirstName();
        String originalLastName = user.getLastName();
        String originalPhoneNumber = user.getPhoneNumber();

        UserResponse userResponse = userService.patchUser(userId, userPatchRequest);

        assertThat(userResponse).isNotNull();
        assertThat(userResponse.id()).isEqualTo(userId);
        assertThat(userResponse.email()).isEqualTo(originalEmail);
        assertThat(userResponse.firstName()).isEqualTo(originalFirstName);
        assertThat(userResponse.lastName()).isEqualTo(originalLastName);
        assertThat(userResponse.phoneNumber()).isEqualTo(originalPhoneNumber);
        assertThat(userResponse.role()).isEqualTo(user.getRole());

        assertThat(user.getEmail())
                .isEqualTo(originalEmail);
        assertThat(user.getFirstName())
                .isEqualTo(originalFirstName);
        assertThat(user.getLastName())
                .isEqualTo(originalLastName);
        assertThat(user.getPhoneNumber())
                .isEqualTo(originalPhoneNumber);
        assertThat(user.getRole())
                .isEqualTo(Role.CUSTOMER);
        assertThat(user.getPasswordHash())
                .isEqualTo(originalPassword);

        verifyNoInteractions(passwordEncoder);
        verify(userRepository).findById(userId);
        verify(userRepository, never()).findByEmail(any());
        verify(userRepository, never()).findByPhoneNumber(any());
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    void patchUser_whenEmailIsDuplicated_throwsDuplicateResourceException() {
        Long firstUserId = 1L;
        User firstUser = createUser(
                firstUserId,
                VALID_EMAIL,
                ENCODED_PASSWORD,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                VALID_PHONE_NUMBER,
                Role.CUSTOMER
        );

        Long secondUserId = 2L;
        User secondUser = createUser(
                secondUserId,
                VALID_UPDATE_EMAIL,
                ENCODED_PASSWORD,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                "12345678912",
                Role.CUSTOMER
        );

        UserPatchRequest userPatchRequest = new UserPatchRequest(
                VALID_UPDATE_EMAIL,
                VALID_PASSWORD,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                "12345678913"
        );

        when(userRepository.findByEmail(userPatchRequest.email())).thenReturn(Optional.of(secondUser));
        when(userRepository.findById(firstUserId)).thenReturn(Optional.of(firstUser));

        DuplicateResourceException ex = assertThrows(
                DuplicateResourceException.class,
                () -> userService.patchUser(firstUserId, userPatchRequest)
        );

        assertThat(ex).isNotNull();
        assertThat(ex.getMessage()).isEqualTo(EMAIL_ALREADY_EXISTS);

        verify(userRepository).findById(firstUserId);
        verify(userRepository).findByEmail(userPatchRequest.email());
        verifyNoMoreInteractions(userRepository);
        verifyNoMoreInteractions(passwordEncoder);
    }

    @Test
    void patchUser_whenPhoneNumberIsDuplicated_throwsDuplicateResourceException() {
        Long firstUserId = 1L;
        User firstUser = createUser(
                firstUserId,
                VALID_EMAIL,
                ENCODED_PASSWORD,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                VALID_PHONE_NUMBER,
                Role.CUSTOMER
        );

        Long secondUserId = 2L;
        User secondUser = createUser(
                secondUserId,
                "secondEmail@gmail.com",
                ENCODED_PASSWORD,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                VALID_UPDATE_PHONE_NUMBER,
                Role.CUSTOMER
        );

        UserPatchRequest userPatchRequest = new UserPatchRequest(
                "thirdEmail@gmail.com",
                VALID_PASSWORD,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                VALID_UPDATE_PHONE_NUMBER
        );

        when(userRepository.findByEmail(userPatchRequest.email())).thenReturn(Optional.empty());
        when(userRepository.findByPhoneNumber(userPatchRequest.phoneNumber())).thenReturn(Optional.of(secondUser));
        when(userRepository.findById(firstUserId)).thenReturn(Optional.of(firstUser));

        DuplicateResourceException ex = assertThrows(
                DuplicateResourceException.class,
                () -> userService.patchUser(firstUserId, userPatchRequest)
        );

        assertThat(ex).isNotNull();
        assertThat(ex.getMessage()).isEqualTo(PHONE_NUMBER_ALREADY_EXISTS);

        verify(userRepository).findById(firstUserId);
        verify(userRepository).findByEmail(userPatchRequest.email());
        verify(userRepository).findByPhoneNumber(userPatchRequest.phoneNumber());
        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void patchUser_whenUserKeepSameEmail_savesCustomerAndReturnsUserResponse() {
        Long userId = 1L;
        User user = createDefaultUser(userId);

        UserPatchRequest userPatchRequest = new UserPatchRequest(
                user.getEmail(),
                VALID_UPDATE_PASSWORD,
                VALID_UPDATE_FIRST_NAME,
                VALID_UPDATE_LAST_NAME,
                VALID_UPDATE_PHONE_NUMBER
        );

        when(userRepository.findByEmail(userPatchRequest.email())).thenReturn(Optional.of(user));
        when(userRepository.findByPhoneNumber(userPatchRequest.phoneNumber())).thenReturn(Optional.empty());
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode(userPatchRequest.password())).thenReturn(ENCODED_UPDATE_PASSWORD);

        UserResponse userResponse = userService.patchUser(userId, userPatchRequest);

        assertThat(userResponse).isNotNull();
        assertThat(userResponse.id()).isEqualTo(userId);
        assertThat(userResponse.email()).isEqualTo(userPatchRequest.email());
        assertThat(userResponse.firstName()).isEqualTo(userPatchRequest.firstName());
        assertThat(userResponse.lastName()).isEqualTo(userPatchRequest.lastName());
        assertThat(userResponse.phoneNumber()).isEqualTo(userPatchRequest.phoneNumber());
        assertThat(userResponse.role()).isEqualTo(Role.CUSTOMER);
        assertThat(user.getPasswordHash()).isNotEqualTo(userPatchRequest.password());

        assertThat(user.getEmail())
                .isEqualTo(userPatchRequest.email());
        assertThat(user.getFirstName())
                .isEqualTo(userPatchRequest.firstName());
        assertThat(user.getLastName())
                .isEqualTo(userPatchRequest.lastName());
        assertThat(user.getPhoneNumber())
                .isEqualTo(userPatchRequest.phoneNumber());
        assertThat(user.getRole())
                .isEqualTo(Role.CUSTOMER);
        assertThat(user.getPasswordHash())
                .isEqualTo(ENCODED_UPDATE_PASSWORD);
        assertThat(user.getPasswordHash())
                .isNotEqualTo(userPatchRequest.password());

        verify(userRepository).findById(userId);
        verify(userRepository).findByEmail(userPatchRequest.email());
        verify(userRepository).findByPhoneNumber(userPatchRequest.phoneNumber());
        verify(passwordEncoder).encode(userPatchRequest.password());
        verifyNoMoreInteractions(userRepository);
        verifyNoMoreInteractions(passwordEncoder);
    }

    @Test
    void patchUser_whenUserKeepSamePhoneNumber_savesCustomerAndReturnsUserResponse() {
        Long userId = 1L;
        User user = createDefaultUser(userId);

        UserPatchRequest userPatchRequest = new UserPatchRequest(
                VALID_UPDATE_EMAIL,
                VALID_UPDATE_PASSWORD,
                VALID_UPDATE_FIRST_NAME,
                VALID_UPDATE_LAST_NAME,
                user.getPhoneNumber()
        );

        when(userRepository.findByEmail(userPatchRequest.email())).thenReturn(Optional.empty());
        when(userRepository.findByPhoneNumber(userPatchRequest.phoneNumber())).thenReturn(Optional.of(user));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode(userPatchRequest.password())).thenReturn(ENCODED_UPDATE_PASSWORD);

        UserResponse userResponse = userService.patchUser(userId, userPatchRequest);

        assertThat(userResponse).isNotNull();
        assertThat(userResponse.id()).isEqualTo(userId);
        assertThat(userResponse.email()).isEqualTo(userPatchRequest.email());
        assertThat(userResponse.firstName()).isEqualTo(userPatchRequest.firstName());
        assertThat(userResponse.lastName()).isEqualTo(userPatchRequest.lastName());
        assertThat(userResponse.phoneNumber()).isEqualTo(userPatchRequest.phoneNumber());
        assertThat(userResponse.role()).isEqualTo(Role.CUSTOMER);
        assertThat(user.getPasswordHash()).isNotEqualTo(userPatchRequest.password());

        assertThat(user.getEmail())
                .isEqualTo(userPatchRequest.email());
        assertThat(user.getFirstName())
                .isEqualTo(userPatchRequest.firstName());
        assertThat(user.getLastName())
                .isEqualTo(userPatchRequest.lastName());
        assertThat(user.getPhoneNumber())
                .isEqualTo(userPatchRequest.phoneNumber());
        assertThat(user.getRole())
                .isEqualTo(Role.CUSTOMER);
        assertThat(user.getPasswordHash())
                .isEqualTo(ENCODED_UPDATE_PASSWORD);
        assertThat(user.getPasswordHash())
                .isNotEqualTo(userPatchRequest.password());

        verify(userRepository).findById(userId);
        verify(userRepository).findByEmail(userPatchRequest.email());
        verify(userRepository).findByPhoneNumber(userPatchRequest.phoneNumber());
        verify(passwordEncoder).encode(userPatchRequest.password());
        verifyNoMoreInteractions(userRepository);
        verifyNoMoreInteractions(passwordEncoder);
    }

    @Test
    void deleteUser_whenUserExists_deletesUser() {
        Long userId = 1L;
        User user = createDefaultUser(userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        userService.deleteUser(userId);

        verify(userRepository).findById(userId);
        verify(userRepository).delete(user);
        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void deleteUser_whenUserDoesNotExist_throwsNoResourceFoundException() {
        Long userId = 999L;

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        NoResourceFoundException ex = assertThrows(
                NoResourceFoundException.class,
                () -> userService.deleteUser(userId)
        );

        assertThat(ex.getMessage()).isEqualTo(userNotFoundWithId(userId));

        verify(userRepository).findById(userId);
        verify(userRepository, never()).delete(any(User.class));
        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(passwordEncoder);
    }
}
