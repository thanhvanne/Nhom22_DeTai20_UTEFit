package com.utefit.service;

import com.utefit.dto.trainer.PersonalTrainingScheduleResponse;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface PersonalTrainingScheduleService {

    /**
     * PT tạo lịch PT cá nhân cho một member.
     * trainerUserId lấy từ tài khoản PT đang đăng nhập.
     */
    PersonalTrainingScheduleResponse createScheduleForMember(
            Long trainerUserId,
            Long memberUserId,
            LocalDate scheduleDate,
            LocalTime startTime,
            LocalTime endTime,
            String title,
            String note
    );

    /**
     * Lịch PT cá nhân của PT trong khoảng ngày [fromDate, toDate]
     */
    List<PersonalTrainingScheduleResponse> getSchedulesForTrainer(
            Long trainerUserId,
            LocalDate fromDate,
            LocalDate toDate
    );

    /**
     * Lịch PT cá nhân của Member trong khoảng ngày [fromDate, toDate]
     */
    List<PersonalTrainingScheduleResponse> getSchedulesForMember(
            Long memberUserId,
            LocalDate fromDate,
            LocalDate toDate
    );
}