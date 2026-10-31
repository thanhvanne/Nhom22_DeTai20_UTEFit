package com.utefit.service;

import com.utefit.dto.admin.ClassScheduleFormDto;
import com.utefit.entity.*;
import com.utefit.repository.CheckinRepository;
import com.utefit.repository.FitnessClassRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ManagerOperationsService {
    private final ManagerAccessService access;
    private final AdminCheckinService checkins;
    private final QrCheckinService qr;
    private final AdminScheduleService schedules;
    private final CheckinRepository history;
    private final FitnessClassRepository classes;
    private final EntityManager entityManager;

    public Page<Checkin> history(String email, Long branchId, int page) {
        access.requireBranch(email, branchId, false);
        return checkins.history(null, branchId, null, null, page);
    }

    public Page<ClassSchedule> schedules(String email, Long branchId, LocalDate date, int page) {
        access.requireBranch(email, branchId, false);
        return schedules.getSchedules(null, branchId, date, null, page, 10);
    }

    public List<FitnessClass> classes(String email, Long branchId) {
        access.requireBranch(email, branchId, false);
        return schedules.getSchedulableClasses().stream().filter(fitnessClass ->
                fitnessClass.getBranch().getId().equals(branchId)).toList();
    }

    @Transactional
    public void checkIn(String email, Long branchId, String memberEmail) {
        access.requireBranch(email, branchId, true);
        checkins.checkIn(memberEmail, branchId);
    }

    @Transactional
    public QrCheckinService.ScanResult scan(String email, Long branchId, String value) {
        access.requireBranch(email, branchId, true);
        return qr.consume(value == null ? null : value.trim(), branchId);
    }

    @Transactional
    public void checkOut(String email, Long branchId, Long checkinId) {
        access.requireBranch(email, branchId, true);
        Checkin checkin = history.findById(checkinId).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy check-in."));
        requireSameBranch(branchId, checkin.getBranch().getId());
        checkins.checkOut(checkinId);
    }

    public ClassScheduleFormDto editForm(String email, Long branchId, Long id) {
        access.requireBranch(email, branchId, false);
        requireSameBranch(branchId, schedules.getScheduleById(id).getFitnessClass().getBranch().getId());
        return schedules.getScheduleFormById(id);
    }

    @Transactional
    public void saveSchedule(String email, Long branchId, Long id, ClassScheduleFormDto form) {
        com.utefit.validation.FormChecks.validate(form);
        access.requireBranch(email, branchId, true);
        if (id != null) {
            requireSameBranch(branchId, lockedScheduleBranch(id));
        }
        FitnessClass fitnessClass = classes.findById(form.getFitnessClassId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lớp."));
        entityManager.lock(fitnessClass, LockModeType.PESSIMISTIC_WRITE);
        entityManager.refresh(fitnessClass);
        requireSameBranch(branchId, fitnessClass.getBranch().getId());
        if (id == null) {
            schedules.createSchedule(form);
        } else {
            schedules.updateSchedule(id, form);
        }
    }

    @Transactional
    public void toggleSchedule(String email, Long branchId, Long id) {
        access.requireBranch(email, branchId, true);
        requireSameBranch(branchId, lockedScheduleBranch(id));
        schedules.toggleActive(id);
    }

    private Long lockedScheduleBranch(Long id) {
        ClassSchedule schedule = entityManager.find(ClassSchedule.class, id, LockModeType.PESSIMISTIC_WRITE);
        if (schedule == null) {
            throw new IllegalArgumentException("Không tìm thấy lịch.");
        }
        entityManager.refresh(schedule);
        FitnessClass fitnessClass = schedule.getFitnessClass();
        entityManager.lock(fitnessClass, LockModeType.PESSIMISTIC_WRITE);
        entityManager.refresh(fitnessClass);
        return fitnessClass.getBranch().getId();
    }

    private void requireSameBranch(Long expected, Long actual) {
        if (!expected.equals(actual)) {
            throw new AccessDeniedException("Bản ghi không thuộc chi nhánh được phân công.");
        }
    }
}
