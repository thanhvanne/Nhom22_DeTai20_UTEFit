package com.utefit.dto.trainer;

import java.util.List;

public class TrainerDashboardSummaryResponse {

    // MỚI: dùng cho hero trên pt/dashboard.html
    private String trainerName;
    private String avatarUrl;
    private String branchName;

    // ĐÃ CÓ:
    private List<TrainerRequestSummaryResponse> pendingRequests;
    private List<UpcomingScheduleResponse> upcomingSchedules;
    private long unreadNotificationCount;
    private long unreadMessageCount;

    // ===== GETTER/SETTER MỚI =====

    public String getTrainerName() {
        return trainerName;
    }

    public void setTrainerName(String trainerName) {
        this.trainerName = trainerName;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getBranchName() {
        return branchName;
    }

    public void setBranchName(String branchName) {
        this.branchName = branchName;
    }

    // ===== GETTER/SETTER CŨ =====

    public List<TrainerRequestSummaryResponse> getPendingRequests() {
        return pendingRequests;
    }

    public void setPendingRequests(List<TrainerRequestSummaryResponse> pendingRequests) {
        this.pendingRequests = pendingRequests;
    }

    public List<UpcomingScheduleResponse> getUpcomingSchedules() {
        return upcomingSchedules;
    }

    public void setUpcomingSchedules(List<UpcomingScheduleResponse> upcomingSchedules) {
        this.upcomingSchedules = upcomingSchedules;
    }

    public long getUnreadNotificationCount() {
        return unreadNotificationCount;
    }

    public void setUnreadNotificationCount(long unreadNotificationCount) {
        this.unreadNotificationCount = unreadNotificationCount;
    }

    public long getUnreadMessageCount() {
        return unreadMessageCount;
    }

    public void setUnreadMessageCount(long unreadMessageCount) {
        this.unreadMessageCount = unreadMessageCount;
    }
}