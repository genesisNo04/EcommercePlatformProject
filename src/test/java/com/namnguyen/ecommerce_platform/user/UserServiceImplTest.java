package com.namnguyen.ecommerce_platform.user;

import com.namnguyen.ecommerce_platform.common.exception.DuplicateResourceException;
import com.namnguyen.ecommerce_platform.common.exception.NoResourceFoundException;
import com.namnguyen.ecommerce_platform.user.dto.UserCreateRequest;
import com.namnguyen.ecommerce_platform.user.dto.UserFilterRequest;
import com.namnguyen.ecommerce_platform.user.dto.UserResponse;
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

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
    void createUser_whenRequestHasDuplicateEmail_returnsDuplicateResourceExceptionResponse() {
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


        verifyNoMoreInteractions(userRepository);
        verifyNoMoreInteractions(passwordEncoder);
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


        verifyNoMoreInteractions(userRepository);
        verifyNoMoreInteractions(passwordEncoder);
    }

    @Test
    void getUserById_whenUsersExist_returnUserResponse() {
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

        verifyNoMoreInteractions(userRepository);
    }

    @Test
    void getUserById_whenUsersNotExist_returnNoResourceFoundException() {
        Long userId = 999L;

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        NoResourceFoundException ex = assertThrows(
                NoResourceFoundException.class,
                () -> userService.getUserById(userId)
        );

        assertThat(ex).isNotNull();
        assertThat(ex.getMessage()).isEqualTo(NO_RESOURCE_FOUND_EXCEPTION_MESSAGE + userId);

        verifyNoMoreInteractions(userRepository);
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
                userId,
                "test@gmail.com",
                "test123",
                "test",
                "user",
                "1234567890",
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
    }
}
