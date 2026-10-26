package com.utefit.service.impl;

import com.utefit.dto.trainer.PersonalTrainingScheduleResponse;
import com.utefit.entity.PersonalTrainingSchedule;
import com.utefit.entity.Trainer;
import com.utefit.entity.User;
import com.utefit.enums.NotificationType;
import com.utefit.enums.TrainerRequestStatus;
import com.utefit.enums.PersonalTrainingStatus;
import com.utefit.enums.UserRole;
import com.utefit.enums.UserStatus;
import com.utefit.repository.CheckinRepository;
import com.utefit.service.ScheduleConflictService;
import com.utefit.service.MembershipEligibilityService;
import java.time.LocalDateTime;
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
    private final MembershipEligibilityService eligibility;
    private final ScheduleConflictService conflicts;
    private final CheckinRepository checkins;

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

        // KHÔNG cho tạo lịch ở ngày quá khứ
        LocalDate today = LocalDate.now();
        if (scheduleDate.isBefore(today) || !scheduleDate.atTime(startTime).isAfter(LocalDateTime.now())) {
            throw new IllegalStateException("Ngày tập phải là hôm nay hoặc trong tương lai.");
        }

        User member = userRepository.lockByIdForBooking(memberUserId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Không tìm thấy thành viên."));
        userRepository.lockByIdForBooking(trainerUserId)
                .orElseThrow(() -> new EntityNotFoundException("PT không tồn tại."));

        if (member.getRole() != UserRole.MEMBER || member.getStatus() != UserStatus.ACTIVE
                || !Boolean.TRUE.equals(member.getEnabled())) {
            throw new IllegalStateException("Tài khoản Member không hoạt động.");
        }
        Trainer trainer = trainerRepository.findByUserId(trainerUserId)
                .orElseThrow(() ->
                        new IllegalStateException("Không tìm thấy hồ sơ huấn luyện viên."));

        if (trainer.getUser().getRole() != UserRole.PT || trainer.getUser().getStatus() != UserStatus.ACTIVE
                || !Boolean.TRUE.equals(trainer.getUser().getEnabled())
                || !Boolean.TRUE.equals(trainer.getActive()) || trainer.getBranch() == null
                || !Boolean.TRUE.equals(trainer.getBranch().getActive())) {
            throw new IllegalStateException("PT phải ACTIVE và được Admin gán chi nhánh đang hoạt động.");
        }
        if (title != null && title.trim().length() > 255
                || note != null && note.trim().length() > 2000) {
            throw new IllegalArgumentException("Tiêu đề tối đa 255 và ghi chú tối đa 2000 ký tự.");
        }
        if (trainer.getBranch().getOpeningTime() != null
                && startTime.isBefore(trainer.getBranch().getOpeningTime())) {
            throw new IllegalStateException("Giờ tập trước giờ mở cửa chi nhánh.");
        }
        if (trainer.getBranch().getClosingTime() != null
                && endTime.isAfter(trainer.getBranch().getClosingTime())) {
            throw new IllegalStateException("Giờ tập sau giờ đóng cửa chi nhánh.");
        }
        eligibility.requirePersonalTraining(member.getId(), trainer.getBranch().getId(), scheduleDate);
        conflicts.assertTrainerFreeForPersonal(trainer.getId(), scheduleDate, startTime, endTime);
        conflicts.assertMemberFree(member.getId(), scheduleDate, startTime, endTime, null);

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
                .branch(trainer.getBranch())
                .status(PersonalTrainingStatus.SCHEDULED)
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
    public void cancelSchedule(Long trainerUserId, Long scheduleId) {
        PersonalTrainingSchedule s = ownedSchedule(trainerUserId, scheduleId);
        if (!Boolean.TRUE.equals(s.getActive()) || s.getStatus() == PersonalTrainingStatus.COMPLETED
                || s.getStatus() == PersonalTrainingStatus.CANCELLED) {
            throw new IllegalStateException("Buổi này không thể hủy.");
        }
        if (!s.getScheduleDate().atTime(s.getStartTime()).isAfter(LocalDateTime.now())) {
            throw new IllegalStateException("Không thể hủy buổi tập đã bắt đầu.");
        }
        s.setActive(false);
        s.setStatus(PersonalTrainingStatus.CANCELLED);
        notificationService.createNotificationForUser(s.getMember().getId(),
                NotificationType.PERSONAL_SCHEDULE_CANCELLED,
                "Lịch PT đã hủy", "PT đã hủy lịch cá nhân ngày " + s.getScheduleDate() + ".", s.getId());
    }

    @Override
    public void completeSchedule(Long trainerUserId, Long scheduleId) {
        PersonalTrainingSchedule s = ownedSchedule(trainerUserId, scheduleId);
        if (!Boolean.TRUE.equals(s.getActive()) || s.getStatus() == PersonalTrainingStatus.COMPLETED
                || s.getStatus() == PersonalTrainingStatus.CANCELLED) {
            throw new IllegalStateException("Trạng thái buổi tập không cho phép hoàn thành.");
        }
        LocalDateTime start = s.getScheduleDate().atTime(s.getStartTime());
        LocalDateTime end = s.getScheduleDate().atTime(s.getEndTime());
        if (LocalDateTime.now().isBefore(end)) {
            throw new IllegalStateException("Buổi tập chưa kết thúc.");
        }
        if (s.getBranch() == null || checkins.findCompletedDuringSession(s.getMember().getId(),
                s.getBranch().getId(), start, end).isEmpty()) {
            throw new IllegalStateException("Cần có Check-in/Check-out hoàn tất, trùng giờ và đúng chi nhánh.");
        }
        s.setStatus(PersonalTrainingStatus.COMPLETED);
    }

    private PersonalTrainingSchedule ownedSchedule(Long trainerUserId, Long scheduleId) {
        PersonalTrainingSchedule s = personalTrainingScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy lịch PT cá nhân."));
        if (!s.getTrainer().getUser().getId().equals(trainerUserId)) {
            throw new org.springframework.security.access.AccessDeniedException("Không phải lịch PT của bạn.");
        }
        return s;
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
                        .findByMember_IdAndScheduleDateBetweenOrderByScheduleDateAscStartTimeAsc(
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

        String branchName = entity.getBranch() != null
                ? entity.getBranch().getName()
                : (trainer.getBranch() != null ? trainer.getBranch().getName() : null);

        return PersonalTrainingScheduleResponse.builder()
                .id(entity.getId())
                .trainerId(trainer.getId())
                .trainerName(trainerName)
                .memberId(member.getId())
                .memberName(memberName)
                .scheduleDate(entity.getScheduleDate())
                .startTime(entity.getStartTime())
                .endTime(entity.getEndTime())
                .title(entity.getStatus() == PersonalTrainingStatus.CANCELLED
                        ? "[Đã hủy] " + entity.getTitle() : entity.getTitle())
                .note(entity.getNote())
                .branchName(branchName)
                .status(entity.getStatus() == null ? "SCHEDULED" : entity.getStatus().name())
                .build();
    }
}
