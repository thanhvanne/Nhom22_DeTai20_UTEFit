package com.utefit.service.impl;

import com.utefit.dto.trainer.PtClassMemberRegistrationResponse;
import com.utefit.dto.trainer.PtClassScheduleDetailResponse;
import com.utefit.dto.trainer.PtClassScheduleSummaryResponse;
import com.utefit.entity.ClassRegistration;
import com.utefit.entity.ClassSchedule;
import com.utefit.entity.FitnessClass;
import com.utefit.entity.Trainer;
import com.utefit.entity.User;
import com.utefit.enums.ClassStatus;
import com.utefit.enums.RegistrationStatus;
import com.utefit.repository.ClassRegistrationRepository;
import com.utefit.repository.ClassScheduleRepository;
import com.utefit.service.PtClassService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class PtClassServiceImpl implements PtClassService {

    private final ClassScheduleRepository classScheduleRepository;
    private final ClassRegistrationRepository classRegistrationRepository;

    public PtClassServiceImpl(ClassScheduleRepository classScheduleRepository,
                              ClassRegistrationRepository classRegistrationRepository) {
        this.classScheduleRepository = classScheduleRepository;
        this.classRegistrationRepository = classRegistrationRepository;
    }

    @Override
    public Page<PtClassScheduleSummaryResponse> getSchedulesForTrainer(Long trainerUserId,
                                                                       LocalDate fromDate,
                                                                       Pageable pageable) {
        if (fromDate == null) {
            fromDate = LocalDate.now();
        }

        Page<ClassSchedule> page = classScheduleRepository.findUpcomingSchedulesForTrainer(
                trainerUserId,
                ClassStatus.CANCELLED,
                fromDate,
                pageable
        );

        return page.map(this::mapToSummaryDto);
    }

    @Override
    public PtClassScheduleDetailResponse getScheduleDetailForTrainer(Long trainerUserId,
                                                                     Long scheduleId) {
        ClassSchedule schedule = classScheduleRepository.findDetailById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lịch lớp."));

        FitnessClass fitnessClass = schedule.getFitnessClass();
        Trainer trainer = fitnessClass.getTrainer();

        if (trainer == null || trainer.getUser() == null
                || !trainer.getUser().getId().equals(trainerUserId)) {
            throw new AccessDeniedException("Bạn không có quyền xem lịch lớp này.");
        }

        long registeredCount = classRegistrationRepository
                .countBySchedule_IdAndStatusNot(scheduleId, RegistrationStatus.CANCELLED);

        List<ClassRegistration> registrations = classRegistrationRepository
                .findBySchedule_IdAndStatusNotOrderByRegisteredAtAsc(
                        scheduleId,
                        RegistrationStatus.CANCELLED
                );

        List<PtClassMemberRegistrationResponse> registrationDtos = registrations.stream()
                .map(this::mapToMemberRegistrationDto)
                .toList();

        return PtClassScheduleDetailResponse.builder()
                .scheduleId(schedule.getId())
                .className(fitnessClass.getName())
                .classDescription(fitnessClass.getDescription())
                .branchName(fitnessClass.getBranch().getName())
                .categoryName(fitnessClass.getCategory().getName())
                .scheduleDate(schedule.getScheduleDate())
                .startTime(schedule.getStartTime())
                .endTime(schedule.getEndTime())
                .room(schedule.getRoom())
                .capacity(fitnessClass.getCapacity())
                .registeredCount(registeredCount)
                .registrations(registrationDtos)
                .build();
    }

    private PtClassScheduleSummaryResponse mapToSummaryDto(ClassSchedule schedule) {
        FitnessClass fitnessClass = schedule.getFitnessClass();

        long registeredCount = classRegistrationRepository
                .countBySchedule_IdAndStatusNot(schedule.getId(), RegistrationStatus.CANCELLED);

        return PtClassScheduleSummaryResponse.builder()
                .scheduleId(schedule.getId())
                .className(fitnessClass.getName())
                .branchName(fitnessClass.getBranch().getName())
                .categoryName(fitnessClass.getCategory().getName())
                .scheduleDate(schedule.getScheduleDate())
                .startTime(schedule.getStartTime())
                .endTime(schedule.getEndTime())
                .room(schedule.getRoom())
                .capacity(fitnessClass.getCapacity())
                .registeredCount(registeredCount)
                .build();
    }

    private PtClassMemberRegistrationResponse mapToMemberRegistrationDto(ClassRegistration registration) {
        User user = registration.getUser();

        return PtClassMemberRegistrationResponse.builder()
                .registrationId(registration.getId())
                .memberId(user.getId())
                .memberName(user.getFullName())
                .memberEmail(user.getEmail())
                .status(registration.getStatus().name())
                .registeredAt(registration.getRegisteredAt())
                .build();
    }
}
