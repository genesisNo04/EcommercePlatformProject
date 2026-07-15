package com.namnguyen.ecommerce_platform.user;

import com.namnguyen.ecommerce_platform.common.exception.*;
import com.namnguyen.ecommerce_platform.user.dto.*;
import com.namnguyen.ecommerce_platform.user.entity.User;
import com.namnguyen.ecommerce_platform.user.enums.Role;
import com.namnguyen.ecommerce_platform.user.repository.UserRepository;
import com.namnguyen.ecommerce_platform.user.service.UserServiceImpl;
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

    private static final String NO_RESOURCE_FOUND_EXCEPTION_MESSAGE = "User not found with id: ";
    private static final String DUPLICATION_RESOURCE_EXCEPTION_EMAIL_MESSAGE = "Email already exists";
    private static final String DUPLICATION_RESOURCE_EXCEPTION_PHONE_NUMBER_MESSAGE = "Phone number already exists";

    private User createUser(
            Long userId,
            String email,
            String password,
            String firstName,
            String lastName,
            String phoneNumber,
            Role role
    ) {
        User user = new User();
        user.setId(userId);
        user.setEmail(email);
        user.setPasswordHash(password);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setPhoneNumber(phoneNumber);
        user.setRole(role);

        return user;
    }

    @Test
    void createCustomerUser_whenRequestIsValid_savesCustomerAndReturnsUserResponse() {
        Long userId = 1L;

        UserCreateRequest request = new UserCreateRequest(
                "test@gmail.com",
                "test123",
                "test",
                "user",
                "1234567890"
        );

        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(userRepository.existsByPhoneNumber(request.phoneNumber())).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn("encodedPassword");

        when(userRepository.save(any(User.class))).thenAnswer(
                inv -> {
                    User savedUser = inv.getArgument(0);
                    savedUser.setId(userId);
                    return savedUser;
                }
        );

        UserResponse response = userService.createUser(request);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(userId);
        assertThat(response.email()).isEqualTo(request.email());
        assertThat(response.firstName()).isEqualTo(request.firstName());
        assertThat(response.lastName()).isEqualTo(request.lastName());
        assertThat(response.phoneNumber()).isEqualTo(request.phoneNumber());
        assertThat(response.role()).isEqualTo(Role.CUSTOMER);

        ArgumentCaptor<User> argumentCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(argumentCaptor.capture());

        User savedUser = argumentCaptor.getValue();

        assertThat(savedUser.getId()).isEqualTo(userId);
        assertThat(savedUser.getEmail()).isEqualTo(request.email());
        assertThat(savedUser.getFirstName()).isEqualTo(request.firstName());
        assertThat(savedUser.getLastName()).isEqualTo(request.lastName());
        assertThat(savedUser.getPhoneNumber()).isEqualTo(request.phoneNumber());
        assertThat(savedUser.getRole()).isEqualTo(Role.CUSTOMER);
        assertThat(savedUser.getPasswordHash()).isEqualTo("encodedPassword");
        assertThat(savedUser.getPasswordHash()).isNotEqualTo(request.password());

        verify(userRepository).existsByEmail(request.email());
        verify(userRepository).existsByPhoneNumber(request.phoneNumber());
        verifyNoMoreInteractions(userRepository);

        verify(passwordEncoder).encode(request.password());
        verifyNoMoreInteractions(passwordEncoder);
    }

    @Test
    void createAdmin_whenRequestIsValid_savesAdminAndReturnsUserResponse() {
        Long userId = 1L;

        UserCreateRequest request = new UserCreateRequest(
                "test@gmail.com",
                "test123",
                "test",
                "user",
                "1234567890"
        );

        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(userRepository.existsByPhoneNumber(request.phoneNumber())).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn("encodedPassword");

        when(userRepository.save(any(User.class))).thenAnswer(
                inv -> {
                    User savedUser = inv.getArgument(0);
                    savedUser.setId(userId);
                    return savedUser;
                }
        );

        UserResponse response = userService.createAdminUser(request);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(userId);
        assertThat(response.email()).isEqualTo(request.email());
        assertThat(response.firstName()).isEqualTo(request.firstName());
        assertThat(response.lastName()).isEqualTo(request.lastName());
        assertThat(response.phoneNumber()).isEqualTo(request.phoneNumber());
        assertThat(response.role()).isEqualTo(Role.ADMIN);

        ArgumentCaptor<User> argumentCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(argumentCaptor.capture());

        User savedUser = argumentCaptor.getValue();

        assertThat(savedUser.getId()).isEqualTo(userId);
        assertThat(savedUser.getEmail()).isEqualTo(request.email());
        assertThat(savedUser.getFirstName()).isEqualTo(request.firstName());
        assertThat(savedUser.getLastName()).isEqualTo(request.lastName());
        assertThat(savedUser.getPhoneNumber()).isEqualTo(request.phoneNumber());
        assertThat(savedUser.getRole()).isEqualTo(Role.ADMIN);
        assertThat(savedUser.getPasswordHash()).isEqualTo("encodedPassword");
        assertThat(savedUser.getPasswordHash()).isNotEqualTo(request.password());

        verify(userRepository).existsByEmail(request.email());
        verify(userRepository).existsByPhoneNumber(request.phoneNumber());
        verifyNoMoreInteractions(userRepository);

        verify(passwordEncoder).encode(request.password());
        verifyNoMoreInteractions(passwordEncoder);
    }

    @Test
    void createUser_whenEmailAlreadyExists_throwsDuplicateResourceException() {
        Long userId = 1L;

        UserCreateRequest request = new UserCreateRequest(
                "test@gmail.com",
                "test123",
                "test",
                "user",
                "1234567890"
        );

        when(userRepository.existsByEmail(request.email())).thenReturn(true);

        DuplicateResourceException ex = assertThrows(
                DuplicateResourceException.class,
                () -> userService.createUser(request)
        );

        assertThat(ex).isNotNull();
        assertThat(ex.getMessage()).isEqualTo(DUPLICATION_RESOURCE_EXCEPTION_EMAIL_MESSAGE);

        verify(userRepository).existsByEmail(request.email());
        verify(userRepository, never()).existsByPhoneNumber(request.phoneNumber());
        verify(userRepository, never()).save(any(User.class));
        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void createUser_whenRequestHasDuplicatePhoneNumber_returnsDuplicateResourceExceptionResponse() {
        Long userId = 1L;

        UserCreateRequest request = new UserCreateRequest(
                "test@gmail.com",
                "test123",
                "test",
                "user",
                "1234567890"
        );

        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(userRepository.existsByPhoneNumber(request.phoneNumber())).thenReturn(true);

        DuplicateResourceException ex = assertThrows(
                DuplicateResourceException.class,
                () -> userService.createUser(request)
        );

        assertThat(ex).isNotNull();
        assertThat(ex.getMessage()).isEqualTo(DUPLICATION_RESOURCE_EXCEPTION_PHONE_NUMBER_MESSAGE);

        verify(userRepository).existsByEmail(request.email());
        verify(userRepository).existsByPhoneNumber(request.phoneNumber());
        verify(userRepository, never()).save(any(User.class));
        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void getUserById_whenUserExists_returnsUserResponse() {
        Long userId = 1L;
        User user = createUser(
                userId,
                "test@gmail.com",
                "test123",
                "test",
                "user",
                "1234567890",
                Role.CUSTOMER
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        UserResponse response = userService.getUserById(userId);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(userId);
        assertThat(response.email()).isEqualTo(user.getEmail());
        assertThat(response.phoneNumber()).isEqualTo(user.getPhoneNumber());
        assertThat(response.firstName()).isEqualTo(user.getFirstName());
        assertThat(response.lastName()).isEqualTo(user.getLastName());
        assertThat(response.role()).isEqualTo(user.getRole());

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
        assertThat(ex.getMessage()).isEqualTo(NO_RESOURCE_FOUND_EXCEPTION_MESSAGE + userId);

        verify(userRepository).findById(userId);
        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void getAllUsers_whenUsersExist_returnsPagedUserResponses() {
        Long userId = 1L;
        User user = createUser(
                userId,
                "test@gmail.com",
                "test123",
                "test",
                "user",
                "1234567890",
                Role.CUSTOMER
        );

        Long userId1 = 2L;
        User user1 = createUser(
                userId1,
                "test1@gmail.com",
                "test1234",
                "test1",
                "user1",
                "1234567891",
                Role.CUSTOMER
        );

        UserFilterRequest request = new UserFilterRequest(null, null, null);

        List<User> users = List.of(user, user1);
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> pageUsers = new PageImpl<>(users, pageable, users.size());

        when(userRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(pageUsers);

        Page<UserResponse> responsePage = userService.getAllUsers(request, pageable);

        assertThat(responsePage).isNotNull();
        assertThat(responsePage.getTotalElements()).isEqualTo(2);
        assertThat(responsePage.getNumberOfElements()).isEqualTo(2);
        assertThat(responsePage.getTotalPages()).isEqualTo(1);
        assertThat(responsePage.getSize()).isEqualTo(10);
        assertThat(responsePage.getNumber()).isEqualTo(0);

        assertThat(responsePage.getContent()).hasSize(2);

        UserResponse firstUser = responsePage.getContent().getFirst();
        assertThat(firstUser.id()).isEqualTo(userId);
        assertThat(firstUser.email()).isEqualTo(user.getEmail());
        assertThat(firstUser.role()).isEqualTo(user.getRole());

        UserResponse secondUser = responsePage.getContent().get(1);
        assertThat(secondUser.id()).isEqualTo(userId1);
        assertThat(secondUser.email()).isEqualTo(user1.getEmail());
        assertThat(secondUser.role()).isEqualTo(user1.getRole());

        verify(userRepository).findAll(any(Specification.class), eq(pageable));
        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void getAllUsers_whenNoUsersExist_returnsPagedUserResponses() {
        UserFilterRequest request = new UserFilterRequest(null, null, null);

        List<User> users = List.of();
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> pageUsers = new PageImpl<>(users, pageable, 0);

        when(userRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(pageUsers);

        Page<UserResponse> responsePage = userService.getAllUsers(request, pageable);

        assertThat(responsePage).isNotNull();
        assertThat(responsePage.getTotalElements()).isEqualTo(0);
        assertThat(responsePage.getNumberOfElements()).isEqualTo(0);
        assertThat(responsePage.getTotalPages()).isEqualTo(0);
        assertThat(responsePage.getSize()).isEqualTo(10);
        assertThat(responsePage.getNumber()).isEqualTo(0);

        verify(userRepository).findAll(any(Specification.class), eq(pageable));
        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void putUser_whenRequestIsValid_savesCustomerAndReturnsUserResponse() {
        Long userId = 1L;
        User user = createUser(
                userId,
                "test@gmail.com",
                "test123",
                "test",
                "user",
                "1234567890",
                Role.CUSTOMER
        );

        UserPutRequest request = new UserPutRequest(
                "testupdate@gmail.com",
                "test123",
                "testupdate",
                "userupdate",
                "1234567891"
        );

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.empty());
        when(userRepository.findByPhoneNumber(request.phoneNumber())).thenReturn(Optional.empty());
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode(request.password())).thenReturn("encodedPasswordUpdate");

        UserResponse response = userService.putUser(userId, request);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(userId);
        assertThat(response.email()).isEqualTo(request.email());
        assertThat(response.firstName()).isEqualTo(request.firstName());
        assertThat(response.lastName()).isEqualTo(request.lastName());
        assertThat(response.phoneNumber()).isEqualTo(request.phoneNumber());
        assertThat(response.role()).isEqualTo(Role.CUSTOMER);
        assertThat(user.getPasswordHash()).isEqualTo("encodedPasswordUpdate");
        assertThat(user.getPasswordHash()).isNotEqualTo(request.password());

        verify(userRepository).findByEmail(request.email());
        verify(userRepository).findByPhoneNumber(request.phoneNumber());
        verify(userRepository).findById(userId);
        verifyNoMoreInteractions(userRepository);

        verify(passwordEncoder).encode(request.password());
        verifyNoMoreInteractions(passwordEncoder);
    }

    @Test
    void putUser_whenUserDoesNotExist_throwsNoResourceFoundException() {
        Long userId = 999L;

        UserPutRequest request = new UserPutRequest(
                "testupdate@gmail.com",
                "test123",
                "testupdate",
                "userupdate",
                "1234567891"
        );

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        NoResourceFoundException ex = assertThrows(
                NoResourceFoundException.class,
                () -> userService.putUser(userId, request)
        );

        assertThat(ex.getMessage()).isEqualTo(NO_RESOURCE_FOUND_EXCEPTION_MESSAGE + userId);

        verify(userRepository).findById(userId);
        verify(userRepository, never()).findByEmail(anyString());
        verify(userRepository, never()).findByPhoneNumber(anyString());
        verifyNoMoreInteractions(userRepository);
        verifyNoMoreInteractions(passwordEncoder);
    }

    @Test
    void putUser_whenEmailIsDuplicated_throwDuplicateResourceException() {
        Long userId = 1L;
        User user = createUser(
                userId,
                "test@gmail.com",
                "test123",
                "test",
                "user",
                "1234567890",
                Role.CUSTOMER
        );

        Long userId2 = 2L;
        User user1 = createUser(
                userId2,
                "testupdate@gmail.com",
                "test123",
                "test",
                "user",
                "1234567890",
                Role.CUSTOMER
        );

        UserPutRequest request = new UserPutRequest(
                "testupdate@gmail.com",
                "test123",
                "testupdate",
                "userupdate",
                "1234567891"
        );

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(user1));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        DuplicateResourceException ex = assertThrows(
                DuplicateResourceException.class,
                () -> userService.putUser(userId, request)
        );

        assertThat(ex).isNotNull();
        assertThat(ex.getMessage()).isEqualTo(DUPLICATION_RESOURCE_EXCEPTION_EMAIL_MESSAGE);

        verify(userRepository).findById(userId);
        verify(userRepository).findByEmail(request.email());
        verifyNoMoreInteractions(userRepository);
        verifyNoMoreInteractions(passwordEncoder);
    }

    @Test
    void putUser_whenPhoneNumberIsDuplicated_throwDuplicateResourceException() {
        Long userId = 1L;
        User user = createUser(
                userId,
                "test@gmail.com",
                "test123",
                "test",
                "user",
                "1234567890",
                Role.CUSTOMER
        );

        Long userId2 = 2L;
        User user1 = createUser(
                userId2,
                "testupdate1@gmail.com",
                "test123",
                "test",
                "user",
                "1234567891",
                Role.CUSTOMER
        );

        UserPutRequest request = new UserPutRequest(
                "testupdate@gmail.com",
                "test123",
                "testupdate",
                "userupdate",
                "1234567891"
        );

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.empty());
        when(userRepository.findByPhoneNumber(request.phoneNumber())).thenReturn(Optional.of(user1));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        DuplicateResourceException ex = assertThrows(
                DuplicateResourceException.class,
                () -> userService.putUser(userId, request)
        );

        assertThat(ex).isNotNull();
        assertThat(ex.getMessage()).isEqualTo(DUPLICATION_RESOURCE_EXCEPTION_PHONE_NUMBER_MESSAGE);

        verify(userRepository).findById(userId);
        verify(userRepository).findByEmail(request.email());
        verify(userRepository).findByPhoneNumber(request.phoneNumber());
        verifyNoMoreInteractions(userRepository);
        verifyNoMoreInteractions(passwordEncoder);
    }

    @Test
    void putUser_whenUserKeepSameEmail_savesCustomerAndReturnsUserResponse() {
        Long userId = 1L;
        User user = createUser(
                userId,
                "test@gmail.com",
                "test123",
                "test",
                "user",
                "1234567890",
                Role.CUSTOMER
        );

        UserPutRequest request = new UserPutRequest(
                "test@gmail.com",
                "test123",
                "testupdate",
                "userupdate",
                "1234567891"
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(user));
        when(userRepository.findByPhoneNumber(request.phoneNumber())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(request.password())).thenReturn("encodedPasswordUpdate");

        UserResponse response = userService.putUser(userId, request);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(userId);
        assertThat(response.email()).isEqualTo(request.email());
        assertThat(response.firstName()).isEqualTo(request.firstName());
        assertThat(response.lastName()).isEqualTo(request.lastName());
        assertThat(response.phoneNumber()).isEqualTo(request.phoneNumber());
        assertThat(response.role()).isEqualTo(Role.CUSTOMER);
        assertThat(user.getPasswordHash()).isEqualTo("encodedPasswordUpdate");
        assertThat(user.getPasswordHash()).isNotEqualTo(request.password());

        verify(userRepository).findById(userId);
        verify(userRepository).findByEmail(request.email());
        verify(userRepository).findByPhoneNumber(request.phoneNumber());
        verify(passwordEncoder).encode(request.password());
        verifyNoMoreInteractions(userRepository);
        verifyNoMoreInteractions(passwordEncoder);
    }

    @Test
    void putUser_whenUserKeepSamePhoneNumber_savesCustomerAndReturnsUserResponse() {
        Long userId = 1L;
        User user = createUser(
                userId,
                "test@gmail.com",
                "test123",
                "test",
                "user",
                "1234567890",
                Role.CUSTOMER
        );

        UserPutRequest request = new UserPutRequest(
                "testupdate@gmail.com",
                "test123",
                "testupdate",
                "userupdate",
                "1234567890"
        );

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.empty());
        when(userRepository.findByPhoneNumber(request.phoneNumber())).thenReturn(Optional.of(user));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode(request.password())).thenReturn("encodedPasswordUpdate");

        UserResponse response = userService.putUser(userId, request);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(userId);
        assertThat(response.email()).isEqualTo(request.email());
        assertThat(response.firstName()).isEqualTo(request.firstName());
        assertThat(response.lastName()).isEqualTo(request.lastName());
        assertThat(response.phoneNumber()).isEqualTo(request.phoneNumber());
        assertThat(response.role()).isEqualTo(Role.CUSTOMER);
        assertThat(user.getPasswordHash()).isEqualTo("encodedPasswordUpdate");
        assertThat(user.getPasswordHash()).isNotEqualTo(request.password());

        verify(userRepository).findById(userId);
        verify(userRepository).findByEmail(request.email());
        verify(userRepository).findByPhoneNumber(request.phoneNumber());
        verify(passwordEncoder).encode(request.password());
        verifyNoMoreInteractions(userRepository);
        verifyNoMoreInteractions(passwordEncoder);
    }

    @Test
    void patchUser_whenRequestIsValid_savesCustomerAndReturnsUserResponse() {
        Long userId = 1L;
        User user = createUser(
                userId,
                "test@gmail.com",
                "test123",
                "test",
                "user",
                "1234567890",
                Role.CUSTOMER
        );

        UserPatchRequest request = new UserPatchRequest(
                "testupdate@gmail.com",
                "test123update",
                "testupdate",
                "userupdate",
                "1234567891"
        );

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.empty());
        when(userRepository.findByPhoneNumber(request.phoneNumber())).thenReturn(Optional.empty());
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode(request.password())).thenReturn("encodedPasswordUpdate");

        UserResponse response = userService.patchUser(userId, request);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(userId);
        assertThat(response.email()).isEqualTo(request.email());
        assertThat(response.firstName()).isEqualTo(request.firstName());
        assertThat(response.lastName()).isEqualTo(request.lastName());
        assertThat(response.phoneNumber()).isEqualTo(request.phoneNumber());
        assertThat(response.role()).isEqualTo(Role.CUSTOMER);

        verify(userRepository).findByEmail(request.email());
        verify(userRepository).findByPhoneNumber(request.phoneNumber());
        verify(userRepository).findById(userId);
        verifyNoMoreInteractions(userRepository);

        verify(passwordEncoder).encode(request.password());
        verifyNoMoreInteractions(passwordEncoder);
    }

    @Test
    void patchUser_whenUserDoesNotExist_throwsNoResourceFoundException() {
        Long userId = 999L;

        UserPatchRequest request = new UserPatchRequest(
                "testupdate@gmail.com",
                "test123",
                "testupdate",
                "userupdate",
                "1234567891"
        );

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        NoResourceFoundException ex = assertThrows(
                NoResourceFoundException.class,
                () -> userService.patchUser(userId, request)
        );

        assertThat(ex.getMessage()).isEqualTo(NO_RESOURCE_FOUND_EXCEPTION_MESSAGE + userId);

        verify(userRepository).findById(userId);
        verify(userRepository, never()).findByEmail(anyString());
        verify(userRepository, never()).findByPhoneNumber(anyString());
        verifyNoMoreInteractions(userRepository);
        verifyNoMoreInteractions(passwordEncoder);
    }

    @Test
    void patchUser_whenRequestIsValidPartiallyUpdate_savesCustomerAndReturnsUserResponse() {
        Long userId = 1L;
        User user = createUser(
                userId,
                "test@gmail.com",
                "test123",
                "test",
                "user",
                "1234567890",
                Role.CUSTOMER
        );

        UserPatchRequest request = new UserPatchRequest(
                "testupdate@gmail.com",
                null,
                null,
                null,
                "1234567891"
        );

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.empty());
        when(userRepository.findByPhoneNumber(request.phoneNumber())).thenReturn(Optional.empty());
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        UserResponse response = userService.patchUser(userId, request);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(userId);
        assertThat(response.email()).isEqualTo(request.email());
        assertThat(response.firstName()).isEqualTo("test");
        assertThat(response.lastName()).isEqualTo("user");
        assertThat(response.phoneNumber()).isEqualTo(request.phoneNumber());
        assertThat(response.role()).isEqualTo(Role.CUSTOMER);

        verify(userRepository).findByEmail(request.email());
        verify(userRepository).findByPhoneNumber(request.phoneNumber());
        verify(userRepository).findById(userId);
        verifyNoMoreInteractions(userRepository);
        verifyNoMoreInteractions(passwordEncoder);
    }

    @Test
    void patchUser_whenAllFieldsAreNull_keepsExistingUserUnchanged() {
        Long userId = 1L;
        User user = createUser(
                userId,
                "test@gmail.com",
                "test123",
                "test",
                "user",
                "1234567890",
                Role.CUSTOMER
        );

        UserPatchRequest request = new UserPatchRequest(
                null,
                null,
                null,
                null,
                null
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        UserResponse response = userService.patchUser(userId, request);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(userId);
        assertThat(response.email()).isEqualTo(user.getEmail());
        assertThat(response.firstName()).isEqualTo(user.getFirstName());
        assertThat(response.lastName()).isEqualTo(user.getLastName());
        assertThat(response.phoneNumber()).isEqualTo(user.getPhoneNumber());
        assertThat(response.role()).isEqualTo(user.getRole());

        verify(userRepository).findById(userId);
        verify(userRepository, never()).findByEmail(any());
        verify(userRepository, never()).findByPhoneNumber(any());
        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void patchUser_whenEmailIsDuplicated_throwDuplicateResourceException() {
        Long userId = 1L;
        User user = createUser(
                userId,
                "test@gmail.com",
                "test123",
                "test",
                "user",
                "1234567890",
                Role.CUSTOMER
        );

        Long userId2 = 2L;
        User user1 = createUser(
                userId2,
                "testupdate@gmail.com",
                "test123",
                "test",
                "user",
                "1234567890",
                Role.CUSTOMER
        );

        UserPatchRequest request = new UserPatchRequest(
                "testupdate@gmail.com",
                "test123",
                "testupdate",
                "userupdate",
                "1234567891"
        );

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(user1));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        DuplicateResourceException ex = assertThrows(
                DuplicateResourceException.class,
                () -> userService.patchUser(userId, request)
        );

        assertThat(ex).isNotNull();
        assertThat(ex.getMessage()).isEqualTo(DUPLICATION_RESOURCE_EXCEPTION_EMAIL_MESSAGE);

        verify(userRepository).findById(userId);
        verify(userRepository).findByEmail(request.email());
        verifyNoMoreInteractions(userRepository);
        verifyNoMoreInteractions(passwordEncoder);
    }

    @Test
    void patchUser_whenPhoneNumberIsDuplicated_throwDuplicateResourceException() {
        Long userId = 1L;
        User user = createUser(
                userId,
                "test@gmail.com",
                "test123",
                "test",
                "user",
                "1234567890",
                Role.CUSTOMER
        );

        Long userId2 = 2L;
        User user1 = createUser(
                userId2,
                "testupdate1@gmail.com",
                "test123",
                "test",
                "user",
                "1234567891",
                Role.CUSTOMER
        );

        UserPatchRequest request = new UserPatchRequest(
                "testupdate@gmail.com",
                "test123",
                "testupdate",
                "userupdate",
                "1234567891"
        );

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.empty());
        when(userRepository.findByPhoneNumber(request.phoneNumber())).thenReturn(Optional.of(user1));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        DuplicateResourceException ex = assertThrows(
                DuplicateResourceException.class,
                () -> userService.patchUser(userId, request)
        );

        assertThat(ex).isNotNull();
        assertThat(ex.getMessage()).isEqualTo(DUPLICATION_RESOURCE_EXCEPTION_PHONE_NUMBER_MESSAGE);

        verify(userRepository).findById(userId);
        verify(userRepository).findByEmail(request.email());
        verify(userRepository).findByPhoneNumber(request.phoneNumber());
        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void patchUser_whenUserKeepSameEmail_savesCustomerAndReturnsUserResponse() {
        Long userId = 1L;
        User user = createUser(
                userId,
                "test@gmail.com",
                "test123",
                "test",
                "user",
                "1234567890",
                Role.CUSTOMER
        );

        UserPatchRequest request = new UserPatchRequest(
                "test@gmail.com",
                "test123",
                "testupdate",
                "userupdate",
                "1234567891"
        );

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(user));
        when(userRepository.findByPhoneNumber(request.phoneNumber())).thenReturn(Optional.empty());
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode(request.password())).thenReturn("encodedPasswordUpdate");

        UserResponse response = userService.patchUser(userId, request);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(userId);
        assertThat(response.email()).isEqualTo(request.email());
        assertThat(response.firstName()).isEqualTo(request.firstName());
        assertThat(response.lastName()).isEqualTo(request.lastName());
        assertThat(response.phoneNumber()).isEqualTo(request.phoneNumber());
        assertThat(response.role()).isEqualTo(Role.CUSTOMER);
        assertThat(user.getPasswordHash()).isEqualTo("encodedPasswordUpdate");
        assertThat(user.getPasswordHash()).isNotEqualTo(request.password());

        verify(userRepository).findById(userId);
        verify(userRepository).findByEmail(request.email());
        verify(userRepository).findByPhoneNumber(request.phoneNumber());
        verify(passwordEncoder).encode(request.password());
        verifyNoMoreInteractions(userRepository);
        verifyNoMoreInteractions(passwordEncoder);
    }

    @Test
    void patchUser_whenUserKeepSamePhoneNumber_savesCustomerAndReturnsUserResponse() {
        Long userId = 1L;
        User user = createUser(
                userId,
                "test@gmail.com",
                "test123",
                "test",
                "user",
                "1234567890",
                Role.CUSTOMER
        );

        UserPatchRequest request = new UserPatchRequest(
                "testupdate@gmail.com",
                "test123",
                "testupdate",
                "userupdate",
                "1234567890"
        );

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.empty());
        when(userRepository.findByPhoneNumber(request.phoneNumber())).thenReturn(Optional.of(user));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode(request.password())).thenReturn("encodedPasswordUpdate");

        UserResponse response = userService.patchUser(userId, request);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(userId);
        assertThat(response.email()).isEqualTo(request.email());
        assertThat(response.firstName()).isEqualTo(request.firstName());
        assertThat(response.lastName()).isEqualTo(request.lastName());
        assertThat(response.phoneNumber()).isEqualTo(request.phoneNumber());
        assertThat(response.role()).isEqualTo(Role.CUSTOMER);
        assertThat(user.getPasswordHash()).isEqualTo("encodedPasswordUpdate");
        assertThat(user.getPasswordHash()).isNotEqualTo(request.password());

        verify(userRepository).findById(userId);
        verify(userRepository).findByEmail(request.email());
        verify(userRepository).findByPhoneNumber(request.phoneNumber());
        verify(passwordEncoder).encode(request.password());
        verifyNoMoreInteractions(userRepository);
        verifyNoMoreInteractions(passwordEncoder);
    }

    @Test
    void deleteUser_whenUserExists_deletesUser() {
        Long userId = 1L;
        User user = createUser(
                userId,
                "test@gmail.com",
                "test123",
                "test",
                "user",
                "1234567890",
                Role.CUSTOMER
        );

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

        assertThat(ex.getMessage()).isEqualTo(NO_RESOURCE_FOUND_EXCEPTION_MESSAGE + userId);

        verify(userRepository).findById(userId);
        verify(userRepository, never()).delete(any(User.class));
        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(passwordEncoder);
    }
}
