package com.utefit.service.impl;

import com.utefit.dto.trainer.PersonalTrainingScheduleResponse;
import com.utefit.entity.PersonalTrainingSchedule;
import com.utefit.entity.Trainer;
import com.utefit.entity.User;
import com.utefit.enums.NotificationType;
import com.utefit.enums.TrainerRequestStatus;
import com.utefit.repository.PersonalTrainingScheduleRepository;
import com.utefit.repository.TrainerRepository;
import com.utefit.repository.TrainerRequestRepository;
import com.utefit.repository.UserRepository;
import com.utefit.service.NotificationService;
import com.utefit.service.PersonalTrainingScheduleService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PersonalTrainingScheduleServiceImpl implements PersonalTrainingScheduleService {

    private final PersonalTrainingScheduleRepository personalTrainingScheduleRepository;
    private final TrainerRepository trainerRepository;
    private final UserRepository userRepository;
    private final TrainerRequestRepository trainerRequestRepository;
    private final NotificationService notificationService;

    @Override
    public PersonalTrainingScheduleResponse createScheduleForMember(
            Long trainerUserId,
            Long memberUserId,
            LocalDate scheduleDate,
            LocalTime startTime,
            LocalTime endTime,
            String title,
            String note
    ) {
        if (scheduleDate == null || startTime == null || endTime == null) {
            throw new IllegalArgumentException("Ngày và thời gian không được để trống.");
        }

        if (!startTime.isBefore(endTime)) {
            throw new IllegalStateException("Thời gian bắt đầu phải trước thời gian kết thúc.");
        }

        User member = userRepository.findById(memberUserId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Không tìm thấy thành viên."));

        Trainer trainer = trainerRepository.findByUserId(trainerUserId)
                .orElseThrow(() ->
                        new IllegalStateException("Không tìm thấy hồ sơ huấn luyện viên."));

        boolean hasAcceptedRequest = trainerRequestRepository
                .existsByMember_IdAndTrainer_IdAndStatus(
                        member.getId(),
                        trainer.getId(),
                        TrainerRequestStatus.ACCEPTED
                );

        if (!hasAcceptedRequest) {
            throw new IllegalStateException(
                    "Chỉ được tạo lịch PT sau khi yêu cầu huấn luyện viên đã được chấp nhận."
            );
        }

        var trainerOverlaps = personalTrainingScheduleRepository
                .findOverlappingForTrainer(
                        trainer.getId(),
                        scheduleDate,
                        startTime,
                        endTime,
                        null
                );

        if (!trainerOverlaps.isEmpty()) {
            throw new IllegalStateException(
                    "Huấn luyện viên đã có lịch PT cá nhân trong khoảng thời gian này."
            );
        }

        var memberOverlaps = personalTrainingScheduleRepository
                .findOverlappingForMember(
                        member.getId(),
                        scheduleDate,
                        startTime,
                        endTime,
                        null
                );

        if (!memberOverlaps.isEmpty()) {
            throw new IllegalStateException(
                    "Thành viên đã có lịch PT cá nhân trong khoảng thời gian này."
            );
        }

        PersonalTrainingSchedule schedule = PersonalTrainingSchedule.builder()
                .trainer(trainer)
                .member(member)
                .scheduleDate(scheduleDate)
                .startTime(startTime)
                .endTime(endTime)
                .title(title)
                .note(note)
                .active(true)
                .build();

        PersonalTrainingSchedule saved = personalTrainingScheduleRepository.save(schedule);

        // Notification cho Member: PT tạo lịch
        User trainerUser = trainer.getUser();

        String trainerName = trainerUser.getFullName();
        String branchName = trainer.getBranch() != null
                ? trainer.getBranch().getName()
                : "";

        String timeStr = "%02d:%02d - %02d:%02d".formatted(
                startTime.getHour(), startTime.getMinute(),
                endTime.getHour(), endTime.getMinute()
        );

        String dateStr = "%02d/%02d/%04d".formatted(
                scheduleDate.getDayOfMonth(),
                scheduleDate.getMonthValue(),
                scheduleDate.getYear()
        );

        String notiTitle = "Lịch PT cá nhân mới";
        StringBuilder notiContent = new StringBuilder();
        notiContent.append("Bạn có buổi PT với ").append(trainerName)
                .append(" vào ").append(dateStr)
                .append(" (").append(timeStr).append(")");
        if (!branchName.isEmpty()) {
            notiContent.append(" tại ").append(branchName);
        }

        notificationService.createNotificationForUser(
                member.getId(),
                NotificationType.PERSONAL_SCHEDULE_CREATED,
                notiTitle,
                notiContent.toString(),
                saved.getId()
        );

        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PersonalTrainingScheduleResponse> getSchedulesForTrainer(
            Long trainerUserId,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        if (fromDate == null || toDate == null) {
            throw new IllegalArgumentException("Khoảng ngày không hợp lệ.");
        }
        if (toDate.isBefore(fromDate)) {
            throw new IllegalArgumentException("Ngày kết thúc phải sau ngày bắt đầu.");
        }

        List<PersonalTrainingSchedule> schedules =
                personalTrainingScheduleRepository
                        .findByTrainer_User_IdAndScheduleDateBetweenAndActiveTrueOrderByScheduleDateAscStartTimeAsc(
                                trainerUserId,
                                fromDate,
                                toDate
                        );

        return schedules.stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PersonalTrainingScheduleResponse> getSchedulesForMember(
            Long memberUserId,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        if (fromDate == null || toDate == null) {
            throw new IllegalArgumentException("Khoảng ngày không hợp lệ.");
        }
        if (toDate.isBefore(fromDate)) {
            throw new IllegalArgumentException("Ngày kết thúc phải sau ngày bắt đầu.");
        }

        List<PersonalTrainingSchedule> schedules =
                personalTrainingScheduleRepository
                        .findByMember_IdAndScheduleDateBetweenAndActiveTrueOrderByScheduleDateAscStartTimeAsc(
                                memberUserId,
                                fromDate,
                                toDate
                        );

        return schedules.stream()
                .map(this::toResponse)
                .toList();
    }

    private PersonalTrainingScheduleResponse toResponse(PersonalTrainingSchedule entity) {
        User member = entity.getMember();
        Trainer trainer = entity.getTrainer();
        User trainerUser = trainer.getUser();

        String trainerName = trainerUser.getFullName();
        String memberName = member.getFullName();

        String branchName = trainer.getBranch() != null
                ? trainer.getBranch().getName()
                : null;

        return PersonalTrainingScheduleResponse.builder()
                .id(entity.getId())
                .trainerId(trainer.getId())
                .trainerName(trainerName)
                .memberId(member.getId())
                .memberName(memberName)
                .scheduleDate(entity.getScheduleDate())
                .startTime(entity.getStartTime())
                .endTime(entity.getEndTime())
                .title(entity.getTitle())
                .note(entity.getNote())
                .branchName(branchName)
                .build();
    }
}