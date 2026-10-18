package com.utefit.service;

import com.utefit.dto.admin.AdminUserResponse;
import com.utefit.enums.UserRole;
import com.utefit.enums.UserStatus;
import org.springframework.data.domain.Page;

public interface AdminUserService {
    Page<AdminUserResponse> getUsers(String keyword, UserRole role, UserStatus status, int page, int size);
    AdminUserResponse getUserById(Long id);
    void toggleLockUser(Long userId, String currentAdminEmail);
}