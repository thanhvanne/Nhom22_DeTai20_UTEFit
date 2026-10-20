package com.utefit.dto.trainer;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PtClassScheduleSummaryResponse {

    private Long scheduleId;

    private String className;
    private String branchName;
    private String categoryName;

    private LocalDate scheduleDate;
    private LocalTime startTime;
    private LocalTime endTime;

    private String room;

    private Integer capacity;
    private long registeredCount;
}