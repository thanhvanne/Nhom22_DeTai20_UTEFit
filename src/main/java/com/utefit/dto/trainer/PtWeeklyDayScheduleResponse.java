package com.utefit.dto.trainer;

import lombok.Data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
public class PtWeeklyDayScheduleResponse {

    private LocalDate date;
    private String dayLabel;

    // LỚP NHÓM
    private List<PtClassScheduleSummaryResponse> morningClasses  = new ArrayList<>();
    private List<PtClassScheduleSummaryResponse> afternoonClasses = new ArrayList<>();
    private List<PtClassScheduleSummaryResponse> eveningClasses  = new ArrayList<>();

    // BUỔI PT 1:1
    private List<PersonalTrainingScheduleResponse> morningPersonalSessions  = new ArrayList<>();
    private List<PersonalTrainingScheduleResponse> afternoonPersonalSessions = new ArrayList<>();
    private List<PersonalTrainingScheduleResponse> eveningPersonalSessions  = new ArrayList<>();
}