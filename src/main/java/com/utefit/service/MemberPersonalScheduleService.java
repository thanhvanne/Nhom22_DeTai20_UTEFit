package com.utefit.service;

import com.utefit.dto.trainer.PersonalTrainingScheduleResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MemberPersonalScheduleService {

    private final MemberAccountService accounts;
    private final PersonalTrainingScheduleService schedules; // B sở hữu

    public List<PersonalTrainingScheduleResponse> getMySchedules(
            String email, LocalDate from, LocalDate to) {
        if (from == null || to == null || to.isBefore(from)
                || to.isAfter(from.plusYears(1))) {
            throw new IllegalArgumentException(
                    "Khoảng ngày không hợp lệ hoặc vượt quá 1 năm.");
        }
        return schedules.getSchedulesForMember(
                accounts.requireMemberId(email), from, to);
    }
}