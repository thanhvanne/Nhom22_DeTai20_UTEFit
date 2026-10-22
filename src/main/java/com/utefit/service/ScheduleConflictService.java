package com.utefit.service;

import com.utefit.entity.ClassRegistration;
import com.utefit.entity.ClassSchedule;
import com.utefit.entity.PersonalTrainingSchedule;
import com.utefit.enums.ClassStatus;
import com.utefit.enums.RegistrationStatus;
import com.utefit.repository.ClassRegistrationRepository;
import com.utefit.repository.ClassScheduleRepository;
import com.utefit.repository.PersonalTrainingScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ScheduleConflictService {
    private final ClassScheduleRepository classes;
    private final ClassRegistrationRepository registrations;
    private final PersonalTrainingScheduleRepository personal;

    private boolean overlaps(LocalTime a, LocalTime b, LocalTime x, LocalTime y) {
        return a.isBefore(y) && x.isBefore(b);
    }

    public void assertTrainerFree(Long trainerId, LocalDate date, LocalTime start,
                                  LocalTime end, Long excludedClassScheduleId) {
        for (ClassSchedule c : classes.findActiveTrainerSessions(trainerId, date,
                List.of(ClassStatus.CANCELLED, ClassStatus.COMPLETED))) {
            if (!c.getId().equals(excludedClassScheduleId)
                    && overlaps(start, end, c.getStartTime(), c.getEndTime())) {
                throw new IllegalStateException("PT đã có lịch lớp nhóm trùng giờ.");
            }
        }
        for (PersonalTrainingSchedule s : personal.findByTrainer_IdAndScheduleDateAndActiveTrue(trainerId, date)) {
            if (overlaps(start, end, s.getStartTime(), s.getEndTime())) {
                throw new IllegalStateException("PT đã có lịch cá nhân trùng giờ.");
            }
        }
    }

    public void assertMemberFree(Long memberId, LocalDate date, LocalTime start,
                                 LocalTime end, Long excludedClassScheduleId) {
        for (ClassRegistration reg : registrations.findOtherRegistrationsOnDate(
                memberId, RegistrationStatus.REGISTERED, date,
                excludedClassScheduleId == null ? -1L : excludedClassScheduleId)) {
            ClassSchedule c = reg.getSchedule();
            if (Boolean.TRUE.equals(c.getActive())
                    && c.getFitnessClass().getStatus() != ClassStatus.CANCELLED
                    && overlaps(start, end, c.getStartTime(), c.getEndTime())) {
                throw new IllegalStateException("Member đã đăng ký lớp nhóm trùng giờ.");
            }
        }
        for (PersonalTrainingSchedule p : personal.findByMember_IdAndScheduleDateAndActiveTrue(memberId, date)) {
            if (overlaps(start, end, p.getStartTime(), p.getEndTime())) {
                throw new IllegalStateException("Member đã có lịch PT cá nhân trùng giờ.");
            }
        }
    }

    /** Dùng khi PT tạo 1:1, không loại trừ lịch nào. */
    public void assertTrainerFreeForPersonal(Long trainerId, LocalDate date,
                                             LocalTime start, LocalTime end) {
        assertTrainerFree(trainerId, date, start, end, null);
    }
}
