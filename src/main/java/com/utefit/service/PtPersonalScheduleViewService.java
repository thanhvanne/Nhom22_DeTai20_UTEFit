package com.utefit.service;

import com.utefit.entity.User;
import com.utefit.enums.UserRole;
import com.utefit.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PtPersonalScheduleViewService {
    private final UserRepository users;

    @Transactional(readOnly = true)
    public Long trainerUserId(String email) {
        User u = users.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản PT."));
        if (u.getRole() != UserRole.PT) throw new IllegalArgumentException("Không phải tài khoản PT.");
        return u.getId();
    }

    @Transactional(readOnly = true)
    public User member(Long id) {
        User u = users.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Member không tồn tại."));
        if (u.getRole() != UserRole.MEMBER) throw new IllegalArgumentException("Không phải Member.");
        return u;
    }
}
