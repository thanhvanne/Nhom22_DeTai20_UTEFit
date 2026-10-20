package com.utefit.service.impl;

import com.utefit.dto.trainer.*;
import com.utefit.dto.trainerrequest.TrainerRequestResponse;
import com.utefit.entity.Trainer;
import com.utefit.entity.TrainerRequest;
import com.utefit.entity.User;
import com.utefit.enums.TrainerRequestStatus;
import com.utefit.repository.TrainerRepository;
import com.utefit.repository.TrainerRequestRepository;
import com.utefit.repository.UserRepository;
import com.utefit.service.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PtDashboardServiceImpl implements PtDashboardService {

    private final UserRepository userRepository;
    private final TrainerRepository trainerRepository;
    private final TrainerRequestRepository trainerRequestRepository;
    private final TrainerRequestService trainerRequestService;
    private final PersonalTrainingScheduleService personalTrainingScheduleService;
    private final PtClassService ptClassService;
    private final ChatService chatService;
    private final NotificationService notificationService;

    @Override
    public PtDashboardResponse getDashboardForTrainer(Long trainerUserId) {
        User user = userRepository.findById(trainerUserId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Không tìm thấy tài khoản PT."));

        Trainer trainer = trainerRepository.findByUserId(trainerUserId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Không tìm thấy hồ sơ huấn luyện viên."));

        String trainerName = user.getFullName();
        String branchName = trainer.getBranch() != null
                ? trainer.getBranch().getName()
                : null;

        LocalDate today = LocalDate.now();
        LocalDate nextWeek = today.plusDays(7);

        // 1. Yêu cầu pending
        long pendingCount = trainerRequestRepository
                .countByTrainer_User_IdAndStatus(trainerUserId, TrainerRequestStatus.PENDING);

        List<TrainerRequest> recentPendingEntities =
                trainerRequestRepository
                        .findTop5ByTrainer_User_IdAndStatusOrderByRequestedAtDesc(
                                trainerUserId, TrainerRequestStatus.PENDING
                        );

        List<TrainerRequestResponse> recentPending = recentPendingEntities.stream()
                .map(this::mapToTrainerRequestResponse)
                .toList();

        // 2. Lịch PT cá nhân 7 ngày tới
        List<PersonalTrainingScheduleResponse> personalSchedules =
                personalTrainingScheduleService.getSchedulesForTrainer(
                        trainerUserId,
                        today,
                        nextWeek
                );

        // Giới hạn 5 lịch sớm nhất
        personalSchedules = personalSchedules.stream()
                .sorted(Comparator.comparing(PersonalTrainingScheduleResponse::getScheduleDate)
                        .thenComparing(PersonalTrainingScheduleResponse::getStartTime))
                .limit(5)
                .toList();

        // 3. Lịch lớp được phân công 7 ngày tới
        var classPage = ptClassService.getSchedulesForTrainer(
                trainerUserId,
                today,
                PageRequest.of(0, 20)
        );

        List<PtClassScheduleSummaryResponse> upcomingClasses = classPage.getContent().stream()
                .filter(c -> !c.getScheduleDate().isAfter(nextWeek))
                .sorted(Comparator.comparing(PtClassScheduleSummaryResponse::getScheduleDate)
                        .thenComparing(PtClassScheduleSummaryResponse::getStartTime))
                .limit(5)
                .toList();

        // 4. Đếm tin nhắn & notification chưa đọc
        long unreadMessages = chatService.countUnreadMessagesForUser(trainerUserId);
        long unreadNotifications = notificationService.countUnreadNotifications(trainerUserId);

        return PtDashboardResponse.builder()
                .trainerUserId(trainerUserId)
                .trainerId(trainer.getId())
                .trainerName(trainerName)
                .branchName(branchName)
                .avatarUrl(user.getAvatarUrl())
                .pendingRequestCount(pendingCount)
                .unreadMessageCount(unreadMessages)
                .unreadNotificationCount(unreadNotifications)
                .recentPendingRequests(recentPending)
                .upcomingPersonalSchedules(personalSchedules)
                .upcomingClassSchedules(upcomingClasses)
                .build();
    }

    private TrainerRequestResponse mapToTrainerRequestResponse(TrainerRequest entity) {
        User member = entity.getMember();
        Trainer trainer = entity.getTrainer();
        User trainerUser = trainer.getUser();

        String memberName = buildFullName(member.getLastName(), member.getFirstName());
        String trainerName = buildFullName(trainerUser.getLastName(), trainerUser.getFirstName());

        return TrainerRequestResponse.builder()
                .id(entity.getId())
                .memberId(member.getId())
                .memberName(memberName)
                .memberEmail(member.getEmail())
                .trainerId(trainer.getId())
                .trainerName(trainerName)
                .trainerEmail(trainerUser.getEmail())
                .message(entity.getMessage())
                .status(entity.getStatus())
                .requestedAt(entity.getRequestedAt())
                .respondedAt(entity.getRespondedAt())
                .build();
    }

    private String buildFullName(String lastName, String firstName) {
        String ln = lastName != null ? lastName.trim() : "";
        String fn = firstName != null ? firstName.trim() : "";
        return (ln + " " + fn).trim();
    }
}