package com.utefit.dto.trainer;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PtWeeklyDayScheduleResponse {

    private LocalDate date;

    // Nhãn hiển thị: "Thứ 2", "Thứ 3", ..., "Chủ nhật"
    private String dayLabel;

    private List<PtClassScheduleSummaryResponse> morningClasses = new ArrayList<>();

    private List<PtClassScheduleSummaryResponse> afternoonClasses = new ArrayList<>();

    private List<PtClassScheduleSummaryResponse> eveningClasses = new ArrayList<>();
}