package com.utefit.service;

import com.utefit.entity.*;
import com.utefit.enums.*;
import com.utefit.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;

/** Nhắc lịch trước 24 giờ, lưu vào notifications và gửi realtime qua NotificationService.
 * Dùng relatedId + title chứa ngày để chặn gửi lặp mỗi lần cron chạy.
 * Khi nhiều instance chạy cùng DB cần thêm unique constraint/idempotency tập trung. */
@Service
@ConditionalOnProperty(name="utefit.scheduler.reminders-enabled",havingValue="true")
@RequiredArgsConstructor
public class UpcomingSessionReminderScheduler {
    private static final ZoneId ZONE=ZoneId.of("Asia/Ho_Chi_Minh");
    private final ClassRegistrationRepository registrations;
    private final PersonalTrainingScheduleRepository personalSchedules;
    private final NotificationRepository existing;
    private final NotificationService notifications;

    @Scheduled(cron="0 */15 * * * *",zone="Asia/Ho_Chi_Minh")
    @Transactional
    public void notifyUpcoming() {
        LocalDateTime now=LocalDateTime.now(ZONE);
        LocalDateTime start=now.plusHours(23);
        LocalDateTime end=now.plusHours(24);
        for (ClassRegistration reg:registrations.findUpcomingForReminders(
                RegistrationStatus.REGISTERED,ClassStatus.CANCELLED,
                start.toLocalDate(),end.toLocalDate())) {
            ClassSchedule s=reg.getSchedule();
            LocalDateTime at=s.getScheduleDate().atTime(s.getStartTime());
            if (at.isBefore(start) || at.isAfter(end)) continue;
            String title="Nhắc lịch lớp · "+s.getScheduleDate();
            alert(reg.getUser().getId(),s.getId(),title,
                    "Lớp "+s.getFitnessClass().getName()+" bắt đầu lúc "+s.getStartTime()+
                            " ngày "+s.getScheduleDate()+". Xem thời khóa biểu của bạn.");
        }
        for (PersonalTrainingSchedule s:personalSchedules.findUpcomingForReminders(
                PersonalTrainingStatus.SCHEDULED,start.toLocalDate(),end.toLocalDate())) {
            LocalDateTime at=s.getScheduleDate().atTime(s.getStartTime());
            if (at.isBefore(start) || at.isAfter(end)) continue;
            String title="Nhắc lịch PT · "+s.getScheduleDate();
            String body="Buổi PT 1:1 bắt đầu lúc "+s.getStartTime()+" ngày "+s.getScheduleDate()+".";
            alert(s.getMember().getId(),s.getId(),title,body);
            alert(s.getTrainer().getUser().getId(),s.getId(),title,
                    "Bạn có buổi PT 1:1 với "+s.getMember().getFullName()+" lúc "+s.getStartTime()+" ngày "+s.getScheduleDate()+".");
        }
    }
    private void alert(Long userId,Long relatedId,String title,String body) {
        if (!existing.existsByUser_IdAndTypeAndRelatedIdAndTitle(
                userId,NotificationType.SCHEDULE_REMINDER,relatedId,title)) {
            notifications.createNotificationForUser(userId,NotificationType.SCHEDULE_REMINDER,
                    title,body,relatedId);
        }
    }
}
