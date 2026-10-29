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
    @org.springframework.beans.factory.annotation.Autowired private com.utefit.repository.TrainerRepository trainers;
    @org.springframework.beans.factory.annotation.Autowired private com.utefit.repository.TrainerRequestRepository requests;
    public User memberForTrainer(String email, Long id) {
        var trainer = trainers.findByUserId(trainerUserId(email)).orElseThrow();
        if (!requests.existsByMember_IdAndTrainer_IdAndStatus(id, trainer.getId(), com.utefit.enums.TrainerRequestStatus.ACCEPTED))
            throw new org.springframework.security.access.AccessDeniedException("Hội viên không thuộc phạm vi huấn luyện của bạn.");
        return member(id);
    }

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
