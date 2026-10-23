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
public class CurrentAccountService {
    private final UserRepository users;
    @Transactional(readOnly=true)
    public Long requireUserId(String email) {
        if (email==null || email.isBlank()) throw new AccessDeniedException("Bạn chưa đăng nhập.");
        User user=users.findByEmail(email).orElseThrow(()->new AccessDeniedException("Không tìm thấy tài khoản."));
        if (!Boolean.TRUE.equals(user.getEnabled()) || user.getStatus()!=UserStatus.ACTIVE)
            throw new AccessDeniedException("Tài khoản không hoạt động.");
        return user.getId();
    }
    @Transactional(readOnly=true)
    public Long requirePtUserId(String email) {
        if (email==null || email.isBlank()) throw new AccessDeniedException("Bạn chưa đăng nhập.");
        User user=users.findByEmail(email).orElseThrow(()->new AccessDeniedException("Không tìm thấy PT."));
        if (user.getRole()!=UserRole.PT || user.getStatus()!=UserStatus.ACTIVE
                || !Boolean.TRUE.equals(user.getEnabled()))
            throw new AccessDeniedException("Không phải tài khoản PT hợp lệ.");
        return user.getId();
    }
}
