package com.namnguyen.ecommerce_platform.user.controller;

import com.namnguyen.ecommerce_platform.user.dto.UserCreateRequest;
import com.namnguyen.ecommerce_platform.user.dto.UserPatchRequest;
import com.namnguyen.ecommerce_platform.user.dto.UserPutRequest;
import com.namnguyen.ecommerce_platform.user.dto.UserResponse;
import com.namnguyen.ecommerce_platform.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserCreateRequest userCreateRequest) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(userService.createUser(userCreateRequest));
    }

    @GetMapping
    public ResponseEntity<Page<UserResponse>> getAllUsers(
            @PageableDefault(size = 10, page = 0, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(userService.getAllUsers(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> putUser(@PathVariable Long id, @Valid @RequestBody UserPutRequest userPutRequest) {
        return ResponseEntity
                .ok(userService.putUser(id, userPutRequest));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<UserResponse> patchUser(@PathVariable Long id, @Valid @RequestBody UserPatchRequest userPatchRequest) {
        return ResponseEntity
                .ok(userService.patchUser(id, userPatchRequest));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
