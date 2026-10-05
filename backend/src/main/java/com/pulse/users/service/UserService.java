package com.pulse.users.service;

import com.pulse.common.exception.ApiException;
import com.pulse.common.exception.ErrorCode;
import com.pulse.users.dto.UpdateProfileRequest;
import com.pulse.users.dto.UserResponse;
import com.pulse.users.entity.User;
import com.pulse.users.entity.UserStatus;
import com.pulse.users.mapper.UserMapper;
import com.pulse.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public UserResponse getById(UUID id) {
        return userMapper.toResponse(findUserOrThrow(id));
    }

    @Transactional
    public UserResponse updateProfile(UUID id, UpdateProfileRequest request) {
        User user = findUserOrThrow(id);

        if (request.displayName() != null) {
            user.setDisplayName(request.displayName());
        }
        if (request.avatarUrl() != null) {
            user.setAvatarUrl(request.avatarUrl());
        }

        return userMapper.toResponse(userRepository.save(user));
    }

    @Transactional
    public void updateStatus(UUID id, UserStatus status) {
        User user = findUserOrThrow(id);
        user.setStatus(status);
        if (status == UserStatus.OFFLINE) {
            user.setLastSeenAt(Instant.now());
        }
        userRepository.save(user);
    }

    private User findUserOrThrow(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound(ErrorCode.USER_NOT_FOUND, "User not found"));
    }
}
