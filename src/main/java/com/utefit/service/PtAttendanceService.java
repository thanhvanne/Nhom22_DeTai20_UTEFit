package com.utefit.service;

import com.utefit.entity.ClassRegistration;
import com.utefit.entity.ClassSchedule;
import com.utefit.enums.ClassStatus;
import com.utefit.enums.RegistrationStatus;
import com.utefit.repository.ClassRegistrationRepository;
import com.utefit.repository.ClassScheduleRepository;
import com.utefit.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PtAttendanceService {
    private final UserRepository users;
    private final ClassScheduleRepository schedules;
    private final ClassRegistrationRepository registrations;

    @Transactional
    public void record(String ptEmail, Long scheduleId, Long registrationId, RegistrationStatus result) {
        if (!Set.of(RegistrationStatus.ATTENDED, RegistrationStatus.ABSENT).contains(result)) {
            throw new IllegalArgumentException("Chỉ ghi nhận ATTENDED hoặc ABSENT.");
        }
        Long trainerUserId = users.findByEmail(ptEmail)
                .orElseThrow(() -> new AccessDeniedException("Tài khoản PT không tồn tại."))
                .getId();
        ClassSchedule schedule = schedules.findDetailById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy buổi học."));
        if (schedule.getFitnessClass().getTrainer() == null
                || !schedule.getFitnessClass().getTrainer().getUser().getId().equals(trainerUserId)) {
            throw new AccessDeniedException("Bạn không được điểm danh lớp của PT khác.");
        }
        if (!Boolean.TRUE.equals(schedule.getActive())
                || schedule.getFitnessClass().getStatus() == ClassStatus.CANCELLED) {
            throw new IllegalStateException("Không điểm danh buổi đã hủy.");
        }
        LocalDateTime end = schedule.getScheduleDate().atTime(schedule.getEndTime());
        if (LocalDateTime.now().isBefore(end)) {
            throw new IllegalStateException("Chỉ điểm danh sau khi buổi học kết thúc.");
        }
        ClassRegistration registration = registrations.lockForAttendance(registrationId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đăng ký."));
        if (!registration.getSchedule().getId().equals(scheduleId)
                || registration.getStatus() != RegistrationStatus.REGISTERED) {
            throw new IllegalStateException("Đăng ký không thuộc buổi này hoặc đã được xử lý.");
        }
        registration.setStatus(result);
    }
}
