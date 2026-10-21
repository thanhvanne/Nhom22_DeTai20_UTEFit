package com.utefit.service.impl;

import com.utefit.dto.trainer.TrainerDashboardSummaryResponse;
import com.utefit.dto.trainer.TrainerRequestSummaryResponse;
import com.utefit.dto.trainer.UpcomingScheduleResponse;
import com.utefit.entity.TrainerRequest;
import com.utefit.enums.TrainerRequestStatus;
import com.utefit.repository.TrainerRequestRepository;
import com.utefit.service.ChatService;
import com.utefit.service.NotificationService;
import com.utefit.service.trainer.TrainerDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TrainerDashboardServiceImpl implements TrainerDashboardService {

    private final TrainerRequestRepository trainerRequestRepository;
    private final NotificationService notificationService;
    private final ChatService chatService;

    @Override
    public TrainerDashboardSummaryResponse getDashboardForTrainer(Long trainerUserId) {
        // Pending requests (tối đa 5)
        List<TrainerRequest> pending =
                trainerRequestRepository
                        .findTop5ByTrainer_User_IdAndStatusOrderByRequestedAtDesc(
                                trainerUserId,
                                TrainerRequestStatus.PENDING
                        );

        List<TrainerRequestSummaryResponse> pendingDtos = pending.stream()
                .map(this::mapToTrainerRequestSummary)
                .toList();

        long unreadNoti = notificationService.countUnreadForUser(trainerUserId);
        long unreadMsg = chatService.countUnreadMessagesForUser(trainerUserId);

        TrainerDashboardSummaryResponse response = new TrainerDashboardSummaryResponse();
        response.setPendingRequests(pendingDtos);
        // Hiện tại chưa gộp lịch class + PT 1:1 vào dashboard, để trống:
        response.setUpcomingSchedules(Collections.<UpcomingScheduleResponse>emptyList());
        response.setUnreadNotificationCount(unreadNoti);
        response.setUnreadMessageCount(unreadMsg);
        return response;
    }

    private TrainerRequestSummaryResponse mapToTrainerRequestSummary(TrainerRequest tr) {
        TrainerRequestSummaryResponse dto = new TrainerRequestSummaryResponse();
        dto.setId(tr.getId());

        // TrainerRequest.member ở đây đang là User (dựa trên ChatServiceImpl anh gửi)
        dto.setMemberUserId(tr.getMember().getId());
        dto.setMemberName(tr.getMember().getFullName());
        dto.setMemberAvatarUrl(tr.getMember().getAvatarUrl());

        dto.setRequestedAt(tr.getRequestedAt());
        dto.setStatus(tr.getStatus().name());
        return dto;
    }
}