package com.utefit.service.impl;

import com.utefit.dto.admin.AdminUserResponse;
import com.utefit.entity.User;
import com.utefit.enums.UserRole;
import com.utefit.enums.UserStatus;
import com.utefit.repository.UserRepository;
import com.utefit.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<AdminUserResponse> getUsers(String keyword, UserRole role, UserStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        String searchPattern = (keyword != null && !keyword.trim().isEmpty())
                ? "%" + keyword.trim().toLowerCase() + "%"
                : null;
        return userRepository.searchUsers(searchPattern, role, status, pageable)
                .map(AdminUserResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminUserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng với ID: " + id));
        return AdminUserResponse.fromEntity(user);
    }

    @Override
    @Transactional
    public void toggleLockUser(Long userId, String currentAdminEmail) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng với ID: " + userId));

        // Business Rule: Không cho phép Admin tự khóa tài khoản của chính mình
        if (currentAdminEmail != null && user.getEmail().equalsIgnoreCase(currentAdminEmail)) {
            throw new IllegalStateException("Bạn không thể tự khóa tài khoản Admin đang đăng nhập!");
        }

        if (user.getStatus() == UserStatus.LOCKED) {
            user.setStatus(UserStatus.ACTIVE);
            user.setEnabled(true);
        } else {
            user.setStatus(UserStatus.LOCKED);
            user.setEnabled(false);
        }

        userRepository.save(user);
    }
}