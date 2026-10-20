package com.utefit.dto.trainer;

import com.utefit.dto.trainerrequest.TrainerRequestResponse;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PtDashboardResponse {

    private Long trainerUserId;
    private Long trainerId;

    private String trainerName;
    private String branchName;
    private String avatarUrl;

    private long pendingRequestCount;
    private long unreadMessageCount;
    private long unreadNotificationCount;

    private List<TrainerRequestResponse> recentPendingRequests;
    private List<PersonalTrainingScheduleResponse> upcomingPersonalSchedules;
    private List<PtClassScheduleSummaryResponse> upcomingClassSchedules;
}