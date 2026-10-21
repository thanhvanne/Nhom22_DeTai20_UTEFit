package com.utefit.service;

import com.utefit.entity.User;
import com.utefit.enums.UserRole;
import com.utefit.enums.UserStatus;
import com.utefit.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberAccountService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Long requireMemberId(String email) {
        if (email == null || email.isBlank()) {
            throw new AccessDeniedException("Bạn cần đăng nhập bằng tài khoản Member.");
        }
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AccessDeniedException("Tài khoản không hợp lệ."));
        if (user.getRole() != UserRole.MEMBER
                || user.getStatus() != UserStatus.ACTIVE
                || !Boolean.TRUE.equals(user.getEnabled())) {
            throw new AccessDeniedException("Tài khoản không có quyền Member.");
        }
        return user.getId();
    }
}