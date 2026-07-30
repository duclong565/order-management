package com.example.order_management.service;

import com.example.order_management.dto.ChangePasswordRequest;
import com.example.order_management.dto.CreateUserRequest;
import com.example.order_management.dto.UserResponse;
import com.example.order_management.entity.User;
import com.example.order_management.common.ErrorCode;
import com.example.order_management.exception.ApplicationException;
import com.example.order_management.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserResponse createUser(CreateUserRequest request) {

        if (userRepository.existsByEmail(request.email())) {
            throw new ApplicationException(ErrorCode.EMAIL_EXISTED);
        }

        if (userRepository.existsByUsername(request.username())) {
            throw new ApplicationException(ErrorCode.USERNAME_EXISTED);
        }

        User user = new User();
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setEmail(request.email());
        user.setRole(request.role());

        User savedUser = userRepository.save(user);

        return toResponse(savedUser);
    }

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream().map(this::toResponse).toList();
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.getCreatedAt()
        );
    }

    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(request.oldPassword(), user.getPassword())) {
            throw new ApplicationException(ErrorCode.WRONG_PASSWORD);
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
    }
}
