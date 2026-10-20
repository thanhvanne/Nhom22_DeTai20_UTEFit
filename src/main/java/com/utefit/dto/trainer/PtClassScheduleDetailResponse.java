package com.utefit.dto.trainer;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PtClassScheduleDetailResponse {

    private Long scheduleId;

    private String className;
    private String classDescription;

    private String branchName;
    private String categoryName;

    private LocalDate scheduleDate;
    private LocalTime startTime;
    private LocalTime endTime;

    private String room;

    private Integer capacity;
    private long registeredCount;

    private List<PtClassMemberRegistrationResponse> registrations;
}