package com.utefit.service.impl;

import com.utefit.dto.trainer.PtClassScheduleSummaryResponse;
import com.utefit.dto.trainer.PersonalTrainingScheduleResponse;
import com.utefit.dto.trainer.TrainerDashboardSummaryResponse;
import com.utefit.dto.trainer.TrainerRequestSummaryResponse;
import com.utefit.dto.trainer.UpcomingScheduleResponse;
import com.utefit.entity.TrainerRequest;
import com.utefit.enums.TrainerRequestStatus;
import com.utefit.repository.TrainerRequestRepository;
import com.utefit.service.ChatService;
import com.utefit.service.NotificationService;
import com.utefit.service.PtClassService;
import com.utefit.service.PersonalTrainingScheduleService;
import com.utefit.service.PtDashboardService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PtDashboardServiceImpl implements PtDashboardService {

    private final TrainerRequestRepository trainerRequestRepository;
    private final PtClassService ptClassService;
    private final PersonalTrainingScheduleService personalTrainingScheduleService;
    private final NotificationService notificationService;
    private final ChatService chatService;

    @Override
    public TrainerDashboardSummaryResponse getDashboardForTrainer(Long trainerUserId) {
        if (trainerUserId == null) {
            throw new EntityNotFoundException("Không xác định được huấn luyện viên đang đăng nhập.");
        }

        // Pending requests (tối đa 5)
        List<TrainerRequest> pendingRequests =
                trainerRequestRepository.findTop5ByTrainer_User_IdAndStatusOrderByRequestedAtDesc(
                        trainerUserId,
                        TrainerRequestStatus.PENDING
                );

        List<TrainerRequestSummaryResponse> pendingDtos = pendingRequests.stream()
                .map(this::mapToTrainerRequestSummary)
                .toList();

        // Lịch 7 ngày tới (gộp class + lịch PT cá nhân)
        LocalDate today = LocalDate.now();
        LocalDate toDate = today.plusDays(7);

        Page<PtClassScheduleSummaryResponse> classPage =
                ptClassService.getSchedulesForTrainer(trainerUserId, today, Pageable.unpaged());

        List<PtClassScheduleSummaryResponse> classSchedules = classPage.getContent().stream()
                .filter(cs -> !cs.getScheduleDate().isBefore(today)
                           && !cs.getScheduleDate().isAfter(toDate))
                .toList();

        List<PersonalTrainingScheduleResponse> personalSchedules =
                personalTrainingScheduleService.getSchedulesForTrainer(trainerUserId, today, toDate);

        List<UpcomingScheduleResponse> upcoming = new ArrayList<>();

        // Class schedules
        for (PtClassScheduleSummaryResponse cs : classSchedules) {
            UpcomingScheduleResponse dto = new UpcomingScheduleResponse();
            dto.setType("CLASS");
            dto.setDate(cs.getScheduleDate());
            dto.setStartTime(cs.getStartTime());
            dto.setEndTime(cs.getEndTime());
            dto.setTitle(cs.getClassName());
            dto.setClassName(cs.getClassName());
            upcoming.add(dto);
        }

        // Personal PT schedules
        for (PersonalTrainingScheduleResponse ps : personalSchedules) {
            UpcomingScheduleResponse dto = new UpcomingScheduleResponse();
            dto.setType("PERSONAL");
            dto.setDate(ps.getScheduleDate());
            dto.setStartTime(ps.getStartTime());
            dto.setEndTime(ps.getEndTime());
            dto.setTitle(ps.getTitle());
            dto.setMemberName(ps.getMemberName());
            upcoming.add(dto);
        }

        upcoming.sort(Comparator
                .comparing(UpcomingScheduleResponse::getDate)
                .thenComparing(UpcomingScheduleResponse::getStartTime));

        long unreadNoti = notificationService.countUnreadForUser(trainerUserId);
        long unreadMsg = chatService.countUnreadMessagesForUser(trainerUserId);

        TrainerDashboardSummaryResponse result = new TrainerDashboardSummaryResponse();
        result.setPendingRequests(pendingDtos);
        result.setUpcomingSchedules(upcoming);
        result.setUnreadNotificationCount(unreadNoti);
        result.setUnreadMessageCount(unreadMsg);
        return result;
    }

    private TrainerRequestSummaryResponse mapToTrainerRequestSummary(TrainerRequest tr) {
        TrainerRequestSummaryResponse dto = new TrainerRequestSummaryResponse();
        dto.setId(tr.getId());

        // entity TrainerRequest: member là User hay Member?
        // Trong ChatServiceImpl anh dùng trainerRequestRepository.existsByMember_Id... => member.id là ID user (member user)
        dto.setMemberUserId(tr.getMember().getId());
        dto.setMemberName(tr.getMember().getFullName());

        // Nếu User có avatarUrl:
        dto.setMemberAvatarUrl(tr.getMember().getAvatarUrl());

        dto.setRequestedAt(tr.getRequestedAt());
        dto.setStatus(tr.getStatus().name());
        return dto;
    }
}