package com.example.order_management.controller;

import com.example.order_management.common.BaseResponse;
import com.example.order_management.dto.ChangePasswordRequest;
import com.example.order_management.dto.CreateUserRequest;
import com.example.order_management.dto.UserResponse;
import com.example.order_management.security.CustomUserDetails;
import com.example.order_management.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<BaseResponse<UserResponse>> createUser(@Valid @RequestBody CreateUserRequest createUserRequest) {
        UserResponse userResponse = userService.createUser(createUserRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(BaseResponse.success(userResponse));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')") // tự thêm prefix ROLE_ ở trước -> ROLE_ADMIN
    public ResponseEntity<BaseResponse<List<UserResponse>>> getAllUsers() {
        List<UserResponse> userResponseList = userService.getAllUsers();
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(userResponseList));
    }

    @PatchMapping("/me/password")
    public ResponseEntity<BaseResponse<Void>> changePassword(
            @AuthenticationPrincipal CustomUserDetails principal, //Principal là chỉ object đang được xác thực/ danh tính
            @Valid @RequestBody ChangePasswordRequest request //@valid là để kích hoạt validation ở dto
            ) {
        userService.changePassword(principal.user().getId(), request);
        return ResponseEntity.ok(BaseResponse.success(null, "Password changed successfully"));
    }
}
